package com.example.peak.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState(
        items = listOf(
            SettingsItem(
                id = "auto_play",
                title = "Auto-play next episode",
                subtitle = "Automatically start the next episode in a series",
                type = SettingsItemType.TOGGLE,
                isEnabled = settingsRepository.autoPlayEnabled.value
            ),
            SettingsItem(
                id = "debug_mode",
                title = "Debug mode",
                subtitle = "Show technical information during playback",
                type = SettingsItemType.TOGGLE,
                isEnabled = settingsRepository.debugModeEnabled.value
            ),
            SettingsItem(
                id = "clear_cache",
                title = "Clear cache",
                subtitle = "Free up space by removing cached images and data",
                type = SettingsItemType.ACTION
            )
        )
    ))
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.autoPlayEnabled,
                settingsRepository.debugModeEnabled
            ) { autoPlay, debugMode ->
                Pair(autoPlay, debugMode)
            }.collectLatest { (autoPlay, debugMode) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        items = currentState.items.map { item ->
                            when (item.id) {
                                "auto_play" -> item.copy(isEnabled = autoPlay)
                                "debug_mode" -> item.copy(isEnabled = debugMode)
                                else -> item
                            }
                        }
                    )
                }
            }
        }
    }

    fun onToggleItem(id: String) {
        val currentItem = _uiState.value.items.find { it.id == id } ?: return
        val newEnabled = !currentItem.isEnabled

        viewModelScope.launch {
            when (id) {
                "auto_play" -> settingsRepository.setAutoPlayEnabled(newEnabled)
                "debug_mode" -> settingsRepository.setDebugModeEnabled(newEnabled)
            }
        }
    }

    fun onActionItem(id: String, context: Context? = null) {
        when (id) {
            "clear_cache" -> {
                if (context != null) {
                    viewModelScope.launch {
                        settingsRepository.clearAppCache(context)
                    }
                }
            }
        }
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsRepository) as T
    }
}
