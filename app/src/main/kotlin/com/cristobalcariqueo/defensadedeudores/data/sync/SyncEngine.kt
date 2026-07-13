package com.cristobalcariqueo.defensadedeudores.data.sync

import com.cristobalcariqueo.defensadedeudores.data.local.dao.SyncDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncConflictEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncStateEntity
import com.cristobalcariqueo.defensadedeudores.data.remote.PersonDto
import com.cristobalcariqueo.defensadedeudores.data.remote.RegistryDto
import com.cristobalcariqueo.defensadedeudores.data.remote.SettingsDto
import com.cristobalcariqueo.defensadedeudores.data.remote.SourceDto
import com.cristobalcariqueo.defensadedeudores.data.remote.TrackDto
import com.cristobalcariqueo.defensadedeudores.data.remote.TrackPersonDto
import com.cristobalcariqueo.defensadedeudores.data.remote.toDto
import com.cristobalcariqueo.defensadedeudores.data.remote.toEntity
import com.cristobalcariqueo.defensadedeudores.data.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Running : SyncStatus
    data class Done(val at: Instant) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/** Which side wins when resolving a conflict; MERGED carries a field-by-field DTO json. */
sealed interface ConflictResolution {
    data object KeepLocal : ConflictResolution
    data object KeepRemote : ConflictResolution
    data class Merged(val mergedJson: String) : ConflictResolution
}

/**
 * Pull-then-push sync between the local Room store and Supabase
 * (DATA_MODEL.md "Offline & sync"). Per-row conflict base = the server
 * updated_at stored at last sync; divergence lands in sync_conflicts for the
 * resolution screen instead of being silently overwritten.
 */
class SyncEngine(
    private val client: SupabaseClient,
    private val syncDao: SyncDao,
    private val authRepository: AuthRepository,
    private val connectivity: ConnectivityObserver,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Auto-sync on sign-in and whenever a validated connection (re)appears. */
    fun start(scope: CoroutineScope) {
        combine(authRepository.observeUser(), connectivity.isOnline) { user, online ->
            user != null && online
        }
            .distinctUntilChanged()
            .onEach { ready -> if (ready) sync() }
            .launchIn(scope)
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun sync() {
        val user = authRepository.currentUser() ?: return
        mutex.withLock {
            _status.value = SyncStatus.Running
            try {
                pushDeletes()
                pullPeople(user.id)
                pullSources(user.id)
                pullTracks(user.id)
                pullRegistries(user.id)
                pullSettings(user.id)
                pushPeople(user.id)
                pushSources(user.id)
                pushTracks(user.id)
                pushRegistries(user.id)
                pushSettings(user.id)
                _status.value = SyncStatus.Done(Clock.System.now())
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: e::class.simpleName.orEmpty())
            }
        }
    }

    // ---- pull ----------------------------------------------------------

    private suspend fun pullPeople(userId: String) {
        val watermark = watermark(TABLE_PEOPLE)
        val rows = client.from(TABLE_PEOPLE).select {
            filter { gt(COL_UPDATED_AT, watermark) }
        }.decodeList<PersonDto>()
        var newest = watermark
        for (dto in rows) {
            newest = maxIso(newest, dto.updatedAt)
            val local = syncDao.personById(dto.id)
            when {
                local == null || !local.dirty -> syncDao.applyPerson(dto.toEntity())
                local.remoteUpdatedAt == dto.updatedAt.toEpochMs() -> Unit // base unchanged; local wins until push
                else -> recordConflict(
                    TABLE_PEOPLE,
                    dto.id,
                    json.encodeToString(local.toDto(userId)),
                    json.encodeToString(dto),
                )
            }
        }
        saveWatermark(TABLE_PEOPLE, watermark, newest)
    }

    private suspend fun pullSources(userId: String) {
        val watermark = watermark(TABLE_SOURCES)
        val rows = client.from(TABLE_SOURCES).select {
            filter { gt(COL_UPDATED_AT, watermark) }
        }.decodeList<SourceDto>()
        var newest = watermark
        for (dto in rows) {
            newest = maxIso(newest, dto.updatedAt)
            val local = syncDao.sourceById(dto.id)
            when {
                local == null || !local.dirty -> syncDao.applySource(dto.toEntity())
                local.remoteUpdatedAt == dto.updatedAt.toEpochMs() -> Unit
                else -> recordConflict(
                    TABLE_SOURCES,
                    dto.id,
                    json.encodeToString(local.toDto(userId)),
                    json.encodeToString(dto),
                )
            }
        }
        saveWatermark(TABLE_SOURCES, watermark, newest)
    }

    private suspend fun pullTracks(userId: String) {
        val watermark = watermark(TABLE_TRACKS)
        val rows = client.from(TABLE_TRACKS).select {
            filter { gt(COL_UPDATED_AT, watermark) }
        }.decodeList<TrackDto>()
        var newest = watermark
        for (dto in rows) {
            newest = maxIso(newest, dto.updatedAt)
            val local = syncDao.trackById(dto.id)
            when {
                local == null || !local.dirty -> syncDao.applyTrack(dto.toEntity())
                local.remoteUpdatedAt == dto.updatedAt.toEpochMs() -> Unit
                else -> recordConflict(
                    TABLE_TRACKS,
                    dto.id,
                    json.encodeToString(local.toDto(userId)),
                    json.encodeToString(dto),
                )
            }
        }
        saveWatermark(TABLE_TRACKS, watermark, newest)
        pullShortcuts()
    }

    /**
     * track_people has no updated_at: fetch the full set (small) and replace
     * locally, but only for tracks that are clean locally -- a dirty track's
     * shortcuts haven't been pushed yet and must not be clobbered.
     */
    private suspend fun pullShortcuts() {
        val remote = client.from(TABLE_TRACK_PEOPLE).select().decodeList<TrackPersonDto>()
        val byTrack = remote.groupBy { it.trackId }
        for ((trackId, dtos) in byTrack) {
            val local = syncDao.trackById(trackId)
            if (local == null || local.dirty) continue
            syncDao.clearShortcuts(trackId)
            syncDao.insertShortcuts(dtos.map(TrackPersonDto::toEntity))
        }
    }

    private suspend fun pullRegistries(userId: String) {
        val watermark = watermark(TABLE_REGISTRIES)
        val rows = client.from(TABLE_REGISTRIES).select {
            filter { gt(COL_UPDATED_AT, watermark) }
        }.decodeList<RegistryDto>()
        var newest = watermark
        for (dto in rows) {
            newest = maxIso(newest, dto.updatedAt)
            val local = syncDao.registryById(dto.id)
            when {
                local == null || !local.dirty -> syncDao.applyRegistry(dto.toEntity())
                local.remoteUpdatedAt == dto.updatedAt.toEpochMs() -> Unit
                else -> recordConflict(
                    TABLE_REGISTRIES,
                    dto.id,
                    json.encodeToString(local.toDto(userId)),
                    json.encodeToString(dto),
                )
            }
        }
        saveWatermark(TABLE_REGISTRIES, watermark, newest)
    }

    private suspend fun pullSettings(userId: String) {
        val dto = client.from(TABLE_SETTINGS).select {
            filter { eq("user_id", userId) }
        }.decodeList<SettingsDto>().firstOrNull() ?: return
        val local = syncDao.settingsRow()
        when {
            local == null || !local.dirty -> syncDao.applySettings(dto.toEntity())
            // Never-synced local settings meeting the auto-provisioned remote
            // row is the normal first sign-in -- local wins, push overwrites.
            local.remoteUpdatedAt == null -> Unit
            local.remoteUpdatedAt == dto.updatedAt.toEpochMs() -> Unit
            else -> recordConflict(
                TABLE_SETTINGS,
                userId,
                json.encodeToString(local.toDto(userId, dto.updatedAt)),
                json.encodeToString(dto),
            )
        }
    }

    // ---- push ----------------------------------------------------------

    @Suppress("TooGenericExceptionCaught")
    private suspend fun pushDeletes() {
        for (pending in syncDao.pendingDeletes()) {
            try {
                client.from(pending.tableName).delete {
                    filter { eq("id", pending.rowId) }
                }
                syncDao.clearDelete(pending.tableName, pending.rowId)
            } catch (_: Exception) {
                // FK restriction or transient failure -- stays queued for the next run.
            }
        }
    }

    private suspend fun pushPeople(userId: String) {
        val dirty = syncDao.dirtyPeople().filterNot { hasConflict(TABLE_PEOPLE, it.id) }
        if (dirty.isEmpty()) return
        val returned = client.from(TABLE_PEOPLE).upsert(dirty.map { it.toDto(userId) }) {
            select(Columns.ALL)
        }.decodeList<PersonDto>().associateBy { it.id }
        for (row in dirty) {
            val remote = returned[row.id] ?: continue
            syncDao.markPersonSynced(row.id, remote.updatedAt.toEpochMs(), row.updatedAt)
        }
    }

    private suspend fun pushSources(userId: String) {
        val dirty = syncDao.dirtySources().filterNot { hasConflict(TABLE_SOURCES, it.id) }
        if (dirty.isEmpty()) return
        val returned = client.from(TABLE_SOURCES).upsert(dirty.map { it.toDto(userId) }) {
            select(Columns.ALL)
        }.decodeList<SourceDto>().associateBy { it.id }
        for (row in dirty) {
            val remote = returned[row.id] ?: continue
            syncDao.markSourceSynced(row.id, remote.updatedAt.toEpochMs(), row.updatedAt)
        }
    }

    private suspend fun pushTracks(userId: String) {
        val dirty = syncDao.dirtyTracks().filterNot { hasConflict(TABLE_TRACKS, it.id) }
        if (dirty.isEmpty()) return
        val returned = client.from(TABLE_TRACKS).upsert(dirty.map { it.toDto(userId) }) {
            select(Columns.ALL)
        }.decodeList<TrackDto>().associateBy { it.id }
        for (row in dirty) {
            val remote = returned[row.id] ?: continue
            // Shortcuts ride along with their track: replace the remote set.
            client.from(TABLE_TRACK_PEOPLE).delete { filter { eq("track_id", row.id) } }
            val shortcuts = syncDao.shortcutsFor(row.id).map { it.toDto() }
            if (shortcuts.isNotEmpty()) client.from(TABLE_TRACK_PEOPLE).insert(shortcuts)
            syncDao.markTrackSynced(row.id, remote.updatedAt.toEpochMs(), row.updatedAt)
        }
    }

    private suspend fun pushRegistries(userId: String) {
        val dirty = syncDao.dirtyRegistries().filterNot { hasConflict(TABLE_REGISTRIES, it.id) }
        if (dirty.isEmpty()) return
        val returned = client.from(TABLE_REGISTRIES).upsert(dirty.map { it.toDto(userId) }) {
            select(Columns.ALL)
        }.decodeList<RegistryDto>().associateBy { it.id }
        for (row in dirty) {
            val remote = returned[row.id] ?: continue
            syncDao.markRegistrySynced(row.id, remote.updatedAt.toEpochMs(), row.updatedAt)
        }
    }

    private suspend fun pushSettings(userId: String) {
        val local = syncDao.dirtySettings().firstOrNull() ?: return
        if (hasConflict(TABLE_SETTINGS, userId)) return
        val returned = client.from(TABLE_SETTINGS).upsert(
            local.toDto(userId, Clock.System.now().toString()),
        ) {
            select(Columns.ALL)
        }.decodeList<SettingsDto>().firstOrNull() ?: return
        syncDao.markSettingsSynced(returned.updatedAt.toEpochMs())
    }

    // ---- conflict resolution --------------------------------------------

    suspend fun resolve(conflictId: Long, resolution: ConflictResolution) {
        val conflict = syncDao.conflictById(conflictId) ?: return
        val remoteBase = remoteUpdatedAtOf(conflict.tableName, conflict.remoteJson)
        when (resolution) {
            ConflictResolution.KeepRemote -> applyJson(conflict.tableName, conflict.remoteJson, dirty = false)
            ConflictResolution.KeepLocal -> markLocalWinning(conflict.tableName, conflict.rowId, remoteBase)
            is ConflictResolution.Merged -> applyJson(
                conflict.tableName,
                resolution.mergedJson,
                dirty = true,
                remoteBaseOverride = remoteBase,
            )
        }
        syncDao.deleteConflict(conflictId)
        sync()
    }

    private suspend fun markLocalWinning(table: String, rowId: String, remoteBase: Long?) {
        when (table) {
            TABLE_PEOPLE -> syncDao.personById(rowId)?.let {
                syncDao.applyPerson(it.copy(dirty = true, remoteUpdatedAt = remoteBase))
            }
            TABLE_SOURCES -> syncDao.sourceById(rowId)?.let {
                syncDao.applySource(it.copy(dirty = true, remoteUpdatedAt = remoteBase))
            }
            TABLE_TRACKS -> syncDao.trackById(rowId)?.let {
                syncDao.applyTrack(it.copy(dirty = true, remoteUpdatedAt = remoteBase))
            }
            TABLE_REGISTRIES -> syncDao.registryById(rowId)?.let {
                syncDao.applyRegistry(it.copy(dirty = true, remoteUpdatedAt = remoteBase))
            }
            TABLE_SETTINGS -> syncDao.settingsRow()?.let {
                syncDao.applySettings(it.copy(dirty = true, remoteUpdatedAt = remoteBase))
            }
        }
    }

    private suspend fun applyJson(
        table: String,
        dtoJson: String,
        dirty: Boolean,
        remoteBaseOverride: Long? = null,
    ) {
        when (table) {
            TABLE_PEOPLE -> {
                val entity = json.decodeFromString<PersonDto>(dtoJson).toEntity()
                syncDao.applyPerson(
                    entity.copy(dirty = dirty, remoteUpdatedAt = remoteBaseOverride ?: entity.remoteUpdatedAt),
                )
            }
            TABLE_SOURCES -> {
                val entity = json.decodeFromString<SourceDto>(dtoJson).toEntity()
                syncDao.applySource(
                    entity.copy(dirty = dirty, remoteUpdatedAt = remoteBaseOverride ?: entity.remoteUpdatedAt),
                )
            }
            TABLE_TRACKS -> {
                val entity = json.decodeFromString<TrackDto>(dtoJson).toEntity()
                syncDao.applyTrack(
                    entity.copy(dirty = dirty, remoteUpdatedAt = remoteBaseOverride ?: entity.remoteUpdatedAt),
                )
            }
            TABLE_REGISTRIES -> {
                val entity = json.decodeFromString<RegistryDto>(dtoJson).toEntity()
                syncDao.applyRegistry(
                    entity.copy(dirty = dirty, remoteUpdatedAt = remoteBaseOverride ?: entity.remoteUpdatedAt),
                )
            }
            TABLE_SETTINGS -> {
                val entity = json.decodeFromString<SettingsDto>(dtoJson).toEntity()
                syncDao.applySettings(
                    entity.copy(dirty = dirty, remoteUpdatedAt = remoteBaseOverride ?: entity.remoteUpdatedAt),
                )
            }
        }
    }

    private fun remoteUpdatedAtOf(table: String, remoteJson: String): Long? = when (table) {
        TABLE_PEOPLE -> json.decodeFromString<PersonDto>(remoteJson).updatedAt.toEpochMs()
        TABLE_SOURCES -> json.decodeFromString<SourceDto>(remoteJson).updatedAt.toEpochMs()
        TABLE_TRACKS -> json.decodeFromString<TrackDto>(remoteJson).updatedAt.toEpochMs()
        TABLE_REGISTRIES -> json.decodeFromString<RegistryDto>(remoteJson).updatedAt.toEpochMs()
        TABLE_SETTINGS -> json.decodeFromString<SettingsDto>(remoteJson).updatedAt.toEpochMs()
        else -> null
    }

    // ---- helpers ---------------------------------------------------------

    private suspend fun watermark(table: String): String =
        syncDao.state(table)?.pulledUntil ?: EPOCH_ISO

    private suspend fun saveWatermark(table: String, old: String, new: String) {
        if (new != old) syncDao.upsertState(SyncStateEntity(table, new))
    }

    private suspend fun hasConflict(table: String, rowId: String): Boolean =
        syncDao.conflictCountFor(table, rowId) > 0

    private suspend fun recordConflict(table: String, rowId: String, localJson: String, remoteJson: String) {
        if (hasConflict(table, rowId)) return
        syncDao.insertConflict(
            SyncConflictEntity(
                tableName = table,
                rowId = rowId,
                localJson = localJson,
                remoteJson = remoteJson,
                detectedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    private fun maxIso(a: String, b: String): String =
        if (Instant.parse(b) > Instant.parse(a)) b else a

    private fun String.toEpochMs(): Long = Instant.parse(this).toEpochMilliseconds()

    companion object {
        const val TABLE_TRACKS = "tracks"
        const val TABLE_TRACK_PEOPLE = "track_people"
        const val TABLE_PEOPLE = "people"
        const val TABLE_SOURCES = "sources"
        const val TABLE_REGISTRIES = "registries"
        const val TABLE_SETTINGS = "settings"
        private const val COL_UPDATED_AT = "updated_at"
        private const val EPOCH_ISO = "1970-01-01T00:00:00Z"
    }
}
