package com.example.peak.ui.screens.addons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.addon.AddonApiClient
import com.example.peak.data.repository.AddonRepository
import com.example.peak.domain.model.Addon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI interactions, installation progress, and manifest retrieval for Add-ons.
 */
class AddonViewModel(
    private val addonRepository: AddonRepository,
    private val addonApiClient: AddonApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddonManagementUiState())
    val uiState: StateFlow<AddonManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            addonRepository.addons.collectLatest { addons ->
                _uiState.update { currentState ->
                    currentState.copy(installedAddons = addons)
                }
            }
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show, errorMessage = null) }
    }

    fun showDeleteConfirm(addon: Addon?) {
        _uiState.update { it.copy(addonToDelete = addon, errorMessage = null) }
    }

    fun installAddon(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Add-on URL cannot be blank") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val addResult = addonRepository.addAddon(trimmed)
            addResult.onSuccess { newAddon ->
                // Attempt manifest retrieval asynchronously
                val manifestResult = addonRepository.fetchAndInstallManifest(newAddon.id, addonApiClient)
                manifestResult.onSuccess { manifest ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showAddDialog = false,
                            successMessage = "Successfully installed ${manifest.name}"
                        )
                    }
                }.onFailure { ex ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showAddDialog = false,
                            errorMessage = "Installed add-on, but failed to fetch manifest: ${ex.message}"
                        )
                    }
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Failed to install add-on"
                    )
                }
            }
        }
    }

    fun retryManifest(addonId: String) {
        _uiState.update { it.copy(retryingAddonId = addonId, errorMessage = null, successMessage = null) }

        viewModelScope.launch {
            val result = addonRepository.fetchAndInstallManifest(addonId, addonApiClient)
            result.onSuccess { manifest ->
                _uiState.update {
                    it.copy(
                        retryingAddonId = null,
                        successMessage = "Retrieved manifest for ${manifest.name}"
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        retryingAddonId = null,
                        errorMessage = "Failed to retrieve manifest: ${ex.message}"
                    )
                }
            }
        }
    }

    fun toggleAddon(addonId: String, enabled: Boolean) {
        viewModelScope.launch {
            addonRepository.toggleAddon(addonId, enabled)
        }
    }

    fun deleteAddon(addonId: String) {
        viewModelScope.launch {
            val result = addonRepository.removeAddon(addonId)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        addonToDelete = null,
                        successMessage = "Add-on removed successfully"
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        addonToDelete = null,
                        errorMessage = ex.message ?: "Failed to remove add-on"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

class AddonViewModelFactory(
    private val addonRepository: AddonRepository,
    private val addonApiClient: AddonApiClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddonViewModel(addonRepository, addonApiClient) as T
    }
}
