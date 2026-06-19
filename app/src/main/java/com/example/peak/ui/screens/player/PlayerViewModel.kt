package com.example.peak.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.ui.navigation.MovieSelectionTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing playback state and persisting progress.
 * Acts as the bridge between the Player UI and the Continue Watching system.
 */
class PlayerViewModel(
    private val repository: ContinueWatchingRepository
) : ViewModel() {

    private val _movieId = MutableStateFlow<String?>(null)

    private val _resumePosition = MutableStateFlow(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isResumeAvailable = MutableStateFlow(false)
    val isResumeAvailable: StateFlow<Boolean> = _isResumeAvailable.asStateFlow()

    // Lifecycle safety: tracking last save state to prevent duplicate writes
    private var lastSavedPosition: Long = -1L
    private var lastSaveTimestamp: Long = 0L
    private val periodicSaveIntervalMs = 5000L // Periodic updates happen every 5s

    init {
        // Reactive sync with the Single Source of Truth
        repository.continueWatchingItems
            .onEach { items ->
                val id = _movieId.value ?: return@onEach
                val item = items.find { it.movieId == id }
                if (item != null) {
                    _resumePosition.value = item.positionMs
                    _duration.value = item.durationMs
                    // Item is resumable if there's progress and it's not yet finished (95% rule in model)
                    _isResumeAvailable.value = item.positionMs > 0 && !item.isEffectivelyCompleted()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Initializes the playback session for a specific movie.
     */
    fun loadMovie(movieId: String) {
        if (movieId.isBlank()) return
        _movieId.value = movieId
        
        // Snapshot current state for immediate UI feedback
        val item = repository.continueWatchingItems.value.find { it.movieId == movieId }
        if (item != null) {
            _resumePosition.value = item.positionMs
            _duration.value = item.durationMs
            _isResumeAvailable.value = item.positionMs > 0 && !item.isEffectivelyCompleted()
        }
    }

    /**
     * Updates the playback progress in the repository.
     * Includes a time-based debounce to prevent excessive writes during playback.
     */
    fun updatePlaybackPosition(positionMs: Long, durationMs: Long) {
        val movieId = _movieId.value ?: return
        if (durationMs <= 0) return

        val currentTime = System.currentTimeMillis()
        
        // Periodic Save Debounce: Only save if interval passed OR significant seek happened
        val isIntervalPassed = currentTime - lastSaveTimestamp >= periodicSaveIntervalMs
        val isSignificantSeek = kotlin.math.abs(positionMs - lastSavedPosition) > periodicSaveIntervalMs

        if (isIntervalPassed || isSignificantSeek) {
            saveProgressInternal(movieId, positionMs, durationMs)
        }
    }

    /**
     * Final sync method called when playback is stopped (exit, background, dispose).
     * Guaranteed to save the final state if it differs from the last periodic save.
     */
    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val movieId = _movieId.value ?: return
        if (durationMs <= 0) return

        // 1. Apply completion rule (95%)
        val completionRatio = positionMs.toDouble() / durationMs.toDouble()
        if (completionRatio >= 0.95) {
            markPlaybackCompleted()
            return
        }

        // 2. Prevent duplicate write if we JUST saved this exact position
        if (positionMs == lastSavedPosition) return

        // 3. Force final save (ignoring interval)
        saveProgressInternal(movieId, positionMs, durationMs)
    }

    private fun saveProgressInternal(movieId: String, positionMs: Long, durationMs: Long) {
        lastSavedPosition = positionMs
        lastSaveTimestamp = System.currentTimeMillis()

        viewModelScope.launch {
            val items = repository.continueWatchingItems.value
            val exists = items.any { it.movieId == movieId }
            
            if (exists) {
                repository.updatePosition(movieId, positionMs, durationMs)
            } else {
                // Initialize the item in Continue Watching using the cached movie details
                val selectedMovie = MovieSelectionTracker.selectedMovie.value
                if (selectedMovie != null && selectedMovie.movieId == movieId) {
                    repository.saveProgress(
                        movieId = selectedMovie.movieId,
                        title = selectedMovie.name,
                        posterPath = selectedMovie.imageUrl,
                        backdropPath = selectedMovie.backdropUrl,
                        mediaType = "movie",
                        positionMs = positionMs,
                        durationMs = durationMs
                    )
                }
            }
        }
    }

    /**
     * Marks the current media as finished, which triggers removal from Continue Watching rows.
     */
    fun markPlaybackCompleted() {
        val movieId = _movieId.value ?: return
        viewModelScope.launch {
            // Setting position to duration triggers the 95% completion rule in the storage layer
            val currentDuration = _duration.value
            if (currentDuration > 0) {
                repository.updatePosition(movieId, currentDuration, currentDuration)
            } else {
                repository.clearProgress(movieId)
            }
        }
    }

    /**
     * Forcibly removes the movie from the Continue Watching list.
     */
    fun clearProgress() {
        val movieId = _movieId.value ?: return
        viewModelScope.launch {
            repository.clearProgress(movieId)
        }
    }
}

/**
 * Factory for creating PlayerViewModel with its repository dependency.
 */
class PlayerViewModelFactory(
    private val repository: ContinueWatchingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
