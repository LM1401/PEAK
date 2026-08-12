package com.example.peak.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
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
            val validCwItems = cwItems.filter { item ->
                item.movieId.isNotBlank() &&
                item.title.isNotBlank() &&
                item.progress > 0.01f &&
                item.progress < 0.95f &&
                !item.posterPath.isNullOrBlank() &&
                !item.movieId.startsWith("test_")
            }

            val progressMap = validCwItems.associateBy({ "${it.mediaType.name}_${it.movieId}" }, { it.progress })
            
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
            _uiState.update { it.copy(
                rows = rows,
                continueWatchingProgress = progressMap,
                loading = false
            ) }

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

    fun onMovieFocused(rowId: String, movieId: String, mediaType: MediaType) {
        if (_focusState.value?.movieId == movieId && _focusState.value?.mediaType == mediaType && _focusState.value?.rowId == rowId) return

        // 1. Immediate update from existing row data (fast path)
        val cachedMovie = _uiState.value.rows.find { it.id == rowId }?.movies?.find { it.movieId == movieId && it.mediaType == mediaType }
        
        if (cachedMovie != null) {
            _focusState.value = FocusState(rowId, movieId, mediaType, cachedMovie)
            
            // If already enriched, we're done
            if (cachedMovie.isEnriched) return
        }

        // 2. Asynchronous Enrichment (Only if metadata is missing or not enriched)
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            val result = repository.getMediaById(movieId, mediaType)
            result.getOrNull()?.let { movie ->
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
