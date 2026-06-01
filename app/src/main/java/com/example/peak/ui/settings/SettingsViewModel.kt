package com.example.peak.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState(
        items = listOf(
            SettingsItem(
                id = "auto_play",
                title = "Auto-play next episode",
                subtitle = "Automatically start the next episode in a series",
                type = SettingsItemType.TOGGLE,
                isEnabled = true
            ),
            SettingsItem(
                id = "debug_mode",
                title = "Debug mode",
                subtitle = "Show technical information during playback",
                type = SettingsItemType.TOGGLE,
                isEnabled = false
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

    fun onToggleItem(id: String) {
        _uiState.update { currentState ->
            currentState.copy(
                items = currentState.items.map { item ->
                    if (item.id == id) {
                        item.copy(isEnabled = !item.isEnabled)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun onActionItem(id: String) {
        when (id) {
            "clear_cache" -> {
                // TODO: Implement clear cache logic
            }
        }
    }
}
