package com.cristobalcariqueo.defensadedeudores.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.cristobalcariqueo.defensadedeudores.data.local.dao.RegistryDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.RegistryEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.GraphSlice
import com.cristobalcariqueo.defensadedeudores.domain.model.Registry
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryFilter
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryType
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryWithNames
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class RegistryRepositoryImpl(private val dao: RegistryDao) : RegistryRepository {

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
            slices.map { GraphSlice(id = it.id, label = it.name, total = it.total) }
        }

    override fun observeSourceSlices(trackId: String): Flow<List<GraphSlice>> =
        dao.observeSourceSlices(trackId).map { slices ->
            slices.map { GraphSlice(id = it.id, label = it.name, total = it.total) }
        }

    override fun observeOutstandingTotal(trackId: String): Flow<Long> =
        dao.observeOutstandingTotal(trackId)

    override suspend fun createNormal(
        trackId: String,
        personId: String,
        sourceId: String,
        amount: Int,
        note: String?,
    ): Registry = insert(trackId, personId, sourceId, amount, RegistryEntity.TYPE_NORMAL, note)

    override suspend fun createReturn(
        trackId: String,
        personId: String,
        amount: Int,
        note: String?,
    ): Registry = insert(trackId, personId, null, amount, RegistryEntity.TYPE_RETURN, note)

    override suspend fun editAmount(registryId: String, newAmount: Int) {
        // Edit/supersede flow lands with task #9.
        throw UnsupportedOperationException("editAmount lands with task #9")
    }

    private suspend fun insert(
        trackId: String,
        personId: String,
        sourceId: String?,
        amount: Int,
        type: Int,
        note: String?,
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
            date = Clock.System.todayIn(TimeZone.currentSystemDefault()).toEpochDays(),
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
        if (filter.query.isNotBlank()) {
            clauses += "(CAST(r.amount AS TEXT) LIKE ? OR r.note LIKE ?)"
            val like = "%${filter.query.trim()}%"
            args += like
            args += like
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
