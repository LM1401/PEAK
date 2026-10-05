package com.example.peak.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoveryResultsViewModel(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val TAG = "DiscoveryResultsVM"

    private val _uiState = MutableStateFlow(DiscoveryResultsState())
    val uiState: StateFlow<DiscoveryResultsState> = _uiState.asStateFlow()

    fun loadDiscoveryResults(title: String, genreIds: String) {
        if (_uiState.value.title == title && _uiState.value.results.isNotEmpty()) return

        _uiState.value = DiscoveryResultsState(title = title, isLoading = true)

        viewModelScope.launch {
            try {
                val result = movieRepository.discoverMedia(genreIds)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        results = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load results"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery load failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun onItemFocused(item: Movie) {
        viewModelScope.launch {
            movieRepository.getMediaById(item.movieId, item.mediaType)
        }
    }
}

data class DiscoveryResultsState(
    val title: String = "",
    val isLoading: Boolean = false,
    val results: List<Movie> = emptyList(),
    val error: String? = null
)

class DiscoveryResultsViewModelFactory(
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoveryResultsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscoveryResultsViewModel(movieRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}