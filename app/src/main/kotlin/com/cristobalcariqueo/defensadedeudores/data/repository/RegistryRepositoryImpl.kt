package com.cristobalcariqueo.defensadedeudores.data.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.cristobalcariqueo.defensadedeudores.data.local.DefensaDatabase
import com.cristobalcariqueo.defensadedeudores.data.local.dao.RegistryDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.RegistryEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.EditResult
import com.cristobalcariqueo.defensadedeudores.domain.model.GraphSlice
import com.cristobalcariqueo.defensadedeudores.domain.model.MatchSuggestion
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Registry
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryFilter
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryType
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryWithNames
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

class RegistryRepositoryImpl(
    private val db: DefensaDatabase,
    private val dao: RegistryDao,
) : RegistryRepository {

    override fun observeRows(
        trackId: String,
        filter: RegistryFilter,
        limit: Int,
        offset: Int,
    ): Flow<List<RegistryWithNames>> {
        val (where, args) = buildWhere(trackId, filter)
        val sql = """
            SELECT r.*, p.name AS person_name, s.name AS source_name
            FROM registries r
            JOIN people p ON p.id = r.person_id
            LEFT JOIN sources s ON s.id = r.source_id
            WHERE $where
            ORDER BY r.date DESC, r.created_at DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return dao.observeRows(SimpleSQLiteQuery(sql, (args + listOf(limit, offset)).toTypedArray()))
            .map { rows ->
                rows.map { row ->
                    RegistryWithNames(
                        registry = row.registry.toDomain(),
                        personName = row.personName,
                        sourceName = row.sourceName,
                    )
                }
            }
    }

    override fun observeCount(trackId: String, filter: RegistryFilter): Flow<Int> {
        val (where, args) = buildWhere(trackId, filter)
        val sql = "SELECT COUNT(*) FROM registries r WHERE $where"
        return dao.observeCount(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    override fun observeDebtorSlices(trackId: String): Flow<List<GraphSlice>> =
        dao.observeDebtorSlices(trackId).map { slices ->
            slices.map { GraphSlice(id = it.id, label = it.name, total = it.total, color = it.color) }
        }

    override fun observeSourceSlices(trackId: String): Flow<List<GraphSlice>> =
        dao.observeSourceSlices(trackId).map { slices ->
            slices.map { GraphSlice(id = it.id, label = it.name, total = it.total, color = it.color) }
        }

    override fun observeOutstandingTotal(trackId: String): Flow<Long> =
        dao.observeOutstandingTotal(trackId)

    override suspend fun createNormal(
        trackId: String,
        personId: String,
        sourceId: String,
        amount: Int,
        note: String?,
        date: LocalDate?,
    ): Registry = insert(trackId, personId, sourceId, amount, RegistryEntity.TYPE_NORMAL, note, date)

    override suspend fun createReturn(
        trackId: String,
        personId: String,
        amount: Int,
        note: String?,
    ): Registry = insert(trackId, personId, null, amount, RegistryEntity.TYPE_RETURN, note)

    override suspend fun updateDetails(
        registryId: String,
        note: String?,
        sourceId: String?,
        date: LocalDate,
    ) {
        val reg = dao.getById(registryId) ?: return
        // Returns have no source; a normal reg must keep one.
        val newSource = if (reg.type == RegistryEntity.TYPE_NORMAL) sourceId ?: reg.sourceId else null
        dao.updateDetails(
            id = registryId,
            note = note?.take(NOTE_MAX_LENGTH)?.ifBlank { null },
            sourceId = newSource,
            date = date.toEpochDays(),
            now = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun editAmount(registryId: String, newAmount: Int): EditResult {
        require(newAmount > 0) { "Registry amounts are always positive" }
        return db.withTransaction {
            val old = dao.getById(registryId)
                ?: throw IllegalArgumentException("Registry $registryId not found")
            require(old.type == RegistryEntity.TYPE_NORMAL) { "Only normal regs are editable" }
            val now = Clock.System.now()
            val nowMs = now.toEpochMilliseconds()

            // Undo an existing retReg match before touching amounts: credit the
            // old amount back and uncheck the retReg (DATA_MODEL.md edit flow).
            val restoredRetReg = old.matchedRetRegId?.takeIf { old.checked }?.let { retId ->
                dao.getById(retId)?.also { ret ->
                    dao.setAmountAndChecked(ret.id, ret.amount + old.amount, false, nowMs)
                }?.let { it.copy(amount = it.amount + old.amount, checked = false) }
            }
            // Checked with no link = settled via return-search Path A; the
            // settlement was real, so the corrected row stays checked.
            val keepChecked = old.checked && old.matchedRetRegId == null

            val createdToday = Instant.fromEpochMilliseconds(old.createdAt)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date ==
                Clock.System.todayIn(TimeZone.currentSystemDefault())

            if (createdToday) {
                dao.setAmountUnlinked(old.id, newAmount, keepChecked, nowMs)
                tryRematch(old.id, newAmount, restoredRetReg, nowMs)
                EditResult.InPlace(old.id)
            } else {
                dao.markSuperseded(old.id, nowMs)
                val newReg = RegistryEntity(
                    id = UUID.randomUUID().toString(),
                    trackId = old.trackId,
                    personId = old.personId,
                    sourceId = old.sourceId,
                    amount = newAmount,
                    type = RegistryEntity.TYPE_NORMAL,
                    checked = keepChecked,
                    note = old.note,
                    date = old.date,
                    createdAt = nowMs,
                    updatedAt = nowMs,
                    supersedesId = old.id,
                )
                dao.insert(newReg)
                val rematched = tryRematch(newReg.id, newAmount, restoredRetReg, nowMs)
                EditResult.Superseded(
                    newReg = newReg.copy(
                        checked = newReg.checked || rematched,
                        matchedRetRegId = if (rematched) restoredRetReg?.id else null,
                    ).toDomain(),
                    rematched = rematched,
                )
            }
        }
    }

    /**
     * Re-applies the corrected amount against the retReg the original was
     * matched to -- only if it still fully fits; otherwise the reg is left for
     * the normal match-suggestion flow.
     */
    private suspend fun tryRematch(
        registryId: String,
        amount: Int,
        retReg: RegistryEntity?,
        nowMs: Long,
    ): Boolean {
        if (retReg == null || retReg.amount < amount) return false
        dao.applyFullCover(registryId, retReg.id, retReg.amount - amount, nowMs)
        return true
    }

    override suspend fun debtorsWithPending(trackId: String): List<Person> =
        dao.debtorsWithPending(trackId).map(PersonEntity::toDomain)

    override suspend fun uncheckedNormalsAll(trackId: String): List<Registry> =
        dao.uncheckedNormalsAll(trackId).map(RegistryEntity::toDomain)

    override suspend fun uncheckedNormals(trackId: String, personId: String): List<Registry> =
        dao.uncheckedNormals(trackId, personId).map(RegistryEntity::toDomain)

    override suspend fun uncheckedRetRegs(trackId: String, personId: String): List<Registry> =
        dao.uncheckedRetRegs(trackId, personId).map(RegistryEntity::toDomain)

    override suspend fun settleExact(registryIds: List<String>) {
        if (registryIds.isEmpty()) return
        dao.checkAll(registryIds, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun applyMatch(suggestion: MatchSuggestion) {
        val now = Clock.System.now().toEpochMilliseconds()
        when (suggestion) {
            is MatchSuggestion.NewRegCovered -> dao.applyFullCover(
                coveredId = suggestion.newReg.id,
                retRegId = suggestion.retReg.id,
                retRegRemainder = suggestion.retReg.amount - suggestion.newReg.amount,
                now = now,
            )
            is MatchSuggestion.ExistingCovered -> dao.applyFullCover(
                coveredId = suggestion.normal.id,
                retRegId = suggestion.retReg.id,
                retRegRemainder = suggestion.retReg.amount - suggestion.normal.amount,
                now = now,
            )
            is MatchSuggestion.NewRegReduced -> dao.applyPartialCover(
                openRegId = suggestion.newReg.id,
                openRegRemainder = suggestion.newReg.amount - suggestion.retReg.amount,
                retRegId = suggestion.retReg.id,
                now = now,
            )
            is MatchSuggestion.ExistingReduced -> dao.applyPartialCover(
                openRegId = suggestion.normal.id,
                openRegRemainder = suggestion.normal.amount - suggestion.retReg.amount,
                retRegId = suggestion.retReg.id,
                now = now,
            )
        }
    }

    @Suppress("LongParameterList") // entity-constructor mirror, one param per column
    private suspend fun insert(
        trackId: String,
        personId: String,
        sourceId: String?,
        amount: Int,
        type: Int,
        note: String?,
        date: LocalDate? = null,
    ): Registry {
        require(amount > 0) { "Registry amounts are always positive" }
        val now = Clock.System.now()
        val entity = RegistryEntity(
            id = UUID.randomUUID().toString(),
            trackId = trackId,
            personId = personId,
            sourceId = sourceId,
            amount = amount,
            type = type,
            checked = false,
            note = note?.take(NOTE_MAX_LENGTH),
            date = (date ?: Clock.System.todayIn(TimeZone.currentSystemDefault())).toEpochDays(),
            createdAt = now.toEpochMilliseconds(),
            updatedAt = now.toEpochMilliseconds(),
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    /** WHERE clause + args for the AND-combinable filters and text search (SCOPE.md). */
    private fun buildWhere(trackId: String, filter: RegistryFilter): Pair<String, List<Any>> {
        val clauses = mutableListOf("r.track_id = ?", "r.deleted_at IS NULL")
        val args = mutableListOf<Any>(trackId)

        if (filter.personIds.isNotEmpty()) {
            clauses += "r.person_id IN (${placeholders(filter.personIds.size)})"
            args.addAll(filter.personIds)
        }
        if (filter.sourceIds.isNotEmpty()) {
            clauses += "r.source_id IN (${placeholders(filter.sourceIds.size)})"
            args.addAll(filter.sourceIds)
        }
        filter.type?.let {
            clauses += "r.type = ?"
            args += it.toCode()
        }
        filter.dateFrom?.let {
            clauses += "r.date >= ?"
            args += it.toEpochDays()
        }
        filter.dateTo?.let {
            clauses += "r.date <= ?"
            args += it.toEpochDays()
        }
        filter.checked?.let {
            clauses += "r.checked = ?"
            args += if (it) 1 else 0
        }
        filter.amountMin?.let {
            clauses += "r.amount >= ?"
            args += it
        }
        filter.amountMax?.let {
            clauses += "r.amount <= ?"
            args += it
        }
        val query = filter.query.trim()
        if (query.isNotEmpty()) {
            val numeric = query.toIntOrNull()
            if (numeric != null) {
                // Numeric search = "what fits in this amount": regs costing at most it.
                clauses += "r.amount <= ?"
                args += numeric
            } else {
                clauses += "r.note LIKE ?"
                args += "%$query%"
            }
        }
        return clauses.joinToString(" AND ") to args
    }

    private fun placeholders(n: Int) = List(n) { "?" }.joinToString(",")

    private fun RegistryType.toCode() = when (this) {
        RegistryType.NORMAL -> RegistryEntity.TYPE_NORMAL
        RegistryType.RETURN -> RegistryEntity.TYPE_RETURN
        RegistryType.SUPERSEDED -> RegistryEntity.TYPE_SUPERSEDED
    }

    private companion object {
        const val NOTE_MAX_LENGTH = 140
    }
}
