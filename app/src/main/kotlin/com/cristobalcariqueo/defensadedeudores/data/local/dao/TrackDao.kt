package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE deleted_at IS NULL ORDER BY created_at")
    fun observeActive(): Flow<List<TrackEntity>>

    @Insert
    suspend fun insert(track: TrackEntity)

    @Insert
    suspend fun insertShortcuts(shortcuts: List<TrackPersonEntity>)

    /** Track + its debtor shortcuts land atomically -- a track must never persist with zero shortcuts. */
    @Transaction
    suspend fun insertWithShortcuts(track: TrackEntity, shortcuts: List<TrackPersonEntity>) {
        insert(track)
        insertShortcuts(shortcuts)
    }

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun observeById(id: String): Flow<TrackEntity?>

    /** Quick-pick debtor shortcuts for the track's quick-create area. */
    @Query(
        """
        SELECT p.* FROM people p
        JOIN track_people tp ON tp.person_id = p.id
        WHERE tp.track_id = :trackId AND p.deleted_at IS NULL
        ORDER BY p.name COLLATE NOCASE
        """,
    )
    fun observeShortcutPeople(trackId: String): Flow<List<PersonEntity>>

    @Query("UPDATE tracks SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    /** Tracks are only ever soft-deleted -- registries keep pointing at them. */
    @Query("UPDATE tracks SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
