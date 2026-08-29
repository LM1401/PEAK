package com.example.peak.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.TvListType
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Optimized: Progressive content loading to ensure immediate UI responsiveness.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: MovieRepository,
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val continueWatchingProgress = _uiState.map { it.continueWatchingProgress }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Tracks categories that have arrived
    private val _apiRowsMap = MutableStateFlow<Map<String, Row>>(emptyMap())
    private var focusDebounceJob: Job? = null
    private var hasUserInteracted = false

    init {
        setupStatePipeline()
        fetchMovies()
    }

    private fun setupStatePipeline() {
        combine(
            continueWatchingRepository.continueWatchingItems,
            _apiRowsMap
        ) { cwItems, apiRowsMap ->
            val validCwItems = cwItems.filter { item ->
                item.movieId.isNotBlank() &&
                item.title.isNotBlank() &&
                item.progress > 0.01f &&
                item.progress < 0.95f &&
                !item.posterPath.isNullOrBlank() &&
                !item.movieId.startsWith("test_")
            }

            val progressMap = validCwItems.associateBy({ "${it.mediaType.name}_${it.movieId}" }, { it.progress })
            
            // Deterministic Row Ordering
            val combinedRows = buildList {
                if (validCwItems.isNotEmpty()) {
                    add(Row("continue_watching", "Continue Watching", validCwItems.map { it.toMovie() }))
                }
                
                // Add API rows in priority order if they exist
                val priorityOrder = listOf("trending", "popular_movies", "popular_series", "top_rated_movies")
                priorityOrder.forEach { id ->
                    apiRowsMap[id]?.let { add(it) }
                }
            }

            Pair(combinedRows, progressMap)
        }.onEach { (rows, progressMap) ->
            _uiState.update { it.copy(
                rows = rows,
                continueWatchingProgress = progressMap,
                loading = false
            ) }

            // Establish or update initial focus based on row priority
            val firstRealRow = rows.firstOrNull { !it.isPlaceholder }
            val firstMovie = firstRealRow?.movies?.firstOrNull { it.movieId.isNotBlank() }

            if (firstMovie != null) {
                val potentialFocus = FocusState(firstRealRow.id, firstMovie.movieId, firstMovie.mediaType, firstMovie)
                
                if (_focusState.value == null) {
                    // Initial establishment
                    _focusState.value = potentialFocus
                } else if (!hasUserInteracted && (_focusState.value?.rowId != potentialFocus.rowId || _focusState.value?.movieId != potentialFocus.movieId)) {
                    // Follow Row 0 if it changes (e.g. Continue Watching arrives) before user interaction
                    _focusState.value = potentialFocus
                }
            }
        }.launchIn(viewModelScope)
    }

    fun fetchMovies() {
        _uiState.update { it.copy(loading = true) }

        // PROGRESSIVE LOADING: Launch independent tasks to maximize parallel network work
        
        // 1. PRIMARY: Trending
        viewModelScope.launch {
            repository.getTrendingMovies().onSuccess { movies ->
                updateRow("trending", "Trending Now", movies)
            }
        }

        // 2. SECONDARY: Popular/Top Rated (Independent Background Requests)
        viewModelScope.launch {
            repository.getMovies(MovieListType.POPULAR).onSuccess { movies ->
                updateRow("popular_movies", "Popular Movies", movies)
            }
        }
        viewModelScope.launch {
            repository.getSeries(TvListType.POPULAR).onSuccess { series ->
                updateRow("popular_series", "Popular Series", series)
            }
        }
        viewModelScope.launch {
            repository.getMovies(MovieListType.TOP_RATED).onSuccess { movies ->
                updateRow("top_rated_movies", "Top Rated Movies", movies)
            }
        }
    }

    private fun updateRow(id: String, title: String, movies: List<Movie>) {
        if (movies.isEmpty()) return
        _apiRowsMap.update { current ->
            current + (id to Row(id, title, movies.take(20)))
        }
    }

    fun onMovieFocused(rowId: String, movieId: String, mediaType: MediaType) {
        val currentFocus = _focusState.value
        if (currentFocus != null && currentFocus.movieId == movieId && currentFocus.mediaType == mediaType && currentFocus.rowId == rowId && currentFocus.movie?.isEnriched == true) return

        // Mark as interacted if we are changing focus from an established state
        if (currentFocus != null) {
            hasUserInteracted = true
        }

        val cachedMovie = _uiState.value.rows.find { it.id == rowId }?.movies?.find { it.movieId == movieId && it.mediaType == mediaType }
        
        if (cachedMovie != null) {
            _focusState.value = FocusState(rowId, movieId, mediaType, cachedMovie)
            if (cachedMovie.isEnriched) return
        }

        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            delay(300L)
            repository.getMediaById(movieId, mediaType).onSuccess { movie ->
                if (_focusState.value?.movieId == movieId && _focusState.value?.mediaType == mediaType && _focusState.value?.rowId == rowId) {
                    _focusState.value = FocusState(rowId, movieId, mediaType, movie)
                }
            }
        }
    }

    fun onMovieSelected(movie: Movie) {
        // Selection logic
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
