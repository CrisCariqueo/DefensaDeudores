package com.cristobalcariqueo.defensadedeudores.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.TrackRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val trackRepository: TrackRepository,
    personRepository: PersonRepository,
) : ViewModel() {

    val tracks: StateFlow<List<Track>> = trackRepository.observeTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    /** For the create-track dialog's debtor-shortcut picker. */
    val people: StateFlow<List<Person>> = personRepository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun createTrack(name: String, shortcutPersonIds: List<String>, onCreated: (Track) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || shortcutPersonIds.isEmpty()) return
        viewModelScope.launch {
            onCreated(trackRepository.create(trimmed, shortcutPersonIds))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
