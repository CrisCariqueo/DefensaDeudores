package com.cristobalcariqueo.defensadedeudores.ui.screens.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cristobalcariqueo.defensadedeudores.data.local.dao.SyncDao
import com.cristobalcariqueo.defensadedeudores.data.repository.AuthRepository
import com.cristobalcariqueo.defensadedeudores.data.repository.AuthUser
import com.cristobalcariqueo.defensadedeudores.data.repository.SettingsRepository
import com.cristobalcariqueo.defensadedeudores.data.sync.SyncEngine
import com.cristobalcariqueo.defensadedeudores.data.sync.SyncStatus
import com.cristobalcariqueo.defensadedeudores.domain.model.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(
    private val repository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val syncEngine: SyncEngine,
    syncDao: SyncDao,
) : ViewModel() {

    val settings: StateFlow<Settings?> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // ---- account & sync

    val user: StateFlow<AuthUser?> = authRepository.observeUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val syncStatus: StateFlow<SyncStatus> = syncEngine.status

    val conflictCount: StateFlow<Int> = syncDao.observeConflictCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    /** Sign-up with email confirmation enabled ends without a session -- surface "check your email". */
    private val _awaitingEmailConfirmation = MutableStateFlow(false)
    val awaitingEmailConfirmation: StateFlow<Boolean> = _awaitingEmailConfirmation.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            try {
                authRepository.signIn(email.trim(), password)
            } catch (e: Exception) {
                _authError.value = e.message ?: e::class.simpleName.orEmpty()
            } finally {
                _authBusy.value = false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            try {
                authRepository.signUp(email.trim(), password)
                _awaitingEmailConfirmation.value = authRepository.currentUser() == null
            } catch (e: Exception) {
                _authError.value = e.message ?: e::class.simpleName.orEmpty()
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun syncNow() {
        viewModelScope.launch { syncEngine.sync() }
    }

    // ---- settings

    fun setTheme(theme: String) = update { it.copy(theme = theme) }

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
