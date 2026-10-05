package com.example.peak.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val TAG = "SearchVM"
    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    private var searchJob: Job? = null
    private var trendingLoaded = false

    init {
        Log.d(TAG, "SearchViewModel Initialized")
        loadTrending()
        loadDefaultSuggestions()
        
        _query
            .debounce(400)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    searchJob?.cancel()
                    loadDefaultSuggestions()
                    _uiState.update { it.copy(results = emptyList(), isLoading = false, error = null) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadDefaultSuggestions() {
        viewModelScope.launch {
            val defaults = com.example.peak.data.search.SearchDefaultsProvider.getPopularSuggestions(movieRepository)
            _uiState.update { it.copy(suggestions = defaults) }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        _uiState.update { it.copy(query = newQuery) }
    }

    fun onItemFocused(item: Movie) {
        // Pre-fetch details if necessary
        viewModelScope.launch {
            movieRepository.getMediaById(item.movieId, item.mediaType)
        }
    }

    private fun loadTrending() {
        if (trendingLoaded) return
        viewModelScope.launch {
            Log.d(TAG, "Loading trending...")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = movieRepository.getTrendingMovies() // We can use movies for recommended
                if (result.isSuccess) {
                    _uiState.update { 
                        it.copy(trendingResults = result.getOrNull() ?: emptyList(), isLoading = false) 
                    }
                    trendingLoaded = true
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Trending load failed", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun performSearch(query: String) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            Log.d(TAG, "Searching for: $normalizedQuery")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = movieRepository.searchMulti(normalizedQuery)
                if (result.isSuccess) {
                    val rawResults = result.getOrNull() ?: emptyList()
                    val suggestions = com.example.peak.data.search.SearchSuggestionEngine.generateSuggestions(rawResults, normalizedQuery)
                    val rankedResults = rankResults(rawResults, normalizedQuery)
                    _uiState.update { 
                        it.copy(
                            results = rankedResults,
                            suggestions = suggestions,
                            isLoading = false 
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Search error") }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Search error") }
            }
        }
    }

    private fun rankResults(items: List<Movie>, query: String): List<Movie> {
        if (query.isEmpty()) return items

        return items.sortedByDescending { item ->
            val title = item.name.lowercase()
            var score = 0.0

            if (title == query) {
                score += 10000.0
            } else if (title.startsWith(query)) {
                score += 5000.0
            } else if (title.contains(" $query")) {
                score += 2000.0
            } else if (title.contains(query)) {
                score += 500.0
            }

            if (item.mediaType == com.example.peak.domain.model.MediaType.MOVIE) {
                score += 100.0
            }

            score
        }
    }
}

class SearchViewModelFactory(
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(movieRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
