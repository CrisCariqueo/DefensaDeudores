package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<SourceEntity>>

    @Insert
    suspend fun insert(source: SourceEntity)

    @Query("UPDATE sources SET name = :name, color = :color, updated_at = :now, dirty = 1 WHERE id = :id")
    suspend fun update(id: String, name: String, color: String?, now: Long)

    @Query("SELECT remote_updated_at FROM sources WHERE id = :id")
    suspend fun remoteUpdatedAt(id: String): Long?

    /** All rows count as a reference -- superseded and soft-deleted included. */
    @Query("SELECT COUNT(*) FROM registries WHERE source_id = :id")
    suspend fun registryCount(id: String): Int

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE sources SET deleted_at = :now, updated_at = :now, dirty = 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
