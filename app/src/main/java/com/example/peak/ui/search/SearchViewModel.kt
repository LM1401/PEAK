package com.example.peak.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.peak.data.search.SearchRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val repository: SearchRepository,
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
            val defaults = com.example.peak.data.search.SearchDefaultsProvider.getPopularSuggestions(repository)
            _uiState.update { it.copy(suggestions = defaults) }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        _uiState.update { it.copy(query = newQuery) }
    }

    /**
     * PREDICTIVE PRELOADING: Prefetch movie details when a search result is focused.
     * This ensures that if the user clicks, the Detail screen has data immediately.
     */
    fun onItemFocused(item: SearchItem) {
        viewModelScope.launch {
            movieRepository.getMediaById(item.id, item.type)
        }
    }

    private fun loadTrending() {
        if (trendingLoaded) return
        viewModelScope.launch {
            Log.d(TAG, "Loading trending...")
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val trending = repository.getTrending()
                _uiState.update { 
                    it.copy(trendingResults = trending, isLoading = false) 
                }
                trendingLoaded = true
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
                val rawResults = repository.search(normalizedQuery)
                
                // 1. Generate Autocomplete Suggestions (Fast Response Feel)
                val suggestions = com.example.peak.data.search.SearchSuggestionEngine
                    .generateSuggestions(rawResults, normalizedQuery)
                
                // 2. Apply Deep Ranking for Grid
                val rankedResults = rankResults(rawResults, normalizedQuery)
                
                _uiState.update { 
                    it.copy(
                        results = rankedResults,
                        suggestions = suggestions,
                        isLoading = false 
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Search error") }
            }
        }
    }

    private fun rankResults(items: List<SearchItem>, query: String): List<SearchItem> {
        if (query.isEmpty()) return items

        return items.sortedByDescending { item ->
            val title = item.title.lowercase()
            var score = item.popularity * 0.1

            if (title == query) {
                score += 10000.0
            } else if (title.startsWith(query)) {
                score += 5000.0
            } else if (title.contains(" $query")) {
                score += 2000.0
            } else if (title.contains(query)) {
                score += 500.0
            }

            if (item.type == MediaType.MOVIE) {
                score += 100.0
            }

            score
        }
    }
}

class SearchViewModelFactory(
    private val repository: SearchRepository,
    private val movieRepository: MovieRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository, movieRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
