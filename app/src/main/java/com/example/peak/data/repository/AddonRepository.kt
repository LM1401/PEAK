package com.example.peak.data.repository

import com.example.peak.data.addon.AddonStorage
import com.example.peak.domain.model.Addon
import com.example.peak.domain.model.AddonManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Thread-safe repository managing installed Stremio-compatible add-ons.
 * Single reactive source of truth for add-on collection state across PEAK.
 */
class AddonRepository(
    private val storage: AddonStorage
) {
    private val mutex = Mutex()

    private val _addons = MutableStateFlow<List<Addon>>(emptyList())
    val addons: StateFlow<List<Addon>> = _addons.asStateFlow()

    init {
        _addons.value = storage.getAllAddons()
    }

    suspend fun addAddon(
        transportUrl: String,
        name: String? = null,
        description: String? = null
    ): Result<Addon> = mutex.withLock {
        val trimmed = transportUrl.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Add-on URL cannot be blank"))
        }

        val normalized = AddonStorage.normalizeAddonUrl(trimmed)
        if (_addons.value.any { AddonStorage.normalizeAddonUrl(it.transportUrl) == normalized }) {
            return Result.failure(IllegalArgumentException("This add-on is already installed"))
        }

        val newAddon = Addon(
            id = UUID.randomUUID().toString(),
            transportUrl = normalized,
            name = name,
            description = description
        )

        val updatedList = withContext(Dispatchers.IO) {
            storage.addAddon(newAddon)
        }

        _addons.value = updatedList
        return Result.success(newAddon)
    }

    suspend fun updateAddonManifest(id: String, manifest: AddonManifest): Result<Addon> = mutex.withLock {
        val current = _addons.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Add-on not found"))
        }

        val target = current[index]
        val updated = target.copy(
            manifestId = manifest.id,
            name = manifest.name,
            description = manifest.description ?: target.description,
            version = manifest.version,
            manifest = manifest
        )

        current[index] = updated
        withContext(Dispatchers.IO) {
            storage.saveAllAddons(current)
        }

        _addons.value = current
        return Result.success(updated)
    }

    suspend fun fetchAndInstallManifest(id: String, apiClient: com.example.peak.data.addon.AddonApiClient): Result<AddonManifest> = mutex.withLock {
        val current = _addons.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Add-on not found"))
        }

        val target = current[index]
        val fetchResult = apiClient.fetchManifest(target.transportUrl)

        fetchResult.onSuccess { manifest ->
            val updated = target.copy(
                manifestId = manifest.id,
                name = manifest.name,
                description = manifest.description ?: target.description,
                version = manifest.version,
                manifest = manifest
            )
            current[index] = updated
            withContext(Dispatchers.IO) {
                storage.saveAllAddons(current)
            }
            _addons.value = current
        }

        return fetchResult
    }

    suspend fun toggleAddon(id: String, enabled: Boolean): Result<Addon> = mutex.withLock {
        val current = _addons.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Add-on not found"))
        }

        val target = current[index]
        val updated = target.copy(isEnabled = enabled)
        current[index] = updated

        withContext(Dispatchers.IO) {
            storage.saveAllAddons(current)
        }

        _addons.value = current
        return Result.success(updated)
    }

    suspend fun removeAddon(id: String): Result<Boolean> = mutex.withLock {
        val current = _addons.value
        if (current.none { it.id == id }) {
            return Result.success(false)
        }

        val updatedList = withContext(Dispatchers.IO) {
            storage.removeAddon(id)
        }

        _addons.value = updatedList
        return Result.success(true)
    }

    fun isAddonInstalled(transportUrl: String): Boolean {
        return storage.isAddonInstalled(transportUrl)
    }
}
