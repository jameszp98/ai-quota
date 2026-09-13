package com.jameszp98.aiquota.ui.settings

import android.app.Application
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jameszp98.aiquota.data.ProviderRegistry
import com.jameszp98.aiquota.data.storage.CursorTokenState
import com.jameszp98.aiquota.data.storage.SettingsStore
import com.jameszp98.aiquota.data.storage.TokenStorage
import com.jameszp98.aiquota.domain.ProviderUsage
import com.jameszp98.aiquota.ui.widget.QuotaWidget
import com.jameszp98.aiquota.ui.widget.WidgetState
import com.jameszp98.aiquota.ui.widget.WidgetStateDefinition
import com.jameszp98.aiquota.worker.UsageRefreshWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStorage = TokenStorage(application)
    private val settingsStore = SettingsStore(application)
    private val registry = ProviderRegistry.getInstance(application)

    val cursorTokenState: StateFlow<CursorTokenState> = tokenStorage.cursorTokenState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CursorTokenState.NoToken)

    val demoMode: StateFlow<Boolean> = settingsStore.demoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val refreshInterval: StateFlow<Int> = settingsStore.refreshIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    private val _usageState = MutableStateFlow<UsageUiState>(UsageUiState.Idle)
    val usageState: StateFlow<UsageUiState> = _usageState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun saveCursorBearerToken(token: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                tokenStorage.saveCursorBearerToken(token)
                _saveState.value = SaveState.Success
                refreshUsage()
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Failed to save token")
            }
        }
    }

    fun saveCursorSessionToken(token: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                tokenStorage.saveCursorSessionToken(token)
                _saveState.value = SaveState.Success
                refreshUsage()
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Failed to save token")
            }
        }
    }

    fun clearCursorTokens() {
        viewModelScope.launch {
            tokenStorage.clearCursorTokens()
            _usageState.value = UsageUiState.Idle
        }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDemoMode(enabled)
            refreshUsage()
        }
    }

    fun setRefreshInterval(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setRefreshInterval(minutes)
            UsageRefreshWorker.schedulePeriodicRefresh(getApplication(), minutes)
        }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            _usageState.value = UsageUiState.Loading

            val demoModeEnabled = demoMode.value
            val providers = registry.fetchAllUsage(demoModeEnabled)

            val hasError = providers.any { it.hasError }
            _usageState.value = if (hasError) {
                val errorMsg = providers.firstOrNull { it.hasError }?.errorMessage
                UsageUiState.Error(errorMsg ?: "Unknown error")
            } else {
                UsageUiState.Success(providers)
            }

            updateWidgets(providers, demoModeEnabled)
        }
    }

    private suspend fun updateWidgets(providers: List<ProviderUsage>, isDemo: Boolean) {
        val state = WidgetState(
            providers = providers,
            lastUpdated = Instant.now(),
            isDemo = isDemo
        )

        val manager = GlanceAppWidgetManager(getApplication())
        val glanceIds = manager.getGlanceIds(QuotaWidget::class.java)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = getApplication(),
                definition = WidgetStateDefinition,
                glanceId = glanceId
            ) {
                state
            }
            QuotaWidget().update(getApplication(), glanceId)
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

sealed class UsageUiState {
    data object Idle : UsageUiState()
    data object Loading : UsageUiState()
    data class Success(val providers: List<ProviderUsage>) : UsageUiState()
    data class Error(val message: String) : UsageUiState()
}

sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data object Success : SaveState()
    data class Error(val message: String) : SaveState()
}
