package com.example.peak.ui.screens.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Movies screen.
 * Refactored for extreme recomposition isolation and synchronous focus response.
 */
class MoviesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMovie = _uiState.map { it.selectedMovie }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * SINGLE SOURCE OF TRUTH: The currently focused movie state.
     */
    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var focusDebounceJob: Job? = null

    init {
        fetchMovies()
    }

    fun fetchMovies() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            repository.getTrendingMovies()
                .onSuccess { movies ->
                    if (movies.isNotEmpty()) {
                        val movieRows = listOf(
                            Row("movies_trending", "Trending Movies", movies.shuffled().take(10)),
                            Row("movies_new", "New Movie Releases", movies.shuffled().take(10)),
                            Row("movies_action", "Action & Adventure", movies.shuffled().take(10)),
                            Row("movies_comedy", "Comedy Hits", movies.shuffled().take(10)),
                            Row("movies_scifi", "Sci-Fi & Fantasy", movies.shuffled().take(10)),
                            Row("movies_award", "Award-Winning Films", movies.shuffled().take(10))
                        )

                        val initialMovie = movies.firstOrNull()
                        _uiState.update { 
                            it.copy(
                                rows = movieRows, 
                                loading = false, 
                                selectedMovie = initialMovie
                            ) 
                        }
                        
                        // Set initial focus (Rule 3 Fix)
                        if (_focusState.value == null && initialMovie != null) {
                            if (initialMovie.movieId.isNotBlank()) {
                                _focusState.value = FocusState(initialMovie.movieId, initialMovie)
                            }
                        }
                    } else {
                        _uiState.update { it.copy(loading = false) }
                    }
                }
                .onFailure { exception ->
                    Log.e("PEAK_API", "Failed to fetch movies", exception)
                    _uiState.update { it.copy(loading = false) }
                }
        }
    }

    /**
     * Synchronous focus handler to eliminate propagation latency.
     */
    fun onMovieFocused(movie: Movie) {
        if (_focusState.value?.movieId == movie.movieId) return
        
        // 1. Immediate state update from memory
        _focusState.value = FocusState(movie.movieId, movie)
        
        // 2. Background enrichment of metadata if necessary
        if (movie.description.isBlank()) {
            focusDebounceJob?.cancel()
            focusDebounceJob = viewModelScope.launch {
                repository.getMovieById(movie.movieId).onSuccess { fullMovie ->
                    if (_focusState.value?.movieId == movie.movieId) {
                        _focusState.value = FocusState(movie.movieId, fullMovie)
                    }
                }
            }
        }
    }

    fun onMovieSelected(movie: Movie) {
        _uiState.update { it.copy(selectedMovie = movie) }
    }
}

class MoviesViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoviesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MoviesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
