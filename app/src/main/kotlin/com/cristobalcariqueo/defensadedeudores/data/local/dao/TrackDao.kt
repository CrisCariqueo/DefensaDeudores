package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackSourceEntity
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

    /** Related sources for the track's quick-create area; empty relation = all sources are offered. */
    @Query(
        """
        SELECT s.* FROM sources s
        JOIN track_sources ts ON ts.source_id = s.id
        WHERE ts.track_id = :trackId AND s.deleted_at IS NULL
        ORDER BY s.name COLLATE NOCASE
        """,
    )
    fun observeRelatedSources(trackId: String): Flow<List<SourceEntity>>

    // -- track config screen: membership management. Shortcut/source sets ride
    // -- along on track pushes, so every change also marks the track dirty.

    @Query("UPDATE tracks SET updated_at = :now, dirty = 1 WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    @Insert
    suspend fun insertShortcut(row: TrackPersonEntity)

    @Query("DELETE FROM track_people WHERE track_id = :trackId AND person_id = :personId")
    suspend fun deleteShortcut(trackId: String, personId: String)

    @Insert
    suspend fun insertTrackSource(row: TrackSourceEntity)

    @Query("DELETE FROM track_sources WHERE track_id = :trackId AND source_id = :sourceId")
    suspend fun deleteTrackSource(trackId: String, sourceId: String)

    @Transaction
    suspend fun addShortcut(trackId: String, personId: String, now: Long) {
        insertShortcut(TrackPersonEntity(trackId = trackId, personId = personId))
        touch(trackId, now)
    }

    @Transaction
    suspend fun removeShortcut(trackId: String, personId: String, now: Long) {
        deleteShortcut(trackId, personId)
        touch(trackId, now)
    }

    @Transaction
    suspend fun addRelatedSource(trackId: String, sourceId: String, now: Long) {
        insertTrackSource(TrackSourceEntity(trackId = trackId, sourceId = sourceId))
        touch(trackId, now)
    }

    @Transaction
    suspend fun removeRelatedSource(trackId: String, sourceId: String, now: Long) {
        deleteTrackSource(trackId, sourceId)
        touch(trackId, now)
    }

    /** Usage guards: only unused members may be removed from a track. */
    @Query("SELECT COUNT(*) FROM registries WHERE track_id = :trackId AND person_id = :personId")
    suspend fun registryCountForPerson(trackId: String, personId: String): Int

    @Query("SELECT COUNT(*) FROM registries WHERE track_id = :trackId AND source_id = :sourceId")
    suspend fun registryCountForSource(trackId: String, sourceId: String): Int

    @Query("SELECT COUNT(*) FROM track_people WHERE track_id = :trackId")
    suspend fun shortcutCount(trackId: String): Int

    @Query("UPDATE tracks SET name = :name, updated_at = :now, dirty = 1 WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    /** Tracks are only ever soft-deleted -- registries keep pointing at them. */
    @Query("UPDATE tracks SET deleted_at = :now, updated_at = :now, dirty = 1 WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
