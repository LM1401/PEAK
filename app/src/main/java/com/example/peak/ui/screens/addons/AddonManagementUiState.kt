package com.example.peak.ui.screens.addons

import com.example.peak.domain.model.Addon

/**
 * UI State model for the Add-on Management screen.
 */
data class AddonManagementUiState(
    val installedAddons: List<Addon> = emptyList(),
    val isLoading: Boolean = false,
    val retryingAddonId: String? = null,
    val showAddDialog: Boolean = false,
    val addonToDelete: Addon? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
