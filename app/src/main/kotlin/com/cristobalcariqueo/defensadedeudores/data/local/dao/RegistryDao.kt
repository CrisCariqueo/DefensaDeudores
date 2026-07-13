package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
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
    val total: Long,
)

@Dao
interface RegistryDao {
    @Insert
    suspend fun insert(registry: RegistryEntity)

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
        SELECT r.person_id AS id, p.name AS name, SUM(r.amount) AS total
        FROM registries r JOIN people p ON p.id = r.person_id
        WHERE r.track_id = :trackId AND r.type = ${RegistryEntity.TYPE_NORMAL}
          AND r.checked = 0 AND r.deleted_at IS NULL
        GROUP BY r.person_id, p.name
        ORDER BY total DESC
        """,
    )
    fun observeDebtorSlices(trackId: String): Flow<List<SliceResult>>

    /** Source graph: unchecked normal regs only; independent of the debtor graph. */
    @Query(
        """
        SELECT r.source_id AS id, s.name AS name, SUM(r.amount) AS total
        FROM registries r JOIN sources s ON s.id = r.source_id
        WHERE r.track_id = :trackId AND r.type = ${RegistryEntity.TYPE_NORMAL}
          AND r.checked = 0 AND r.deleted_at IS NULL
        GROUP BY r.source_id, s.name
        ORDER BY total DESC
        """,
    )
    fun observeSourceSlices(trackId: String): Flow<List<SliceResult>>
}
