package com.example.peak.ui.screens.home

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
 * ViewModel for the Home screen.
 * It manages the state of the movie rows and the currently focused movie.
 */
class HomeViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var focusDebounceJob: Job? = null

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
                        // Create a variety of rows for a rich home screen
                        val movieRows = mutableListOf<Row>()
                        
                        // Row 1: Trending
                        movieRows.add(Row("Trending This Week", movies.shuffled().take(10)))
                        
                        // Row 2: Top Picks
                        movieRows.add(Row("Top Picks for You", movies.shuffled().take(10)))
                        
                        // Row 3: Action & Adventure
                        movieRows.add(Row("Action & Adventure", movies.shuffled().take(10)))
                        
                        // Row 4: New Releases
                        movieRows.add(Row("New Releases", movies.shuffled().take(10)))
                        
                        // Row 5: Documentaries
                        movieRows.add(Row("Documentaries", movies.shuffled().take(10)))
                        
                        // Row 6: Award-Winning
                        movieRows.add(Row("Award-Winning Movies", movies.shuffled().take(10)))

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

    /**
     * Debounces the selection update to prevent Hero flickering while scrolling.
     * ViewModel updates only occur when focus has settled.
     */
    fun onMovieFocused(movie: Movie) {
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            delay(500) // Wait for focus to settle before updating global state
            _uiState.update { it.copy(selectedMovie = movie) }
        }
    }

    /**
     * Updates the master selection immediately (e.g. on click).
     */
    fun onMovieSelected(movie: Movie) {
        focusDebounceJob?.cancel()
        _uiState.update { it.copy(selectedMovie = movie) }
    }
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
