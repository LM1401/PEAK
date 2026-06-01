package com.example.peak.ui.search

data class SearchItem(
    val id: String,
    val title: String,
    val year: String? = null,
    val type: String, // "movie" or "tv"
    val posterUrl: String? = null,
    val popularity: Double = 0.0
)
