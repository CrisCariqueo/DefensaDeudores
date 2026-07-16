package com.cristobalcariqueo.defensadedeudores.ui.screens.starting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.SourceRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Onboarding: the account must create >= 1 person and >= 1 source before
 * leaving the Starting screen (SCOPE.md screen 0).
 */
class StartingViewModel(
    private val personRepository: PersonRepository,
    private val sourceRepository: SourceRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val people: StateFlow<List<Person>> = personRepository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val sources: StateFlow<List<Source>> = sourceRepository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val canContinue: StateFlow<Boolean> =
        combine(people, sources) { p, s -> p.isNotEmpty() && s.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun addPerson(name: String, color: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { personRepository.create(trimmed, color) }
    }

    fun addSource(name: String, color: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { sourceRepository.create(trimmed, color) }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.markOnboarded()
            onComplete()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
