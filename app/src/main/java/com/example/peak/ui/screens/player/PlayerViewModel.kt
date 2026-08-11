package com.example.peak.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.MediaType
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

    private val _mediaId = MutableStateFlow<String?>(null)
    private val _mediaType = MutableStateFlow<MediaType?>(null)

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _videoUrl = MutableStateFlow<String?>(null)
    val videoUrl: StateFlow<String?> = _videoUrl.asStateFlow()

    // ISSUE 2 — MAKE RESUME POSITION FULLY REACTIVE
    @OptIn(ExperimentalCoroutinesApi::class)
    val resumePosition: StateFlow<Long> = combine(_mediaId.filterNotNull(), _mediaType.filterNotNull()) { id, type ->
        Pair(id, type)
    }.flatMapLatest { (id, type) ->
        repository.continueWatchingItems.map { items ->
            items.find { it.movieId == id && it.mediaType == type }?.positionMs ?: 0L
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Lifecycle safety: tracking last save state to prevent duplicate writes
    private var lastSavedPosition: Long = -1L
    private var lastSaveTimestamp: Long = 0L
    private val periodicSaveIntervalMs = 5000L

    /**
     * Initializes the playback session for a specific media.
     */
    fun loadMedia(id: String, type: MediaType) {
        if (id.isBlank() || (_mediaId.value == id && _mediaType.value == type)) return
        _mediaId.value = id
        _mediaType.value = type
        _isLoading.value = true
        
        viewModelScope.launch {
            // Load full Media object from repository
            movieRepository.getMediaById(id, type)
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
        val id = _mediaId.value ?: return
        val type = _mediaType.value ?: return
        if (durationMs <= 0) return

        val currentTime = System.currentTimeMillis()
        val isIntervalPassed = currentTime - lastSaveTimestamp >= periodicSaveIntervalMs
        val isSignificantSeek = kotlin.math.abs(positionMs - lastSavedPosition) > periodicSaveIntervalMs

        if (isIntervalPassed || isSignificantSeek) {
            saveProgressInternal(id, type, positionMs, durationMs)
        }
    }

    /**
     * Final sync method called when playback is stopped.
     */
    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val id = _mediaId.value ?: return
        val type = _mediaType.value ?: return
        if (durationMs <= 0) return

        // Apply completion rule (95%)
        val completionRatio = positionMs.toDouble() / durationMs.toDouble()
        if (completionRatio >= 0.95) {
            markPlaybackCompleted()
            return
        }

        if (positionMs == lastSavedPosition) return

        saveProgressInternal(id, type, positionMs, durationMs)
    }

    private fun saveProgressInternal(id: String, type: MediaType, positionMs: Long, durationMs: Long) {
        lastSavedPosition = positionMs
        lastSaveTimestamp = System.currentTimeMillis()

        viewModelScope.launch {
            val items = repository.continueWatchingItems.value
            val exists = items.any { it.movieId == id && it.mediaType == type }
            
            if (exists) {
                repository.updatePosition(id, type, positionMs, durationMs)
            } else {
                val movieData = _movie.value
                if (movieData != null) {
                    repository.saveProgress(
                        movieId = movieData.movieId,
                        title = movieData.name,
                        posterPath = movieData.imageUrl,
                        backdropPath = movieData.backdropUrl,
                        mediaType = type,
                        positionMs = positionMs,
                        durationMs = durationMs
                    )
                }
            }
        }
    }

    fun markPlaybackCompleted() {
        val id = _mediaId.value ?: return
        val type = _mediaType.value ?: return
        viewModelScope.launch {
            val currentDuration = _duration.value
            if (currentDuration > 0) {
                repository.updatePosition(id, type, currentDuration, currentDuration)
            } else {
                repository.clearProgress(id, type)
            }
        }
    }

    fun clearProgress() {
        val id = _mediaId.value ?: return
        val type = _mediaType.value ?: return
        viewModelScope.launch {
            repository.clearProgress(id, type)
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
