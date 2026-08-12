package com.example.peak.domain.model

/**
 * Identity types for media in the PEAK ecosystem.
 */
enum class MediaType {
    MOVIE,
    TV
}

/**
 * Domain model for a Movie or TV Show. 
 * This is what the UI will use to display data.
 */
data class Movie(
    val movieId: String,
    val mediaType: MediaType = MediaType.MOVIE,
    val name: String, 
    val imageUrl: String,
    val backdropUrl: String,
    val description: String = "",
    val ageRating: String = "",
    val rating: String = "",
    val year: String = "",
    val duration: String = "",
    val cast: String = "",
    val director: String = "",
    val genres: String = "",
    val videoUrl: String? = null,
    val subTitle: String? = null, // e.g. "S3 E4 • Old Friends"
    val info: String? = null,     // e.g. "20m left"
    val isEnriched: Boolean = false
)

data class Row(
    val id: String,
    val title: String,
    val movies: List<Movie>,
    val isPlaceholder: Boolean = false
)

data class FocusState(
    val rowId: String,
    val movieId: String,
    val mediaType: MediaType = MediaType.MOVIE,
    val movie: Movie?
)
