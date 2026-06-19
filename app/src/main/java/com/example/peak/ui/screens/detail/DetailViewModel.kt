package com.example.peak.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DetailViewModel(
    private val movieRepository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository
) : ViewModel() {
    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _similarMovies = MutableStateFlow<List<Movie>>(emptyList())
    val similarMovies: StateFlow<List<Movie>> = _similarMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _resumePosition = MutableStateFlow<Long>(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _totalDuration = MutableStateFlow<Long>(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private var currentMovieId: String? = null

    init {
        observeContinueWatching()
    }

    private fun observeContinueWatching() {
        continueWatchingRepository.continueWatchingItems.onEach { items ->
            val id = currentMovieId ?: return@onEach
            val item = items.find { it.movieId == id }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L
        }.launchIn(viewModelScope)
    }

    fun loadMovie(movieId: String) {
        if (movieId.isBlank() || currentMovieId == movieId) return
        
        this.currentMovieId = movieId
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            // 1. Initial check for resume position
            val item = continueWatchingRepository.continueWatchingItems.value.find { it.movieId == movieId }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L

            // 2. Fetch full movie details from repository (Option A)
            movieRepository.getMovieById(movieId)
                .onSuccess { movieDetails ->
                    _movie.value = movieDetails
                    _isLoading.value = false
                    
                    // Fetch "similar" movies (using trending as fallback for now)
                    movieRepository.getTrendingMovies().onSuccess { trending ->
                        _similarMovies.value = trending.filter { it.movieId != movieId }.shuffled().take(12)
                    }
                }
                .onFailure {
                    _isLoading.value = false
                    _error.value = "Failed to load movie details"
                }
        }
    }
}

class DetailViewModelFactory(
    private val movieRepository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(movieRepository, continueWatchingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
