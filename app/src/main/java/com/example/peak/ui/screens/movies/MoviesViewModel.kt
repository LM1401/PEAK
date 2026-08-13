package com.example.peak.ui.screens.movies

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.MovieListType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Movies screen.
 * Optimized: Progressive loading of movie categories.
 */
class MoviesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    // Tracks categories that have arrived
    private val _apiRowsMap = MutableStateFlow<Map<String, Row>>(emptyMap())

    val rows = _apiRowsMap.map { map ->
        val priorityOrder = listOf(
            "movies_trending", "movies_popular", "movies_now_playing", 
            "movies_top_rated", "movies_action", "movies_comedy", "movies_scifi"
        )
        priorityOrder.mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var focusDebounceJob: Job? = null

    init {
        fetchMovies()
        
        // SYNC INITIAL FOCUS: When first rows arrive
        rows.filter { it.isNotEmpty() }.take(1).onEach { movieRows ->
            val initialRow = movieRows.firstOrNull()
            val initialMovie = initialRow?.movies?.firstOrNull()
            if (_focusState.value == null && initialMovie != null) {
                _focusState.value = FocusState(initialRow.id, initialMovie.movieId, initialMovie.mediaType, initialMovie)
            }
            _uiState.update { it.copy(loading = false) }
        }.launchIn(viewModelScope)
    }

    fun fetchMovies() {
        _uiState.update { it.copy(loading = true) }
        
        // 1. PRIMARY: Trending & Popular (Immediate)
        viewModelScope.launch {
            repository.getTrendingMovies().onSuccess { updateRow("movies_trending", "Trending Movies", it) }
            repository.getMovies(MovieListType.POPULAR).onSuccess { updateRow("movies_popular", "Popular Movies", it) }
        }

        // 2. SECONDARY: Status Categories
        viewModelScope.launch {
            repository.getMovies(MovieListType.NOW_PLAYING).onSuccess { updateRow("movies_now_playing", "Now Playing", it) }
            repository.getMovies(MovieListType.TOP_RATED).onSuccess { updateRow("movies_top_rated", "Top Rated", it) }
        }

        // 3. TERTIARY: Genres
        viewModelScope.launch {
            repository.getMovies(MovieListType.GENRE, 28).onSuccess { updateRow("movies_action", "Action", it) }
            repository.getMovies(MovieListType.GENRE, 35).onSuccess { updateRow("movies_comedy", "Comedy", it) }
            repository.getMovies(MovieListType.GENRE, 878).onSuccess { updateRow("movies_scifi", "Sci-Fi & Fantasy", it) }
        }
    }

    private fun updateRow(id: String, title: String, movies: List<Movie>) {
        if (movies.isEmpty()) return
        _apiRowsMap.update { current ->
            current + (id to Row(id, title, movies.take(20)))
        }
    }

    fun onMovieFocused(rowId: String, movie: Movie) {
        if (_focusState.value?.movieId == movie.movieId && _focusState.value?.mediaType == movie.mediaType && _focusState.value?.rowId == rowId) return
        _focusState.value = FocusState(rowId, movie.movieId, movie.mediaType, movie)
        
        if (!movie.isEnriched) {
            focusDebounceJob?.cancel()
            focusDebounceJob = viewModelScope.launch {
                repository.getMediaById(movie.movieId, movie.mediaType).onSuccess { fullMovie ->
                    if (_focusState.value?.movieId == movie.movieId && _focusState.value?.mediaType == movie.mediaType && _focusState.value?.rowId == rowId) {
                        _focusState.value = FocusState(rowId, movie.movieId, movie.mediaType, fullMovie)
                    }
                }
            }
        }
    }

    fun onMovieSelected(movie: Movie) {
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
