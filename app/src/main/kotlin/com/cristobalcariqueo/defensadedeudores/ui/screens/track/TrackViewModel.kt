package com.cristobalcariqueo.defensadedeudores.ui.screens.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.RegistryRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SourceRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.TrackRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.GraphSlice
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryFilter
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryWithNames
import com.cristobalcariqueo.defensadedeudores.domain.model.Settings
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModel(
    private val trackId: String,
    private val trackRepository: TrackRepository,
    personRepository: PersonRepository,
    sourceRepository: SourceRepository,
    settingsRepository: SettingsRepository,
    private val registryRepository: RegistryRepository,
) : ViewModel() {

    val track: StateFlow<Track?> = trackRepository.observeTrack(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Quick-pick debtors for the quick-create area. */
    val shortcuts: StateFlow<List<Person>> = trackRepository.observeShortcutPeople(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** All sources -- one quick-create button each. */
    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** All people, for the filter sheet -- registries aren't restricted to shortcuts. */
    val allPeople: StateFlow<List<Person>> = personRepository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val settings: StateFlow<Settings?> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _selectedDebtorId = MutableStateFlow<String?>(null)

    /** Selected quick-create debtor; defaults to the first shortcut. */
    val selectedDebtorId: StateFlow<String?> =
        combine(_selectedDebtorId, shortcuts) { selected, people ->
            selected?.takeIf { id -> people.any { it.id == id } } ?: people.firstOrNull()?.id
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _filter = MutableStateFlow(RegistryFilter())
    val filter: StateFlow<RegistryFilter> = _filter.asStateFlow()

    /** Recent-table row cap, seeded from settings once loaded, user-adjustable in place. */
    private val _recentSizeOverride = MutableStateFlow<Int?>(null)
    val recentSize: StateFlow<Int> =
        combine(_recentSizeOverride, settings) { override, s ->
            override ?: s?.recentTableSize ?: DEFAULT_RECENT
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DEFAULT_RECENT)

    val recentRows: StateFlow<List<RegistryWithNames>> =
        combine(_filter, recentSize) { f, size -> f to size }
            .flatMapLatest { (f, size) -> registryRepository.observeRows(trackId, f, size, 0) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _historyVisible = MutableStateFlow(false)
    val historyVisible: StateFlow<Boolean> = _historyVisible.asStateFlow()

    private val _historyPage = MutableStateFlow(0)
    val historyPage: StateFlow<Int> = _historyPage.asStateFlow()

    private val _historySizeOverride = MutableStateFlow<Int?>(null)
    val historySize: StateFlow<Int> =
        combine(_historySizeOverride, settings) { override, s ->
            override ?: s?.historicalTableSize ?: DEFAULT_HISTORY
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DEFAULT_HISTORY)

    val historyRows: StateFlow<List<RegistryWithNames>> =
        combine(_historyVisible, _filter, historySize, _historyPage) { visible, f, size, page ->
            Triple(visible, f, size to page)
        }.flatMapLatest { (visible, f, sizePage) ->
            val (size, page) = sizePage
            if (visible) registryRepository.observeRows(trackId, f, size, page * size) else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val historyCount: StateFlow<Int> = _filter
        .flatMapLatest { registryRepository.observeCount(trackId, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val debtorSlices: StateFlow<List<GraphSlice>> = registryRepository.observeDebtorSlices(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val sourceSlices: StateFlow<List<GraphSlice>> = registryRepository.observeSourceSlices(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val outstandingTotal: StateFlow<Long> = registryRepository.observeOutstandingTotal(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0L)

    fun selectDebtor(personId: String) {
        _selectedDebtorId.value = personId
    }

    fun setFilter(filter: RegistryFilter) {
        _filter.value = filter
        _historyPage.value = 0
    }

    fun setSearchQuery(query: String) {
        _filter.update { it.copy(query = query) }
        _historyPage.value = 0
    }

    fun setRecentSize(size: Int) {
        _recentSizeOverride.value = size
    }

    fun setHistorySize(size: Int) {
        _historySizeOverride.value = size
        _historyPage.value = 0
    }

    fun toggleHistory() {
        _historyVisible.update { !it }
    }

    fun setHistoryPage(page: Int) {
        _historyPage.value = page.coerceAtLeast(0)
    }

    fun createNormal(personId: String, sourceId: String, amount: Int, note: String?) {
        if (amount <= 0) return
        viewModelScope.launch {
            registryRepository.createNormal(trackId, personId, sourceId, amount, note?.ifBlank { null })
        }
    }

    fun renameTrack(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { trackRepository.rename(trackId, trimmed) }
    }

    fun deleteTrack(onDeleted: () -> Unit) {
        viewModelScope.launch {
            trackRepository.softDelete(trackId)
            onDeleted()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val DEFAULT_RECENT = 50
        const val DEFAULT_HISTORY = 100
    }
}
