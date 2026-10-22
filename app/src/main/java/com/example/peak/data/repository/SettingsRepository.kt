package com.example.peak.data.repository

import android.content.Context
import android.util.Log
import com.example.peak.data.settings.SettingsStorage
import com.example.peak.ui.image.PeakImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thread-safe repository managing application preferences and settings persistence.
 * Acts as the authoritative single source of truth for user settings.
 */
class SettingsRepository(
    private val storage: SettingsStorage
) {
    private val mutex = Mutex()

    private val _autoPlayEnabled = MutableStateFlow(storage.getAutoPlayEnabled())
    val autoPlayEnabled: StateFlow<Boolean> = _autoPlayEnabled.asStateFlow()

    private val _debugModeEnabled = MutableStateFlow(storage.getDebugModeEnabled())
    val debugModeEnabled: StateFlow<Boolean> = _debugModeEnabled.asStateFlow()

    suspend fun setAutoPlayEnabled(enabled: Boolean) = mutex.withLock {
        if (_autoPlayEnabled.value == enabled) return@withLock
        _autoPlayEnabled.value = enabled
        withContext(Dispatchers.IO) {
            storage.setAutoPlayEnabled(enabled)
        }
    }

    suspend fun setDebugModeEnabled(enabled: Boolean) = mutex.withLock {
        if (_debugModeEnabled.value == enabled) return@withLock
        _debugModeEnabled.value = enabled
        withContext(Dispatchers.IO) {
            storage.setDebugModeEnabled(enabled)
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun clearAppCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val imageLoader = PeakImageLoader.getInstance(context)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            Log.e("SETTINGS_REPO", "Failed to clear application cache", e)
        }
    }
}
