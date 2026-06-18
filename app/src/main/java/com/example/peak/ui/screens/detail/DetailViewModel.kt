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

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DetailViewModel(
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository? = null
) : ViewModel() {
    private val _movie = MutableStateFlow<Movie?>(null)
    val movie: StateFlow<Movie?> = _movie.asStateFlow()

    private val _similarMovies = MutableStateFlow<List<Movie>>(emptyList())
    val similarMovies: StateFlow<List<Movie>> = _similarMovies.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resumePosition = MutableStateFlow<Long>(0L)
    val resumePosition: StateFlow<Long> = _resumePosition.asStateFlow()

    private val _totalDuration = MutableStateFlow<Long>(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    private var currentMovieId: String? = null

    init {
        observeContinueWatching()
    }

    private fun observeContinueWatching() {
        continueWatchingRepository?.continueWatchingItems?.onEach { items ->
            val id = currentMovieId ?: return@onEach
            val item = items.find { it.movieId == id }
            _resumePosition.value = item?.positionMs ?: 0L
            _totalDuration.value = item?.durationMs ?: 0L
        }?.launchIn(viewModelScope)
    }

    fun loadMovie(movieId: String) {
        this.currentMovieId = movieId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // ... fetch movie logic
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getTrending()
                }
                val foundMovie = response.results.find { it.id.toString() == movieId }?.toMovie()

                _movie.value = foundMovie
                _similarMovies.value = response.results.take(10).map { it.toMovie() }

                // Trigger initial check for resume position
                continueWatchingRepository?.let { repo ->
                    val item = repo.continueWatchingItems.value.find { it.movieId == movieId }
                    _resumePosition.value = item?.positionMs ?: 0L
                    _totalDuration.value = item?.durationMs ?: 0L
                }

                _isLoading.value = false
            } catch (_: Exception) {
                // ... fallback logic
                _movie.value = sampleRows().firstOrNull()?.movies?.firstOrNull()
                _similarMovies.value = sampleRows().flatMap { it.movies }
                _isLoading.value = false
            }
        }
    }
}

class DetailViewModelFactory(
    private val continueWatchingRepository: com.example.peak.data.repository.ContinueWatchingRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(continueWatchingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
