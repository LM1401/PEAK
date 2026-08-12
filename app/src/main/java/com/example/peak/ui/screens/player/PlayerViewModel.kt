package com.example.peak.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing playback state and persisting progress.
 */
class PlayerViewModel(
    private val repository: ContinueWatchingRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    // Authoritative Playback Session State
    data class PlaybackSession(
        val mediaId: String,
        val mediaType: MediaType,
        val videoUrl: String,
        val resumePosition: Long,
        val token: Int
    )

    private val _playbackSession = MutableStateFlow<PlaybackSession?>(null)
    val playbackSession: StateFlow<PlaybackSession?> = _playbackSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // SESSION MANAGEMENT
    private var currentSessionToken = 0

    // SAVE POLICY STATE
    private var lastSavedPosition: Long = -1L
    private var lastSaveTimestamp: Long = 0L
    private val periodicSaveIntervalMs = 5000L

    /**
     * Initializes a deterministic playback session.
     */
    fun loadMedia(id: String, type: MediaType) {
        if (id.isBlank() || (_playbackSession.value?.mediaId == id && _playbackSession.value?.mediaType == type)) return
        
        // Reset state and increment session token
        currentSessionToken++
        _playbackSession.value = null
        _isLoading.value = true
        lastSavedPosition = -1L
        lastSaveTimestamp = 0L

        viewModelScope.launch {
            val tokenAtStart = currentSessionToken
            
            // 1. Fetch metadata (includes videoUrl)
            val movieResult = movieRepository.getMediaById(id, type)
            // 2. Resolve resume position for this exact identity
            val resumePos = repository.getResumePosition(id, type)

            if (tokenAtStart != currentSessionToken) return@launch // Stale load

            movieResult.onSuccess { movieDetails ->
                val url = movieDetails.videoUrl
                if (url != null) {
                    _playbackSession.value = PlaybackSession(id, type, url, resumePos, tokenAtStart)
                    _isLoading.value = false
                } else {
                    _isLoading.value = false
                    // Handle missing URL if needed
                }
            }.onFailure {
                _isLoading.value = false
            }
        }
    }

    /**
     * Periodic progress save logic.
     */
    fun updatePlaybackPosition(positionMs: Long, durationMs: Long) {
        val session = _playbackSession.value ?: return
        if (durationMs <= 0 || positionMs < 0) return

        val currentTime = System.currentTimeMillis()
        val isIntervalPassed = currentTime - lastSaveTimestamp >= periodicSaveIntervalMs
        val isSignificantSeek = kotlin.math.abs(positionMs - lastSavedPosition) > periodicSaveIntervalMs

        if (isIntervalPassed || isSignificantSeek) {
            saveProgressInternal(session, positionMs, durationMs)
        }
    }

    /**
     * Authoritative final save decision.
     */
    fun onPlaybackStopped(positionMs: Long, durationMs: Long) {
        val session = _playbackSession.value ?: return
        if (durationMs <= 0) return

        // 95% Completion check
        val completionRatio = positionMs.toDouble() / durationMs.toDouble()
        if (completionRatio >= 0.95) {
            markPlaybackCompleted(session)
            return
        }

        if (positionMs == lastSavedPosition) return
        saveProgressInternal(session, positionMs, durationMs)
    }

    private fun saveProgressInternal(session: PlaybackSession, positionMs: Long, durationMs: Long) {
        if (session.token != currentSessionToken) return
        
        lastSavedPosition = positionMs
        lastSaveTimestamp = System.currentTimeMillis()

        viewModelScope.launch {
            // Re-verify session validity inside coroutine
            if (session.token != currentSessionToken) return@launch

            val items = repository.continueWatchingItems.value
            val exists = items.any { it.movieId == session.mediaId && it.mediaType == session.mediaType }

            if (exists) {
                repository.updatePosition(
                    movieId = session.mediaId,
                    mediaType = session.mediaType,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            } else {
                // Fetch full data from movie flow to ensure we have images
                movieRepository.getMediaById(session.mediaId, session.mediaType).onSuccess { movie ->
                    repository.saveProgress(
                        movieId = movie.movieId,
                        title = movie.name,
                        posterPath = movie.imageUrl,
                        backdropPath = movie.backdropUrl,
                        mediaType = session.mediaType,
                        positionMs = positionMs,
                        durationMs = durationMs
                    )
                }
            }
        }
    }

    fun markPlaybackCompleted(session: PlaybackSession) {
        if (session.token != currentSessionToken) return
        // Invalidate current session for future saves
        currentSessionToken++
        
        viewModelScope.launch {
            repository.clearProgress(session.mediaId, session.mediaType)
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
