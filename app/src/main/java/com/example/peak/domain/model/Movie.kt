package com.example.peak.domain.model

/**
 * Domain model for a Movie. 
 * This is what the UI will use to display data.
 */
data class Movie(
    val movieId: String,
    val name: String, 
    val imageUrl: String,
    val backdropUrl: String,
    val description: String = "",
    val ageRating: String = "PG-13",
    val rating: String = "8.5",
    val year: String = "2024",
    val duration: String = "2h 15m",
    val cast: String = "Actor One, Actor Two, Actor Three",
    val director: String = "Director Name",
    val genres: String = "Action, Adventure, Thriller",
    val videoUrl: String? = null
)

data class Row(val title: String, val movies: List<Movie>)
