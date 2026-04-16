package com.example.peak.data.remote.dto

import com.example.peak.domain.model.Movie
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) representing the response from TMDB API.
 */
data class TmdbResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?
)

/**
 * Extension function to map Remote DTO to Domain Model.
 */
fun TmdbMovie.toMovie(): Movie {
    return Movie(
        name = title,
        imageUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            ?: "https://via.placeholder.com/1280x720?text=$title",
        backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
            ?: "https://via.placeholder.com/1280x720?text=$title",
        description = overview ?: "Experience the latest trending story. Now streaming on PEAK."
    )
}
