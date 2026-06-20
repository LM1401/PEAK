package com.example.peak.ui.screens.series

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Series screen.
 * Refactored for extreme recomposition isolation.
 */
class SeriesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    val rows = _uiState.map { it.rows }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMovie = _uiState.map { it.selectedMovie }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _focusedMovieId = MutableStateFlow<String?>(null)
    val focusedMovieId: StateFlow<String?> = _focusedMovieId.asStateFlow()

    private val _focusedMovieBackdropUrl = MutableStateFlow<String?>(null)
    val focusedMovieBackdropUrl = _focusedMovieBackdropUrl.asStateFlow()

    val loading = _uiState.map { it.loading }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var focusDebounceJob: Job? = null

    init {
        fetchSeries()
    }

    fun fetchSeries() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            repository.getTrendingSeries()
                .onSuccess { series ->
                    if (series.isNotEmpty()) {
                        val movieRows = listOf(
                            Row("series_trending", "Trending Series", series.shuffled().take(10)),
                            Row("series_drama", "Binge-worthy Dramas", series.shuffled().take(10)),
                            Row("series_comedy", "Comedy Series", series.shuffled().take(10)),
                            Row("series_scifi", "Sci-Fi Series", series.shuffled().take(10)),
                            Row("series_doc", "Documentary Series", series.shuffled().take(10))
                        )

                        val initialSeries = series.firstOrNull()
                        _uiState.update { 
                            it.copy(
                                rows = movieRows, 
                                loading = false, 
                                selectedMovie = initialSeries
                            ) 
                        }
                        if (_focusedMovieBackdropUrl.value == null) {
                            _focusedMovieBackdropUrl.value = initialSeries?.backdropUrl
                        }
                    } else {
                        _uiState.update { it.copy(loading = false) }
                    }
                }
                .onFailure { exception ->
                    Log.e("PEAK_API", "Failed to fetch series", exception)
                    _uiState.update { it.copy(loading = false) }
                }
        }
    }

    fun onMovieFocused(movie: Movie) {
        if (_focusedMovieId.value == movie.movieId) return
        _focusedMovieId.value = movie.movieId
        _focusedMovieBackdropUrl.value = movie.backdropUrl
        
        // Background preloading of metadata
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            launch { repository.getMovieById(movie.movieId) }
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
