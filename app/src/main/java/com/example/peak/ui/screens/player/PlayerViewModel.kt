package com.example.peak.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing playback state and persisting progress.
 * Acts as the bridge between the Player UI and the Continue Watching system.
 * Final Stability Pass: Reactive derived state for resume position.
 */
class PlayerViewModel(
    private val repository: ContinueWatchingRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _movieId = MutableStateFlow<String?>(null)

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _videoUrl = MutableStateFlow<String?>(null)
    val videoUrl: StateFlow<String?> = _videoUrl.asStateFlow()

    // ISSUE 2 — MAKE RESUME POSITION FULLY REACTIVE
    @OptIn(ExperimentalCoroutinesApi::class)
    val resumePosition: StateFlow<Long> = _movieId
        .filterNotNull()
        .flatMapLatest { id ->
            repository.continueWatchingItems.map { items ->
                items.find { it.movieId == id }?.positionMs ?: 0L
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Lifecycle safety: tracking last save state to prevent duplicate writes
    private var lastSavedPosition: Long = -1L
    private var lastSaveTimestamp: Long = 0L
    private val periodicSaveIntervalMs = 5000L

    /**
     * Initializes the playback session for a specific movie.
     * Derived states (resumePosition) update automatically via flatMapLatest.
     */
    fun loadMovie(movieId: String) {
        if (movieId.isBlank() || _movieId.value == movieId) return
        _movieId.value = movieId
        _isLoading.value = true
        
        viewModelScope.launch {
            // Load full Movie object from repository (Option A)
            movieRepository.getMovieById(movieId)
                .onSuccess { movieDetails ->
                    _movie.value = movieDetails
                    _videoUrl.value = movieDetails.videoUrl
                    _isLoading.value = false
                }
                .onFailure {
                    _isLoading.value = false
                    _videoUrl.value = null
                }
        }
    }

    /**
     * Updates the playback progress in the repository.
     */
    fun updatePlaybackPosition(positionMs: Long, durationMs: Long) {
        val movieId = _movieId.value ?: return
        if (durationMs <= 0) return

        val currentTime = System.currentTimeMillis()
        val isIntervalPassed = currentTime - lastSaveTimestamp >= periodicSaveIntervalMs
        val isSignificantSeek = kotlin.math.abs(positionMs - lastSavedPosition) > periodicSaveIntervalMs

        if (isIntervalPassed || isSignificantSeek) {
            saveProgressInternal(movieId, positionMs, durationMs)
        }
    }

    /**
     * Final sync method called when playback is stopped.
     */
    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val movieId = _movieId.value ?: return
        if (durationMs <= 0) return

        // Apply completion rule (95%)
        val completionRatio = positionMs.toDouble() / durationMs.toDouble()
        if (completionRatio >= 0.95) {
            markPlaybackCompleted()
            return
        }

        if (positionMs == lastSavedPosition) return

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
                val movieData = _movie.value
                if (movieData != null) {
                    repository.saveProgress(
                        movieId = movieData.movieId,
                        title = movieData.name,
                        posterPath = movieData.imageUrl,
                        backdropPath = movieData.backdropUrl,
                        mediaType = "movie",
                        positionMs = positionMs,
                        durationMs = durationMs
                    )
                }
            }
        }
    }

    fun markPlaybackCompleted() {
        val movieId = _movieId.value ?: return
        viewModelScope.launch {
            val currentDuration = _duration.value
            if (currentDuration > 0) {
                repository.updatePosition(movieId, currentDuration, currentDuration)
            } else {
                repository.clearProgress(movieId)
            }
        }
    }

    fun clearProgress() {
        val movieId = _movieId.value ?: return
        viewModelScope.launch {
            repository.clearProgress(movieId)
        }
    }
}

class PlayerViewModelFactory(
    private val repository: ContinueWatchingRepository,
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(repository, movieRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
