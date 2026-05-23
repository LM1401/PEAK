package com.example.peak.ui.screens.series

import android.util.Log
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

/**
 * ViewModel for the Series screen.
 * Mirrors HomeViewModel pattern.
 */
class SeriesViewModel(
    private val repository: MovieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

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
                        val movieRows = mutableListOf<Row>()
                        
                        movieRows.add(Row("Trending Series", series.shuffled().take(10)))
                        movieRows.add(Row("Binge-worthy Dramas", series.shuffled().take(10)))
                        movieRows.add(Row("Comedy Series", series.shuffled().take(10)))
                        movieRows.add(Row("Sci-Fi Series", series.shuffled().take(10)))
                        movieRows.add(Row("Documentary Series", series.shuffled().take(10)))

                        _uiState.update { 
                            it.copy(
                                rows = movieRows, 
                                loading = false, 
                                selectedMovie = series.first()
                            ) 
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
        focusDebounceJob?.cancel()
        focusDebounceJob = viewModelScope.launch {
            delay(180) 
            _uiState.update { it.copy(selectedMovie = movie) }
        }
    }

    fun onMovieSelected(movie: Movie) {
        focusDebounceJob?.cancel()
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
