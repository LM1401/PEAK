package com.example.peak.ui.search

data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchItem> = emptyList(),
    val trendingResults: List<SearchItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val error: String? = null
)
