package com.example.peak.ui.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.peak.data.search.SearchRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: SearchRepository) : ViewModel() {

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

    /**
     * Netflix-style ranking algorithm.
     * Prioritises exact matches, word starts, and popularity.
     */
    private fun rankResults(items: List<SearchItem>, query: String): List<SearchItem> {
        if (query.isEmpty()) return items

        return items.sortedByDescending { item ->
            val title = item.title.lowercase()
            var score = item.popularity * 0.1 // Use popularity as a base tie-breaker

            // 1. Exact match (Highest Priority)
            if (title == query) {
                score += 10000.0
            } 
            // 2. Starts with query (High Priority)
            else if (title.startsWith(query)) {
                score += 5000.0
            } 
            // 3. Contains as a distinct word
            else if (title.contains(" $query")) {
                score += 2000.0
            }
            // 4. Contains query anywhere
            else if (title.contains(query)) {
                score += 500.0
            }

            // 5. Media type bias: prefer movies slightly over TV/Person for short queries
            if (item.type == "movie") {
                score += 100.0
            }

            score
        }
    }
}
