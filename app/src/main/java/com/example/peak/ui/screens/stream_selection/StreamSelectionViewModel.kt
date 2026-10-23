package com.example.peak.ui.screens.stream_selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.repository.AddonRepository
import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing stream discovery, IMDb resolution, provider error isolation,
 * and stream selection state without initiating playback.
 */
class StreamSelectionViewModel(
    private val movieRepository: MovieRepository,
    private val addonRepository: AddonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamSelectionUiState())
    val uiState: StateFlow<StreamSelectionUiState> = _uiState.asStateFlow()

    private var currentMediaId: String? = null
    private var currentMediaType: MediaType? = null
    private var currentSeason: Int? = null
    private var currentEpisode: Int? = null

    private var fetchJob: Job? = null
    private var requestToken: Long = 0L

    fun loadStreams(
        mediaId: String,
        mediaType: MediaType,
        season: Int? = null,
        episode: Int? = null,
        forceReload: Boolean = false
    ) {
        val cleanMediaId = mediaId.trim()
        if (cleanMediaId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid media ID"
                )
            }
            return
        }

        // Avoid duplicate re-loads unless forced or inputs changed
        if (!forceReload &&
            currentMediaId == cleanMediaId &&
            currentMediaType == mediaType &&
            currentSeason == season &&
            currentEpisode == episode &&
            _uiState.value.movie != null &&
            !_uiState.value.isLoading
        ) {
            return
        }

        currentMediaId = cleanMediaId
        currentMediaType = mediaType
        currentSeason = season
        currentEpisode = episode

        val thisToken = ++requestToken
        fetchJob?.cancel()

        _uiState.update {
            StreamSelectionUiState(
                isLoading = true,
                movie = null,
                imdbId = null,
                providerResults = emptyList(),
                selectedStream = null,
                errorMessage = null,
                noAddonsConfigured = false,
                isTvWithoutEpisode = false
            )
        }

        // Check 1: TV Series requires explicit valid season and episode
        if (mediaType == MediaType.TV && (season == null || episode == null || season <= 0 || episode <= 0)) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isTvWithoutEpisode = true
                )
            }
            return
        }

        // Check 2: Require enabled add-ons with stream capabilities
        val installedAddons = addonRepository.addons.value
        val hasStreamAddons = installedAddons.any { addon ->
            addon.isEnabled && addon.manifest?.resources?.any { resource ->
                resource.name.equals("stream", ignoreCase = true)
            } == true
        }

        if (!hasStreamAddons) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    noAddonsConfigured = true
                )
            }
            return
        }

        fetchJob = viewModelScope.launch {
            // Fetch movie details
            val movieResult = movieRepository.getMediaById(cleanMediaId, mediaType)
            if (thisToken != requestToken) return@launch

            val movieObj = movieResult.getOrNull()
            _uiState.update { it.copy(movie = movieObj) }

            // Resolve IMDb ID
            val imdbResult = movieRepository.getImdbId(cleanMediaId, mediaType)
            if (thisToken != requestToken) return@launch

            imdbResult.fold(
                onSuccess = { resolvedImdbId ->
                    _uiState.update { it.copy(imdbId = resolvedImdbId) }

                    val streamsResult = addonRepository.getStreams(
                        mediaType = mediaType,
                        imdbId = resolvedImdbId,
                        season = season,
                        episode = episode
                    )
                    if (thisToken != requestToken) return@launch

                    streamsResult.fold(
                        onSuccess = { providerResults ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    providerResults = providerResults
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Failed to retrieve streams"
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Unable to resolve IMDb ID for stream lookup: ${error.message ?: "ID missing"}"
                        )
                    }
                }
            )
        }
    }

    fun selectStream(stream: AddonStream) {
        _uiState.update { it.copy(selectedStream = stream) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedStream = null) }
    }

    fun retry(mediaId: String, mediaType: MediaType, season: Int? = null, episode: Int? = null) {
        loadStreams(mediaId, mediaType, season, episode, forceReload = true)
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }
}

class StreamSelectionViewModelFactory(
    private val movieRepository: MovieRepository,
    private val addonRepository: AddonRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StreamSelectionViewModel::class.java)) {
            return StreamSelectionViewModel(movieRepository, addonRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
