package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.RegistryEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SettingsEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncConflictEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncDeleteEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncStateEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackSourceEntity
import kotlinx.coroutines.flow.Flow

/** Sync-engine bookkeeping: watermarks, dirty-row reads, remote-row writes, conflicts, delete queue. */
@Dao
interface SyncDao {

    // ---- watermarks

    @Query("SELECT * FROM sync_state WHERE table_name = :table")
    suspend fun state(table: String): SyncStateEntity?

    @Upsert
    suspend fun upsertState(state: SyncStateEntity)

    // ---- dirty rows (push candidates)

    @Query("SELECT * FROM tracks WHERE dirty = 1")
    suspend fun dirtyTracks(): List<TrackEntity>

    @Query("SELECT * FROM people WHERE dirty = 1")
    suspend fun dirtyPeople(): List<PersonEntity>

    @Query("SELECT * FROM sources WHERE dirty = 1")
    suspend fun dirtySources(): List<SourceEntity>

    @Query("SELECT * FROM registries WHERE dirty = 1")
    suspend fun dirtyRegistries(): List<RegistryEntity>

    @Query("SELECT * FROM settings WHERE dirty = 1")
    suspend fun dirtySettings(): List<SettingsEntity>

    @Query("SELECT * FROM track_people WHERE track_id = :trackId")
    suspend fun shortcutsFor(trackId: String): List<TrackPersonEntity>

    @Query("SELECT * FROM track_sources WHERE track_id = :trackId")
    suspend fun trackSourcesFor(trackId: String): List<TrackSourceEntity>

    // ---- marking pushed

    @Query("UPDATE tracks SET dirty = 0, remote_updated_at = :remote WHERE id = :id AND updated_at = :localBase")
    suspend fun markTrackSynced(id: String, remote: Long, localBase: Long)

    @Query("UPDATE people SET dirty = 0, remote_updated_at = :remote WHERE id = :id AND updated_at = :localBase")
    suspend fun markPersonSynced(id: String, remote: Long, localBase: Long)

    @Query("UPDATE sources SET dirty = 0, remote_updated_at = :remote WHERE id = :id AND updated_at = :localBase")
    suspend fun markSourceSynced(id: String, remote: Long, localBase: Long)

    @Query(
        "UPDATE registries SET dirty = 0, remote_updated_at = :remote WHERE id = :id AND updated_at = :localBase",
    )
    suspend fun markRegistrySynced(id: String, remote: Long, localBase: Long)

    @Query(
        """
        UPDATE settings SET dirty = 0, remote_updated_at = :remote
        WHERE id = ${SettingsEntity.SINGLETON_ID} AND dirty = 1
        """,
    )
    suspend fun markSettingsSynced(remote: Long)

    // ---- applying pulled rows (REPLACE keeps the local PK)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applyTrack(row: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applyPerson(row: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applySource(row: SourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applyRegistry(row: RegistryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applySettings(row: SettingsEntity)

    @Query("DELETE FROM track_people WHERE track_id = :trackId")
    suspend fun clearShortcuts(trackId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShortcuts(rows: List<TrackPersonEntity>)

    @Query("DELETE FROM track_sources WHERE track_id = :trackId")
    suspend fun clearTrackSources(trackId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrackSources(rows: List<TrackSourceEntity>)

    // ---- row lookups for conflict decisions

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun trackById(id: String): TrackEntity?

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun personById(id: String): PersonEntity?

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun sourceById(id: String): SourceEntity?

    @Query("SELECT * FROM registries WHERE id = :id")
    suspend fun registryById(id: String): RegistryEntity?

    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SINGLETON_ID}")
    suspend fun settingsRow(): SettingsEntity?

    // ---- conflicts

    @Insert
    suspend fun insertConflict(conflict: SyncConflictEntity)

    @Query("SELECT * FROM sync_conflicts ORDER BY detected_at")
    fun observeConflicts(): Flow<List<SyncConflictEntity>>

    @Query("SELECT COUNT(*) FROM sync_conflicts")
    fun observeConflictCount(): Flow<Int>

    @Query("SELECT * FROM sync_conflicts WHERE id = :id")
    suspend fun conflictById(id: Long): SyncConflictEntity?

    @Query("SELECT COUNT(*) FROM sync_conflicts WHERE table_name = :table AND row_id = :rowId")
    suspend fun conflictCountFor(table: String, rowId: String): Int

    @Query("DELETE FROM sync_conflicts WHERE id = :id")
    suspend fun deleteConflict(id: Long)

    // ---- hard-delete queue

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun queueDelete(delete: SyncDeleteEntity)

    @Query("SELECT * FROM sync_deletes")
    suspend fun pendingDeletes(): List<SyncDeleteEntity>

    @Query("DELETE FROM sync_deletes WHERE table_name = :table AND row_id = :rowId")
    suspend fun clearDelete(table: String, rowId: String)
}
