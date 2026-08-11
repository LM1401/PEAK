package com.example.peak.ui.search

import com.example.peak.domain.model.MediaType

data class SearchItem(
    val id: String,
    val title: String,
    val year: String? = null,
    val type: MediaType, // MOVIE or TV
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val overview: String? = null,
    val rating: Double = 0.0,
    val popularity: Double = 0.0
)

fun SearchItem.toMovie(): com.example.peak.domain.model.Movie {
    return com.example.peak.domain.model.Movie(
        movieId = id,
        mediaType = type,
        name = title,
        imageUrl = posterUrl ?: "https://via.placeholder.com/500x750?text=$title",
        backdropUrl = backdropUrl ?: "https://via.placeholder.com/1280x720?text=$title",
        description = overview ?: "Experience the latest trending story. Now streaming on PEAK.",
        rating = String.format("%.1f", rating),
        year = year ?: "2024"
    )
}
