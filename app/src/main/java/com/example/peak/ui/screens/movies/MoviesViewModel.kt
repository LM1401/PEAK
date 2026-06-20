package com.example.peak.ui.screens.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Movies screen.
 * Refactored for extreme recomposition isolation.
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

    private val _focusedMovieId = MutableStateFlow<String?>(null)
    val focusedMovieId: StateFlow<String?> = _focusedMovieId.asStateFlow()

    private val _focusedMovieBackdropUrl = MutableStateFlow<String?>(null)
    val focusedMovieBackdropUrl = _focusedMovieBackdropUrl.asStateFlow()

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
                        if (_focusedMovieBackdropUrl.value == null) {
                            _focusedMovieBackdropUrl.value = initialMovie?.backdropUrl
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

    fun onMovieFocused(movie: Movie) {
        if (_focusedMovieId.value == movie.movieId) return
        _focusedMovieId.value = movie.movieId
        _focusedMovieBackdropUrl.value = movie.backdropUrl
        
        // Background preloading of metadata
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            launch { repository.getMovieById(movie.movieId) }
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
