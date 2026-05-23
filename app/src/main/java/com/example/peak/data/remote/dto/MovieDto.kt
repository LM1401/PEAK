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
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?
)

/**
 * Extension function to map Remote DTO to Domain Model.
 */
fun TmdbMovie.toMovie(): Movie {
    val displayName = title ?: name ?: "Unknown Title"
    return Movie(
        movieId = id.toString(),
        name = displayName,
        imageUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            ?: "https://via.placeholder.com/1280x720?text=$displayName",
        backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
            ?: "https://via.placeholder.com/1280x720?text=$displayName",
        description = overview ?: "Experience the latest trending story. Now streaming on PEAK.",
        rating = voteAverage?.toString() ?: "8.5"
    )
}
