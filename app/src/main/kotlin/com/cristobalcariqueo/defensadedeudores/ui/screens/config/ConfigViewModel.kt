package com.cristobalcariqueo.defensadedeudores.ui.screens.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.domain.model.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<Settings?> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun setDarkTheme(enabled: Boolean) = update { it.copy(darkTheme = enabled) }

    fun setLanguage(tag: String) = update { it.copy(language = tag) }

    fun setFont(key: String) = update { it.copy(font = key) }

    fun setReturnBgColor(hex: String) = update { it.copy(returnBgColor = hex) }

    fun setRecentTableSize(size: Int) = update { it.copy(recentTableSize = size) }

    fun setHistoricalTableSize(size: Int) = update { it.copy(historicalTableSize = size) }

    private fun update(transform: (Settings) -> Settings) {
        val current = settings.value ?: return
        viewModelScope.launch { repository.update(transform(current)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
