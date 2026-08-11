package com.example.peak.data.repository

import android.util.Log
import com.example.peak.data.continuewatching.ContinueWatchingStorage
import com.example.peak.domain.model.ContinueWatchingItem
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing Continue Watching data.
 * Reactive Single Source of Truth for the application.
 */
class ContinueWatchingRepository(
    private val storage: ContinueWatchingStorage
) {
    private val _continueWatchingItems = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatchingItems: StateFlow<List<ContinueWatchingItem>> = _continueWatchingItems.asStateFlow()

    init {
        // Initial load from storage to ensure UI has data immediately
        loadFromStorage()
    }

    private fun loadFromStorage() {
        try {
            val items = storage.getAllItems()
            _continueWatchingItems.value = items
            Log.d("CW_DEBUG", "Repository loadFromStorage -> size=${items.size}")
        } catch (e: Exception) {
            Log.e("CW_DEBUG", "Repository loadFromStorage failed", e)
            _continueWatchingItems.value = emptyList()
        }
    }

    suspend fun refresh() {
        loadFromStorage()
    }

    suspend fun saveProgress(
        movieId: String,
        title: String,
        posterPath: String?,
        backdropPath: String?,
        mediaType: MediaType,
        positionMs: Long,
        durationMs: Long,
        tmdbId: String? = null,
        season: Int? = null,
        episode: Int? = null
    ) {
        try {
            val item = ContinueWatchingItem(
                movieId = movieId,
                tmdbId = tmdbId,
                title = title,
                posterPath = posterPath,
                backdropPath = backdropPath,
                mediaType = mediaType,
                season = season,
                episode = episode,
                durationMs = durationMs,
                positionMs = positionMs
            )
            _continueWatchingItems.value = storage.saveItem(item)
        } catch (e: Exception) {
            // Re-emit existing state to trigger UI if needed, but don't crash
            _continueWatchingItems.value = _continueWatchingItems.value
        }
    }

    suspend fun updatePosition(movieId: String, mediaType: MediaType, positionMs: Long, durationMs: Long) {
        try {
            val existingItem = storage.getItem(movieId, mediaType) ?: return
            val updatedItem = existingItem.copy(
                positionMs = positionMs,
                durationMs = if (durationMs > 0) durationMs else existingItem.durationMs,
                lastWatched = System.currentTimeMillis()
            )
            _continueWatchingItems.value = storage.saveItem(updatedItem)
        } catch (e: Exception) {
            _continueWatchingItems.value = _continueWatchingItems.value
        }
    }

    suspend fun getResumePosition(movieId: String, mediaType: MediaType): Long {
        return storage.getItem(movieId, mediaType)?.positionMs ?: 0L
    }

    suspend fun clearProgress(movieId: String, mediaType: MediaType) {
        try {
            _continueWatchingItems.value = storage.deleteItem(movieId, mediaType)
        } catch (e: Exception) {
            _continueWatchingItems.value = _continueWatchingItems.value
        }
    }

    suspend fun removeCompleted() {
        try {
            val items = storage.getAllItems()
            var currentItems = items
            items.forEach {
                if (it.isEffectivelyCompleted()) {
                    currentItems = storage.deleteItem(it.movieId, it.mediaType)
                }
            }
            _continueWatchingItems.value = currentItems
        } catch (e: Exception) {
            _continueWatchingItems.value = _continueWatchingItems.value
        }
    }
}
