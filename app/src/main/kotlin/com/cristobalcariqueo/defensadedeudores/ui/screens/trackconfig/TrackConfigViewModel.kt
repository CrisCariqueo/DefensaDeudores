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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Why a removal tap was rejected -- surfaces as a snackbar. */
enum class RemovalBlock { IN_USE }

/**
 * Per-track settings (v0.2.0): rename, delete, and DRAFT membership editing --
 * quick-create debtors and the optional related-sources set (empty = all
 * sources offered). Nothing persists until Apply. Members with registries in
 * the track can't be removed; zero debtors is allowed but the screen warns on
 * exit.
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

    private val savedDebtors: StateFlow<Set<String>> = trackRepository.observeShortcutPeople(trackId)
        .map { people -> people.map(Person::id).toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val savedSources: StateFlow<Set<String>> = trackRepository.observeRelatedSources(trackId)
        .map { sources -> sources.map(Source::id).toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Null = no local edits yet; falls through to the saved set. */
    private val _draftDebtors = MutableStateFlow<Set<String>?>(null)
    private val _draftSources = MutableStateFlow<Set<String>?>(null)

    val draftDebtors: StateFlow<Set<String>> =
        combine(_draftDebtors, savedDebtors) { draft, saved -> draft ?: saved }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val draftSources: StateFlow<Set<String>> =
        combine(_draftSources, savedSources) { draft, saved -> draft ?: saved }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val hasChanges: StateFlow<Boolean> = combine(
        _draftDebtors, _draftSources, savedDebtors, savedSources,
    ) { dd, ds, sd, ss -> (dd != null && dd != sd) || (ds != null && ds != ss) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _removalBlock = MutableStateFlow<RemovalBlock?>(null)
    val removalBlock: StateFlow<RemovalBlock?> = _removalBlock.asStateFlow()

    fun toggleDebtor(personId: String) {
        viewModelScope.launch {
            val current = draftDebtors.value
            if (personId in current) {
                if (trackRepository.personUsedInTrack(trackId, personId)) {
                    _removalBlock.value = RemovalBlock.IN_USE
                } else {
                    _draftDebtors.value = current - personId
                }
            } else {
                _draftDebtors.value = current + personId
            }
        }
    }

    fun toggleSource(sourceId: String) {
        viewModelScope.launch {
            val current = draftSources.value
            if (sourceId in current) {
                if (trackRepository.sourceUsedInTrack(trackId, sourceId)) {
                    _removalBlock.value = RemovalBlock.IN_USE
                } else {
                    _draftSources.value = current - sourceId
                }
            } else {
                _draftSources.value = current + sourceId
            }
        }
    }

    /** Empties the draft except members that are in use (they can't be removed). */
    fun clearDebtors() {
        viewModelScope.launch {
            _draftDebtors.value = draftDebtors.value
                .filter { trackRepository.personUsedInTrack(trackId, it) }.toSet()
        }
    }

    fun clearSources() {
        viewModelScope.launch {
            _draftSources.value = draftSources.value
                .filter { trackRepository.sourceUsedInTrack(trackId, it) }.toSet()
        }
    }

    fun apply() {
        viewModelScope.launch {
            val debtors = _draftDebtors.value
            if (debtors != null) {
                val saved = savedDebtors.value
                (debtors - saved).forEach { trackRepository.addShortcut(trackId, it) }
                (saved - debtors).forEach { trackRepository.removeShortcut(trackId, it) }
                _draftDebtors.value = null
            }
            val sources = _draftSources.value
            if (sources != null) {
                val saved = savedSources.value
                (sources - saved).forEach { trackRepository.addRelatedSource(trackId, it) }
                (saved - sources).forEach { trackRepository.removeRelatedSource(trackId, it) }
                _draftSources.value = null
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
