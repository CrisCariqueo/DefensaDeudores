package com.cristobalcariqueo.defensadedeudores.ui.screens.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.SourceRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourcesViewModel(private val repository: SourceRepository) : ViewModel() {

    val sources: StateFlow<List<Source>> = repository.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun add(name: String, color: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.create(trimmed, color) }
    }

    fun rename(sourceId: String, name: String, color: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.update(sourceId, trimmed, color) }
    }

    /** Hard-deletes if the source has no registries, soft-deletes otherwise. */
    fun delete(sourceId: String) {
        viewModelScope.launch { repository.delete(sourceId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
