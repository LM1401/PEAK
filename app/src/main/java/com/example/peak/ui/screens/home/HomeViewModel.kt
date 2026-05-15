package com.example.peak.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * It manages the state of the movie rows and the currently focused movie.
 */
class HomeViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchMovies()
    }

    fun fetchMovies() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            repository.getTrendingMovies()
                .onSuccess { movies ->
                    Log.d("PEAK_API", "Fetched ${movies.size} movies")
                    if (movies.isNotEmpty()) {
                        val movieRows = if (movies.size >= 10) {
                            listOf(
                                Row("Trending This Week", movies.subList(0, 10)),
                                Row("Recommended for You", movies.subList(10, movies.size.coerceAtMost(20)))
                            )
                        } else {
                            listOf(Row("Trending", movies))
                        }
                        _uiState.update { 
                            it.copy(rows = movieRows, loading = false, selectedMovie = movies.first()) 
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

    fun onMovieSelected(movie: Movie) {
        _uiState.update {
            it.copy(selectedMovie = movie)
        }
    }

    // Keep for UI compatibility if needed, but should transition to onMovieSelected
    fun onMovieFocused(movie: Movie) = onMovieSelected(movie)
}

/**
 * Simple factory to create HomeViewModel with its repository dependency.
 */
class HomeViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
