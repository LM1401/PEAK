package com.example.peak.ui.screens.random

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.DiscoveryMode
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.RandomSelectionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RandomViewModel(
    private val randomRepository: RandomSelectionRepository,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RandomUiState())
    val uiState: StateFlow<RandomUiState> = _uiState.asStateFlow()

    private var selectionJob: Job? = null
    private var replaceJob: Job? = null

    fun setMode(mode: DiscoveryMode) {
        _uiState.update { it.copy(request = it.request.copy(mode = mode)) }
    }

    fun setMediaTypes(movies: Boolean, tvShows: Boolean) {
        _uiState.update { 
            it.copy(request = it.request.copy(
                includeMovies = movies,
                includeTvShows = tvShows,
                moviesRequested = if (movies) it.request.moviesRequested else 0,
                tvRequested = if (tvShows) it.request.tvRequested else 0
            ))
        }
    }

    fun setCustomMix(movies: Int, tvShows: Int) {
        _uiState.update {
            it.copy(request = it.request.copy(moviesRequested = movies, tvRequested = tvShows))
        }
    }

    fun generateSelection(skipReveal: Boolean = false) {
        if (_uiState.value.status == RandomStatus.LOADING || _uiState.value.status == RandomStatus.REVEALING) return
        
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            _uiState.update { it.copy(
                status = RandomStatus.LOADING, 
                errorMessage = null,
                partialResultWarning = null,
                results = emptyList()
            ) }

            val result = randomRepository.fetchRandomSelection(_uiState.value.request)
            
            if (result.isSuccess) {
                var movies = result.getOrNull() ?: emptyList()
                
                // Enrich them
                val enrichedMovies = movies.map { m ->
                    val enriched = movieRepository.getMediaById(m.movieId, m.mediaType).getOrNull()
                    enriched ?: m
                }

                if (!skipReveal) {
                    _uiState.update { it.copy(status = RandomStatus.REVEALING) }
                    delay(1500) // Simulate reveal animation time
                }

                val requestedTotal = _uiState.value.request.totalRequested
                val warning = if (enrichedMovies.size < requestedTotal) {
                    "Only ${enrichedMovies.size} titles matched your filters."
                } else null

                _uiState.update { 
                    it.copy(
                        status = RandomStatus.RESULTS,
                        results = enrichedMovies,
                        partialResultWarning = warning
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        status = RandomStatus.ERROR,
                        errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    fun replaceResult(index: Int) {
        if (index !in _uiState.value.results.indices) return
        if (replaceJob?.isActive == true) return
        
        val itemToReplace = _uiState.value.results[index]
        val currentExclusions = _uiState.value.request.sessionExclusions.toMutableSet()
        currentExclusions.add("${itemToReplace.mediaType.name}_${itemToReplace.movieId}")

        _uiState.update { it.copy(
            request = it.request.copy(sessionExclusions = currentExclusions)
        ) }

        replaceJob = viewModelScope.launch {
            val result = randomRepository.fetchSingleReplacement(
                _uiState.value.request, 
                itemToReplace.mediaType,
                _uiState.value.results
            )

            if (result.isSuccess) {
                val newMedia = result.getOrNull()
                if (newMedia != null) {
                    val enriched = movieRepository.getMediaById(newMedia.movieId, newMedia.mediaType).getOrNull() ?: newMedia
                    val updatedList = _uiState.value.results.toMutableList()
                    updatedList[index] = enriched
                    _uiState.update { it.copy(results = updatedList) }
                }
            }
        }
    }

    fun resetToIdle() {
        _uiState.update { it.copy(status = RandomStatus.IDLE, results = emptyList(), errorMessage = null, partialResultWarning = null) }
    }
}

class RandomViewModelFactory(
    private val randomRepository: RandomSelectionRepository,
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RandomViewModel(randomRepository, movieRepository) as T
    }
}
