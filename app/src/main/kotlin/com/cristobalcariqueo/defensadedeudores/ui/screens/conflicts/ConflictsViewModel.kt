package com.cristobalcariqueo.defensadedeudores.ui.screens.conflicts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.local.dao.SyncDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncConflictEntity
import com.cristobalcariqueo.defensadedeudores.data.sync.ConflictResolution
import com.cristobalcariqueo.defensadedeudores.data.sync.SyncEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** One field of a conflicted row, shown when both sides differ. */
data class ConflictField(val key: String, val localValue: String, val remoteValue: String)

class ConflictsViewModel(
    syncDao: SyncDao,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val conflicts: StateFlow<List<SyncConflictEntity>> = syncDao.observeConflicts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun resolveLocal(conflictId: Long) {
        viewModelScope.launch { syncEngine.resolve(conflictId, ConflictResolution.KeepLocal) }
    }

    fun resolveRemote(conflictId: Long) {
        viewModelScope.launch { syncEngine.resolve(conflictId, ConflictResolution.KeepRemote) }
    }

    /** Fields where local and remote disagree -- the field-by-field picker's rows. */
    fun differingFields(conflict: SyncConflictEntity): List<ConflictField> {
        val local = json.parseToJsonElement(conflict.localJson).jsonObject
        val remote = json.parseToJsonElement(conflict.remoteJson).jsonObject
        return (local.keys + remote.keys).distinct().mapNotNull { key ->
            val l = local[key]?.toString() ?: "null"
            val r = remote[key]?.toString() ?: "null"
            if (l == r) null else ConflictField(key = key, localValue = l, remoteValue = r)
        }
    }

    /**
     * Merge: start from the remote row (it has every column) and overwrite the
     * fields the user kept local with the local values.
     */
    fun resolveMerged(conflict: SyncConflictEntity, keepLocalKeys: Set<String>) {
        val local = json.parseToJsonElement(conflict.localJson).jsonObject
        val remote = json.parseToJsonElement(conflict.remoteJson).jsonObject
        val merged = JsonObject(
            remote.toMutableMap().apply {
                for (key in keepLocalKeys) local[key]?.let { put(key, it) }
            },
        )
        viewModelScope.launch {
            syncEngine.resolve(conflict.id, ConflictResolution.Merged(merged.toString()))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
