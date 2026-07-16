package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.RegistryEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

/** [RegistryEntity] joined with the names its table row displays. */
data class RegistryRowResult(
    @Embedded val registry: RegistryEntity,
    @ColumnInfo(name = "person_name") val personName: String,
    @ColumnInfo(name = "source_name") val sourceName: String?,
)

/** One graph slice: outstanding total per debtor or per source. */
data class SliceResult(
    val id: String,
    val name: String,
    val color: String?,
    val total: Long,
)

@Dao
interface RegistryDao {
    @Insert
    suspend fun insert(registry: RegistryEntity)

    /** Debtors owing something in this track -- the return-search debtor selector. */
    @Query(
        """
        SELECT * FROM people WHERE id IN (
            SELECT DISTINCT person_id FROM registries
            WHERE track_id = :trackId AND type = ${RegistryEntity.TYPE_NORMAL}
              AND checked = 0 AND deleted_at IS NULL
        )
        ORDER BY name COLLATE NOCASE
        """,
    )
    suspend fun debtorsWithPending(trackId: String): List<PersonEntity>

    /** All unchecked normal regs in this track -- return-search before a debtor is picked. */
    @Query(
        """
        SELECT * FROM registries
        WHERE track_id = :trackId
          AND type = ${RegistryEntity.TYPE_NORMAL} AND checked = 0 AND deleted_at IS NULL
        ORDER BY date DESC, created_at DESC
        """,
    )
    suspend fun uncheckedNormalsAll(trackId: String): List<RegistryEntity>

    /** Full-edit in-place fields; amount goes through the supersede flow instead. */
    @Query(
        """
        UPDATE registries SET note = :note, source_id = :sourceId, date = :date,
            updated_at = :now, dirty = 1 WHERE id = :id
        """,
    )
    suspend fun updateDetails(id: String, note: String?, sourceId: String?, date: Int, now: Long)

    /** Debtor's unchecked normal regs in this track, for return-search + retro matching. */
    @Query(
        """
        SELECT * FROM registries
        WHERE track_id = :trackId AND person_id = :personId
          AND type = ${RegistryEntity.TYPE_NORMAL} AND checked = 0 AND deleted_at IS NULL
        ORDER BY date DESC, created_at DESC
        """,
    )
    suspend fun uncheckedNormals(trackId: String, personId: String): List<RegistryEntity>

    /** Debtor's live retRegs in this track, for forward matching. */
    @Query(
        """
        SELECT * FROM registries
        WHERE track_id = :trackId AND person_id = :personId
          AND type = ${RegistryEntity.TYPE_RETURN} AND checked = 0 AND deleted_at IS NULL
        ORDER BY date DESC, created_at DESC
        """,
    )
    suspend fun uncheckedRetRegs(trackId: String, personId: String): List<RegistryEntity>

    @Query("SELECT * FROM registries WHERE id = :id")
    suspend fun getById(id: String): RegistryEntity?

    /** Amount correction that also drops any retReg link. */
    @Query(
        """
        UPDATE registries SET amount = :amount, checked = :checked,
            matched_retreg_id = NULL, updated_at = :now, dirty = 1 WHERE id = :id
        """,
    )
    suspend fun setAmountUnlinked(id: String, amount: Int, checked: Boolean, now: Long)

    /** Old row of a supersede: frozen out of totals/graphs/matching. */
    @Query(
        """
        UPDATE registries SET type = ${RegistryEntity.TYPE_SUPERSEDED}, checked = 0,
            matched_retreg_id = NULL, updated_at = :now, dirty = 1 WHERE id = :id
        """,
    )
    suspend fun markSuperseded(id: String, now: Long)

    /** Path A of return-search: one-shot settlement, no retReg created. */
    @Query("UPDATE registries SET checked = 1, updated_at = :now, dirty = 1 WHERE id IN (:ids)")
    suspend fun checkAll(ids: List<String>, now: Long)

    @Query(
        "UPDATE registries SET checked = 1, matched_retreg_id = :retRegId, updated_at = :now, dirty = 1 WHERE id = :id",
    )
    suspend fun checkAndLink(id: String, retRegId: String, now: Long)

    @Query("UPDATE registries SET amount = :amount, checked = :checked, updated_at = :now, dirty = 1 WHERE id = :id")
    suspend fun setAmountAndChecked(id: String, amount: Int, checked: Boolean, now: Long)

    /** Forward case 1 / retro case 1: covered reg checked+linked, retReg reduced (consumed at 0). */
    @Transaction
    suspend fun applyFullCover(coveredId: String, retRegId: String, retRegRemainder: Int, now: Long) {
        checkAndLink(coveredId, retRegId, now)
        setAmountAndChecked(retRegId, retRegRemainder, retRegRemainder == 0, now)
    }

    /** Forward case 2 / retro case 2: open reg reduced, retReg fully consumed. */
    @Transaction
    suspend fun applyPartialCover(openRegId: String, openRegRemainder: Int, retRegId: String, now: Long) {
        setAmountAndChecked(openRegId, openRegRemainder, false, now)
        setAmountAndChecked(retRegId, 0, true, now)
    }

    /**
     * Filtered + paginated table query built in the repository (filters are
     * dynamic AND-combinations -- SCOPE.md). Also serves the unfiltered case.
     */
    @RawQuery(observedEntities = [RegistryEntity::class, PersonEntity::class, SourceEntity::class])
    fun observeRows(query: SupportSQLiteQuery): Flow<List<RegistryRowResult>>

    @RawQuery(observedEntities = [RegistryEntity::class])
    fun observeCount(query: SupportSQLiteQuery): Flow<Int>

    /** Net outstanding: unchecked normal minus unchecked returns (returns count negative). */
    @Query(
        """
        SELECT COALESCE(SUM(CASE
            WHEN type = ${RegistryEntity.TYPE_NORMAL} THEN amount
            WHEN type = ${RegistryEntity.TYPE_RETURN} THEN -amount
            ELSE 0 END), 0)
        FROM registries
        WHERE track_id = :trackId AND checked = 0 AND deleted_at IS NULL
        """,
    )
    fun observeOutstandingTotal(trackId: String): Flow<Long>

    /** Debtor graph: unchecked normal regs only (outstanding debt). */
    @Query(
        """
        SELECT r.person_id AS id, p.name AS name, p.color AS color, SUM(r.amount) AS total
        FROM registries r JOIN people p ON p.id = r.person_id
        WHERE r.track_id = :trackId AND r.type = ${RegistryEntity.TYPE_NORMAL}
          AND r.checked = 0 AND r.deleted_at IS NULL
        GROUP BY r.person_id, p.name, p.color
        ORDER BY total DESC
        """,
    )
    fun observeDebtorSlices(trackId: String): Flow<List<SliceResult>>

    /** Source graph: unchecked normal regs only; independent of the debtor graph. */
    @Query(
        """
        SELECT r.source_id AS id, s.name AS name, s.color AS color, SUM(r.amount) AS total
        FROM registries r JOIN sources s ON s.id = r.source_id
        WHERE r.track_id = :trackId AND r.type = ${RegistryEntity.TYPE_NORMAL}
          AND r.checked = 0 AND r.deleted_at IS NULL
        GROUP BY r.source_id, s.name, s.color
        ORDER BY total DESC
        """,
    )
    fun observeSourceSlices(trackId: String): Flow<List<SliceResult>>
}
