package com.example.peak.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Consolidates multiple data sources into a single reactive UI state pipeline.
 * Hardened against network failures with a unified, atomic focus state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _focusedMovieId = MutableStateFlow<String?>(null)

    /**
     * SINGLE SOURCE OF TRUTH: The currently focused movie.
     * Derived atomically from the focused ID and available data sources.
     */
    val currentFocusedMovie: StateFlow<Movie?> = _focusedMovieId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else flow<Movie?> {
                // 1. Instant sync from current rows
                val cached = _uiState.value.rows.flatMap { it.movies }.find { it.movieId == id }
                emit(cached)
                
                // 2. Refresh with full metadata if needed
                if (cached == null || cached.description.isEmpty()) {
                    val result = repository.getMovieById(id)
                    emit(result.getOrNull())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Granular flows derived from the single source of truth to ensure absolute synchronization
    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusedMovieId = currentFocusedMovie.map { it?.movieId }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val focusedMovieBackdropUrl = currentFocusedMovie.map { it?.backdropUrl }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
            
            val combinedRows = buildList {
                if (cwItems.isNotEmpty()) {
                    add(Row("continue_watching", "Continue Watching", cwItems.map { it.toMovie() }))
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
            _uiState.update { it.copy(
                rows = rows,
                continueWatchingProgress = progressMap
            ) }
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
                
                // Set initial focus if none exists
                if (_focusedMovieId.value == null) {
                    _focusedMovieId.value = movies.firstOrNull()?.movieId
                }
                _uiState.update { it.copy(loading = false) }
            } else {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun onMovieFocused(movieId: String) {
        if (_focusedMovieId.value == movieId) return
        _focusedMovieId.value = movieId
        
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            repository.getMovieById(movieId)
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
