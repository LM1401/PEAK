package com.example.peak.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.ui.image.ImageWarmingManager
import com.example.peak.ui.image.PeakImageLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Consolidates multiple data sources into a single reactive UI state pipeline.
 * Synchronous focus model for frame-perfect UI response.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * SINGLE SOURCE OF TRUTH: The currently focused movie state.
     * Updated synchronously whenever possible to eliminate Flow propagation lag.
     */
    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    // Granular flows derived from UI state to ensure absolute synchronization
    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            // PHASE 2 — REMOVE TEST/INVALID CONTENT (Rule 1 & 2)
            // Filter invalid items: must have ID, title, poster, and meaningful progress (not finished).
            val validCwItems = cwItems.filter { item ->
                item.movieId.isNotBlank() &&
                item.title.isNotBlank() &&
                item.progress > 0.01f &&
                item.progress < 0.95f &&
                !item.posterPath.isNullOrBlank() &&
                !item.movieId.startsWith("test_")
            }

            val progressMap = validCwItems.associateBy({ it.movieId }, { it.progress })
            
            val combinedRows = buildList {
                if (validCwItems.isNotEmpty()) {
                    add(Row("continue_watching", "Continue Watching", validCwItems.map { it.toMovie() }))
                }
                if (apiRows.isNotEmpty()) {
                    addAll(apiRows)
                } else {
                    add(Row("trending", "Trending", emptyList(), isPlaceholder = true))
                    add(Row("popular", "Popular", emptyList(), isPlaceholder = true))
                }
            }

            Pair(combinedRows, progressMap)
        }.onEach { (rows, progressMap) ->
            // 1. UPDATE UI STATE FIRST
            _uiState.update { it.copy(
                rows = rows,
                continueWatchingProgress = progressMap,
                loading = false
            ) }

            // 2. AUTOMATIC FOCUS SYNC (Rule 3 & 4 Fix)
            // Established ONLY after rows are in the UI State and verified as real data.
            if (_focusState.value == null) {
                rows.firstOrNull { !it.isPlaceholder }?.movies?.firstOrNull()?.let { movie ->
                    if (movie.movieId.isNotBlank()) {
                        _focusState.value = FocusState(movie.movieId, movie)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun fetchMovies() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val result = repository.getTrendingMovies()
            val movies = result.getOrDefault(emptyList())
            
            if (movies.isNotEmpty()) {
                val movieRows = listOf(
                    Row("trending", "Trending This Week", movies.shuffled().take(10)),
                    Row("top_picks", "Top Picks for You", movies.shuffled().take(10)),
                    Row("action", "Action & Adventure", movies.shuffled().take(10))
                )
                _apiRows.value = movieRows
            } else {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    /**
     * Handles movie focus events from the UI.
     * Performs synchronous lookup to eliminate "first frame delay".
     */
    fun onMovieFocused(movieId: String) {
        if (_focusState.value?.movieId == movieId) return

        // 1. Synchronous Cache Lookup (Zero Latency)
        val cachedMovie = _uiState.value.rows.flatMap { it.movies }.find { it.movieId == movieId }
        
        if (cachedMovie != null && cachedMovie.description.isNotBlank()) {
            _focusState.value = FocusState(movieId, cachedMovie)
            return
        }

        // 2. Immediate partial state update if ID exists but metadata is thin
        if (cachedMovie != null) {
            _focusState.value = FocusState(movieId, cachedMovie)
        }

        // 3. Asynchronous Enrichment (Only if metadata is missing)
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            val result = repository.getMovieById(movieId)
            result.getOrNull()?.let { movie ->
                // Ensure we haven't navigated away during fetch
                if (_focusState.value?.movieId == movieId) {
                    _focusState.value = FocusState(movieId, movie)
                }
            }
        }
    }

    fun onMovieSelected(movie: Movie) {
        // Handle selection/navigation logic here
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
