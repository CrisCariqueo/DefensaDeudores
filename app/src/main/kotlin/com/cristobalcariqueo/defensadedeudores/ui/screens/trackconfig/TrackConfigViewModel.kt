package com.cristobalcariqueo.defensadedeudores.ui.screens.trackconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SourceRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.TrackRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Why a removal tap was rejected -- surfaces as a snackbar. */
enum class RemovalBlock { IN_USE, LAST_DEBTOR }

/**
 * Per-track settings (v1.1): rename, delete, quick-create debtor membership,
 * and the optional related-sources set (empty = all sources offered). Members
 * with registries in the track can't be removed; a track keeps >= 1 debtor.
 */
class TrackConfigViewModel(
    private val trackId: String,
    private val trackRepository: TrackRepository,
    personRepository: PersonRepository,
    sourceRepository: SourceRepository,
) : ViewModel() {

    val track: StateFlow<Track?> = trackRepository.observeTrack(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val allPeople: StateFlow<List<Person>> = personRepository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allSources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val shortcutIds: StateFlow<Set<String>> = trackRepository.observeShortcutPeople(trackId)
        .map { people -> people.map(Person::id).toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val relatedSourceIds: StateFlow<Set<String>> = trackRepository.observeRelatedSources(trackId)
        .map { sources -> sources.map(Source::id).toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    private val _removalBlock = MutableStateFlow<RemovalBlock?>(null)
    val removalBlock: StateFlow<RemovalBlock?> = _removalBlock.asStateFlow()

    fun toggleDebtor(personId: String) {
        viewModelScope.launch {
            if (personId in shortcutIds.value) {
                when {
                    trackRepository.shortcutCount(trackId) <= 1 ->
                        _removalBlock.value = RemovalBlock.LAST_DEBTOR
                    trackRepository.personUsedInTrack(trackId, personId) ->
                        _removalBlock.value = RemovalBlock.IN_USE
                    else -> trackRepository.removeShortcut(trackId, personId)
                }
            } else {
                trackRepository.addShortcut(trackId, personId)
            }
        }
    }

    fun toggleSource(sourceId: String) {
        viewModelScope.launch {
            if (sourceId in relatedSourceIds.value) {
                if (trackRepository.sourceUsedInTrack(trackId, sourceId)) {
                    _removalBlock.value = RemovalBlock.IN_USE
                } else {
                    trackRepository.removeRelatedSource(trackId, sourceId)
                }
            } else {
                trackRepository.addRelatedSource(trackId, sourceId)
            }
        }
    }

    fun clearRemovalBlock() {
        _removalBlock.value = null
    }

    fun rename(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { trackRepository.rename(trackId, trimmed) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            trackRepository.softDelete(trackId)
            onDeleted()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
