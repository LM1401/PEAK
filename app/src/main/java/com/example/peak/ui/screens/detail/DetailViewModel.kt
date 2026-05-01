package com.example.peak.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.retrofit.RetrofitInstance
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.sampleRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailViewModel : ViewModel() {
    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _similarMovies = MutableStateFlow<List<Movie>>(emptyList())
    val similarMovies: StateFlow<List<Movie>> = _similarMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadMovie(movieId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getTrending()
                }
                val foundMovie = response.results.find { it.id.toString() == movieId }?.toMovie()

                _movie.value = foundMovie
                _similarMovies.value = response.results.take(10).map { it.toMovie() }
                _isLoading.value = false
            } catch (_: Exception) {
                _movie.value = sampleRows().firstOrNull()?.movies?.firstOrNull()
                _similarMovies.value = sampleRows().flatMap { it.movies }
                _isLoading.value = false
            }
        }
    }
}
