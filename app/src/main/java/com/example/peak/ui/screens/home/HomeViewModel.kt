package com.example.peak.ui.screens.home

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

import android.util.Log
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * ViewModel for the Home screen.
 * Consolidates multiple data sources into a single reactive UI state pipeline.
 */
class HomeViewModel(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Internal state for API-sourced rows to prevent race conditions with Continue Watching
    private val _apiRows = MutableStateFlow<List<Row>>(emptyList())
    private var focusDebounceJob: Job? = null

    init {
        setupStatePipeline()
        fetchMovies()
    }

    /**
     * Consolidates all data streams into a single source of truth for the UI.
     * Prevents flickering by ensuring 'rows' are calculated in one place.
     */
    private fun setupStatePipeline() {
        combine(
            continueWatchingRepository.continueWatchingItems,
            _apiRows
        ) { cwItems, apiRows ->
            Log.d("CW_DEBUG", "ViewModel received -> cwItems size=${cwItems.size}, apiRows size=${apiRows.size}")
            val progressMap = cwItems.associateBy({ it.movieId }, { it.progress })
            val cwRowTitle = "Continue Watching"
            
            val combinedRows = if (cwItems.isNotEmpty()) {
                val cwRow = Row(cwRowTitle, cwItems.map { it.toMovie() })
                listOf(cwRow) + apiRows
            } else {
                apiRows
            }
            
            Pair(combinedRows, progressMap)
        }.onEach { (rows, progressMap) ->
            _uiState.update { it.copy(
                rows = rows,
                continueWatchingProgress = progressMap
            ) }
        }.launchIn(viewModelScope)
    }

    fun fetchMovies() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            repository.getTrendingMovies()
                .onSuccess { movies ->
                    if (movies.isNotEmpty()) {
                        val movieRows = listOf(
                            Row("Trending This Week", movies.shuffled().take(10)),
                            Row("Top Picks for You", movies.shuffled().take(10)),
                            Row("Action & Adventure", movies.shuffled().take(10)),
                            Row("New Releases", movies.shuffled().take(10)),
                            Row("Documentaries", movies.shuffled().take(10)),
                            Row("Award-Winning Movies", movies.shuffled().take(10))
                        )

                        _apiRows.value = movieRows
                        _uiState.update { it.copy(
                            loading = false,
                            selectedMovie = it.selectedMovie ?: movies.firstOrNull()
                        ) }
                    } else {
                        _uiState.update { it.copy(loading = false) }
                    }
                }
                .onFailure {
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
            delay(180) // SNAPPY: Reduced delay for more responsive cinematic background transitions
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
class HomeViewModelFactory(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, continueWatchingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
