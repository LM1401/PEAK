package com.example.peak.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ViewModel for the Home screen.
 * Consolidates multiple data sources into a single reactive UI state pipeline.
 * Refactored for extreme recomposition isolation.
 */
class HomeViewModel(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Granular flows to avoid full-screen recomposition
    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // FIX 3: selectedMovie updates ONLY on click/navigation (background/Hero source)
    val selectedMovie = _uiState.map { it.selectedMovie }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // FIX 1: Stable Snapshot Flow for Focus (Zero Global Ripple)
    private val _focusedMovieId = MutableStateFlow<String?>(null)
    val focusedMovieId = _focusedMovieId
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    // Flow specifically for the browsing background (follows focus)
    private val _focusedMovieBackdropUrl = MutableStateFlow<String?>(null)
    val focusedMovieBackdropUrl = _focusedMovieBackdropUrl.asStateFlow()

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val continueWatchingProgress = _uiState.map { it.continueWatchingProgress }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Internal state for API-sourced rows
    private val _apiRows = MutableStateFlow<List<Row>>(emptyList())
    private var focusDebounceJob: Job? = null

    init {
        setupStatePipeline()
        fetchMovies()
    }

    private fun setupStatePipeline() {
        combine(
            continueWatchingRepository.continueWatchingItems,
            _apiRows
        ) { cwItems, apiRows ->
            val progressMap = cwItems.associateBy({ it.movieId }, { it.progress })
            val combinedRows = if (cwItems.isNotEmpty()) {
                val cwRow = Row("continue_watching", "Continue Watching", cwItems.map { it.toMovie() })
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
                            Row("trending", "Trending This Week", movies.shuffled().take(10)),
                            Row("top_picks", "Top Picks for You", movies.shuffled().take(10)),
                            Row("action", "Action & Adventure", movies.shuffled().take(10))
                        )
                        _apiRows.value = movieRows
                        val initialMovie = movies.firstOrNull()
                        _uiState.update { it.copy(
                            loading = false,
                            selectedMovie = it.selectedMovie ?: initialMovie
                        ) }
                        // Set initial background
                        if (_focusedMovieBackdropUrl.value == null) {
                            _focusedMovieBackdropUrl.value = initialMovie?.backdropUrl
                        }
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
     * FIX 1: Update only the decoupled focusedMovieId.
     * HARD GUARD: Prevent redundant emissions from D-pad spam.
     */
    fun onMovieFocused(movie: Movie) {
        if (_focusedMovieId.value == movie.movieId) return
        _focusedMovieId.value = movie.movieId
        _focusedMovieBackdropUrl.value = movie.backdropUrl
        
        // Background preloading of metadata remains
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            launch { repository.getMovieById(movie.movieId) }
        }
    }

    /**
     * Updates the master selection (e.g. on click).
     * Triggers background/Hero changes.
     */
    fun onMovieSelected(movie: Movie) {
        _uiState.update { it.copy(selectedMovie = movie) }
    }
}

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
