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
    val genres: String = "Action, Adventure, Thriller"
)

data class Row(val title: String, val movies: List<Movie>)

// Sample data for testing your UI
fun sampleRows(): List<Row> {
    val sampleMovies = listOf(
        Movie(
            movieId = "1", 
            name = "Movie 1", 
            imageUrl = "https://link-to-image.jpg", 
            backdropUrl = "https://link-to-backdrop-image.jpg", 
            description = "A thrilling adventure in the unknown.", 
            ageRating = "PG-13", 
            rating = "8.5"
        ),
        Movie(
            movieId = "2", 
            name = "Movie 2", 
            imageUrl = "https://link-to-image2.jpg", 
            backdropUrl = "https://link-to-backdrop-image2.jpg", 
            description = "A heartwarming story about family.", 
            ageRating = "PG-13", 
            rating = "8.2"
        )
    )
    return listOf(
        Row("Trending", sampleMovies),
        Row("Popular", sampleMovies)
    )
}
