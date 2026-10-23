package com.example.peak.data.repository

import com.example.peak.data.addon.AddonStorage
import com.example.peak.data.addon.AddonStreamApiClient
import com.example.peak.domain.model.Addon
import com.example.peak.domain.model.AddonManifest
import com.example.peak.domain.model.AddonStreamProviderResult
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
class AddonRepository @JvmOverloads constructor(
    private val storage: AddonStorage,
    private val streamApiClient: AddonStreamApiClient = AddonStreamApiClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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

        val updatedList = withContext(ioDispatcher) {
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
        withContext(ioDispatcher) {
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
            withContext(ioDispatcher) {
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

        withContext(ioDispatcher) {
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

        val updatedList = withContext(ioDispatcher) {
            storage.removeAddon(id)
        }

        _addons.value = updatedList
        return Result.success(true)
    }

    fun isAddonInstalled(transportUrl: String): Boolean {
        return storage.isAddonInstalled(transportUrl)
    }

    /**
     * Determines whether an add-on is enabled and declares support for streaming the requested content item.
     */
    fun Addon.supportsStream(mediaType: MediaType, contentId: String): Boolean {
        if (!isEnabled) return false
        val manifest = this.manifest ?: return false
        val stremioType = when (mediaType) {
            MediaType.MOVIE -> "movie"
            MediaType.TV -> "series"
        }

        val streamResource = manifest.resources.find { it.name.equals("stream", ignoreCase = true) }
            ?: return false

        val isTypeSupported = when {
            streamResource.types.isNotEmpty() -> streamResource.types.any { it.equals(stremioType, ignoreCase = true) }
            manifest.types.isNotEmpty() -> manifest.types.any { it.equals(stremioType, ignoreCase = true) }
            else -> true
        }
        if (!isTypeSupported) return false

        val prefixes = streamResource.idPrefixes.ifEmpty { manifest.idPrefixes }
        if (prefixes.isNotEmpty()) {
            val matchesPrefix = prefixes.any { prefix -> contentId.startsWith(prefix, ignoreCase = true) }
            if (!matchesPrefix) return false
        }

        return true
    }

    /**
     * Queries enabled, compatible add-ons concurrently to retrieve available streams for a content item.
     * Enforces explicit season and episode numbers for TV series requests. Isolates provider errors.
     */
    suspend fun getStreams(
        mediaType: MediaType,
        imdbId: String,
        season: Int? = null,
        episode: Int? = null
    ): Result<List<AddonStreamProviderResult>> = withContext(ioDispatcher) {
        val cleanImdbId = imdbId.trim()
        if (cleanImdbId.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("IMDb ID cannot be blank"))
        }

        val (stremioType, stremioId) = when (mediaType) {
            MediaType.MOVIE -> {
                "movie" to cleanImdbId
            }
            MediaType.TV -> {
                if (season == null || season <= 0 || episode == null || episode <= 0) {
                    return@withContext Result.failure(
                        IllegalArgumentException("TV series stream requests require explicit, valid season and episode numbers (> 0)")
                    )
                }
                "series" to "$cleanImdbId:$season:$episode"
            }
        }

        val availableAddons = addons.value.filter { it.supportsStream(mediaType, cleanImdbId) }
        if (availableAddons.isEmpty()) {
            return@withContext Result.success(emptyList())
        }

        coroutineScope {
            val deferreds = availableAddons.map { addon ->
                async {
                    runCatching {
                        streamApiClient.fetchStreams(addon.transportUrl, stremioType, stremioId)
                    }.fold(
                        onSuccess = { apiResult ->
                            apiResult.fold(
                                onSuccess = { response ->
                                    AddonStreamProviderResult(
                                        addonId = addon.id,
                                        addonName = addon.displayName,
                                        streams = response.streams
                                    )
                                },
                                onFailure = { error ->
                                    AddonStreamProviderResult(
                                        addonId = addon.id,
                                        addonName = addon.displayName,
                                        streams = emptyList(),
                                        error = error
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            AddonStreamProviderResult(
                                addonId = addon.id,
                                addonName = addon.displayName,
                                streams = emptyList(),
                                error = error
                            )
                        }
                    )
                }
            }

            val providerResults = deferreds.awaitAll()
            Result.success(providerResults)
        }
    }
}
