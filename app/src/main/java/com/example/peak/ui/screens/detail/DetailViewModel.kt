package com.example.peak.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DetailViewModel(
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository? = null
) : ViewModel() {
    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _similarMovies = MutableStateFlow<List<Movie>>(emptyList())
    val similarMovies: StateFlow<List<Movie>> = _similarMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resumePosition = MutableStateFlow<Long>(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _totalDuration = MutableStateFlow<Long>(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private var currentMovieId: String? = null

    init {
        observeContinueWatching()
        observeSelectedMovie()
    }

    private fun observeSelectedMovie() {
        com.example.peak.ui.navigation.MovieSelectionTracker.selectedMovie
            .onEach { selected ->
                if (selected != null && selected.movieId == currentMovieId) {
                    _movie.value = selected
                    _isLoading.value = false
                }
            }.launchIn(viewModelScope)
    }

    private fun observeContinueWatching() {
        continueWatchingRepository?.continueWatchingItems?.onEach { items ->
            val id = currentMovieId ?: return@onEach
            val item = items.find { it.movieId == id }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L
        }?.launchIn(viewModelScope)
    }

    fun loadMovie(movieId: String) {
        if (movieId.isBlank()) {
            _isLoading.value = false
            return
        }
        this.currentMovieId = movieId
        
        // Trigger initial check for resume position
        continueWatchingRepository?.let { repo ->
            val item = repo.continueWatchingItems.value.find { it.movieId == movieId }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L
        }

        // Check if the movie is already selected in the tracker
        val selected = com.example.peak.ui.navigation.MovieSelectionTracker.selectedMovie.value
        if (selected != null && selected.movieId == movieId) {
            _movie.value = selected
            _isLoading.value = false
        } else {
            // If not found in tracker, we might still want to show something or wait for observation
            // But the user said "Remove all API lookup logic"
            _isLoading.value = true
        }
    }
}

class DetailViewModelFactory(
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(continueWatchingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
