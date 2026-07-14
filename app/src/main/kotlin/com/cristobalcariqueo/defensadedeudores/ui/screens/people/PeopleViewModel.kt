package com.cristobalcariqueo.defensadedeudores.ui.screens.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.PersonRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeopleViewModel(private val repository: PersonRepository) : ViewModel() {

    val people: StateFlow<List<Person>> = repository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.create(trimmed) }
    }

    fun rename(personId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.update(personId, trimmed) }
    }

    /** Hard-deletes if the person has no registries, soft-deletes otherwise. */
    fun delete(personId: String) {
        viewModelScope.launch { repository.delete(personId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
