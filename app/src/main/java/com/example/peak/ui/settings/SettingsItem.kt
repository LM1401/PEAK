package com.example.peak.ui.settings

enum class SettingsItemType {
    TOGGLE,
    ACTION
}

data class SettingsItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: SettingsItemType,
    val isEnabled: Boolean = false
)
