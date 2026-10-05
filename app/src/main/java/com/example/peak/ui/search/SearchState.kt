package com.example.peak.ui.search

import com.example.peak.domain.model.Movie

data class SearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<Movie> = emptyList(),
    val trendingResults: List<Movie> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val error: String? = null
)
