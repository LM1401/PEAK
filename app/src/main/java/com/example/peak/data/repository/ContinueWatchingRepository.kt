package com.example.peak.data.repository

import android.util.Log
import com.example.peak.data.continuewatching.ContinueWatchingStorage
import com.example.peak.domain.model.ContinueWatchingItem
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for managing Continue Watching data.
 * Reactive Single Source of Truth for the application.
 */
class ContinueWatchingRepository(
    private val storage: ContinueWatchingStorage
) {
    private val mutex = Mutex()
    private val _continueWatchingItems = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatchingItems: StateFlow<List<ContinueWatchingItem>> = _continueWatchingItems.asStateFlow()

    init {
        // Initial load on creation
        _continueWatchingItems.value = storage.getAllItems()
    }

    suspend fun refresh() {
        mutex.withLock {
            _continueWatchingItems.value = storage.getAllItems()
        }
    }

    /**
     * Authoritative upsert for progress updates.
     */
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
        mutex.withLock {
            try {
                val currentList = _continueWatchingItems.value.toMutableList()
                
                // Identify exact entry
                currentList.removeAll { it.movieId == movieId && it.mediaType == mediaType }
                
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
                    positionMs = positionMs,
                    lastWatched = System.currentTimeMillis()
                )

                // 95% Completion Guard
                if (!item.isEffectivelyCompleted()) {
                    currentList.add(0, item)
                }
                
                val updatedList = currentList.take(20)
                storage.saveAllItems(updatedList)
                _continueWatchingItems.value = updatedList
            } catch (e: Exception) {
                Log.e("CW_REPO", "saveProgress failed", e)
            }
        }
    }

    /**
     * Specialized update that preserves existing metadata while updating playback state.
     */
    suspend fun updatePosition(
        movieId: String,
        mediaType: MediaType,
        positionMs: Long,
        durationMs: Long,
        season: Int? = null,
        episode: Int? = null
    ) {
        mutex.withLock {
            try {
                val currentList = _continueWatchingItems.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.movieId == movieId && it.mediaType == mediaType }
                
                if (existingIndex == -1) return@withLock // Should have been created by initial saveProgress

                val existingItem = currentList.removeAt(existingIndex)
                val updatedItem = existingItem.copy(
                    positionMs = positionMs,
                    durationMs = if (durationMs > 0) durationMs else existingItem.durationMs,
                    lastWatched = System.currentTimeMillis(),
                    season = season ?: existingItem.season,
                    episode = episode ?: existingItem.episode
                )

                if (!updatedItem.isEffectivelyCompleted()) {
                    currentList.add(0, updatedItem)
                }

                val updatedList = currentList.take(20)
                storage.saveAllItems(updatedList)
                _continueWatchingItems.value = updatedList
            } catch (e: Exception) {
                Log.e("CW_REPO", "updatePosition failed", e)
            }
        }
    }

    suspend fun getResumePosition(movieId: String, mediaType: MediaType): Long {
        return _continueWatchingItems.value.find { 
            it.movieId == movieId && it.mediaType == mediaType 
        }?.positionMs ?: 0L
    }

    suspend fun clearProgress(movieId: String, mediaType: MediaType) {
        mutex.withLock {
            try {
                val currentList = _continueWatchingItems.value.toMutableList()
                if (currentList.removeAll { it.movieId == movieId && it.mediaType == mediaType }) {
                    storage.saveAllItems(currentList)
                    _continueWatchingItems.value = currentList
                }
            } catch (e: Exception) {
                Log.e("CW_REPO", "clearProgress failed", e)
            }
        }
    }

    suspend fun removeCompleted() {
        mutex.withLock {
            try {
                val currentList = _continueWatchingItems.value.toMutableList()
                if (currentList.removeAll { it.isEffectivelyCompleted() }) {
                    storage.saveAllItems(currentList)
                    _continueWatchingItems.value = currentList
                }
            } catch (e: Exception) {
                Log.e("CW_REPO", "removeCompleted failed", e)
            }
        }
    }
}
