package com.example.peak.ui.screens.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Movies screen.
 * Mirrors HomeViewModel pattern.
 */
class MoviesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

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
                        val movieRows = mutableListOf<Row>()
                        
                        movieRows.add(Row("Trending Movies", movies.shuffled().take(10)))
                        movieRows.add(Row("New Movie Releases", movies.shuffled().take(10)))
                        movieRows.add(Row("Action & Adventure", movies.shuffled().take(10)))
                        movieRows.add(Row("Comedy Hits", movies.shuffled().take(10)))
                        movieRows.add(Row("Sci-Fi & Fantasy", movies.shuffled().take(10)))
                        movieRows.add(Row("Award-Winning Films", movies.shuffled().take(10)))

                        _uiState.update { 
                            it.copy(
                                rows = movieRows, 
                                loading = false, 
                                selectedMovie = movies.first()
                            ) 
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
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            delay(180) 
            _uiState.update { it.copy(selectedMovie = movie) }
        }
    }

    fun onMovieSelected(movie: Movie) {
        focusDebounceJob?.cancel()
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
