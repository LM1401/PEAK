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

            // Establish initial focus ONLY if not set and we have real rows
            if (_focusState.value == null) {
                rows.firstOrNull { !it.isPlaceholder }?.let { row ->
                    row.movies.firstOrNull()?.let { movie ->
                        if (movie.movieId.isNotBlank()) {
                            _focusState.value = FocusState(row.id, movie.movieId, movie.mediaType, movie)
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun fetchMovies() {
        _uiState.update { it.copy(loading = true) }

        // PROGRESSIVE LOADING: Launch independent tasks so UI updates as data arrives
        
        // 1. PRIMARY: Trending (Immediate)
        viewModelScope.launch {
            repository.getTrendingMovies().onSuccess { movies ->
                updateRow("trending", "Trending Now", movies)
            }
        }

        // 2. SECONDARY: Popular/Top Rated (Background)
        viewModelScope.launch {
            repository.getMovies(MovieListType.POPULAR).onSuccess { movies ->
                updateRow("popular_movies", "Popular Movies", movies)
            }
            repository.getSeries(TvListType.POPULAR).onSuccess { series ->
                updateRow("popular_series", "Popular Series", series)
            }
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
        if (_focusState.value?.movieId == movieId && _focusState.value?.mediaType == mediaType && _focusState.value?.rowId == rowId) return

        val cachedMovie = _uiState.value.rows.find { it.id == rowId }?.movies?.find { it.movieId == movieId && it.mediaType == mediaType }
        
        if (cachedMovie != null) {
            _focusState.value = FocusState(rowId, movieId, mediaType, cachedMovie)
            if (cachedMovie.isEnriched) return
        }

        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
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
