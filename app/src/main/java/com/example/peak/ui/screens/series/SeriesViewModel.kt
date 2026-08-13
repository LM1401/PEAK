package com.example.peak.ui.screens.series

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.FocusState
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.TvListType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Series screen.
 * Optimized: Progressive loading of TV categories.
 */
class SeriesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    // Tracks categories that have arrived
    private val _apiRowsMap = MutableStateFlow<Map<String, Row>>(emptyMap())

    val rows = _apiRowsMap.map { map ->
        val priorityOrder = listOf(
            "series_trending", "series_popular", "series_top_rated", 
            "series_on_the_air", "series_drama", "series_comedy", "series_scifi"
        )
        priorityOrder.mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusState = MutableStateFlow<FocusState?>(null)
    val focusState: StateFlow<FocusState?> = _focusState.asStateFlow()

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var focusDebounceJob: Job? = null

    init {
        fetchSeries()

        // SYNC INITIAL FOCUS: When first rows arrive
        rows.filter { it.isNotEmpty() }.take(1).onEach { seriesRows ->
            val initialRow = seriesRows.firstOrNull()
            val initialMovie = initialRow?.movies?.firstOrNull()
            if (_focusState.value == null && initialMovie != null) {
                _focusState.value = FocusState(initialRow.id, initialMovie.movieId, initialMovie.mediaType, initialMovie)
            }
            _uiState.update { it.copy(loading = false) }
        }.launchIn(viewModelScope)
    }

    fun fetchSeries() {
        _uiState.update { it.copy(loading = true) }
        
        // 1. PRIMARY: Trending & Popular
        viewModelScope.launch {
            repository.getTrendingSeries().onSuccess { updateRow("series_trending", "Trending Series", it) }
            repository.getSeries(TvListType.POPULAR).onSuccess { updateRow("series_popular", "Popular Series", it) }
        }

        // 2. SECONDARY: Status
        viewModelScope.launch {
            repository.getSeries(TvListType.TOP_RATED).onSuccess { updateRow("series_top_rated", "Top Rated", it) }
            repository.getSeries(TvListType.ON_THE_AIR).onSuccess { updateRow("series_on_the_air", "On The Air", it) }
        }

        // 3. TERTIARY: Genres
        viewModelScope.launch {
            repository.getSeries(TvListType.GENRE, 18).onSuccess { updateRow("series_drama", "Drama", it) }
            repository.getSeries(TvListType.GENRE, 35).onSuccess { updateRow("series_comedy", "Comedy", it) }
            repository.getSeries(TvListType.GENRE, 10765).onSuccess { updateRow("series_scifi", "Sci-Fi & Fantasy", it) }
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

class SeriesViewModelFactory(private val repository: MovieRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
