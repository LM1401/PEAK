package com.example.peak.domain.model

/**
 * Domain model for a Movie. 
 * This is what the UI will use to display data.
 */
data class Movie(
    val name: String, 
    val imageUrl: String,
    val backdropUrl: String,
    val description: String = "",
    val ageRating: String = "PG-13"
)

data class Row(val title: String, val movies: List<Movie>)

// Sample data for testing your UI
fun sampleRows(): List<Row> {
    val sampleMovies = listOf(
        Movie("Movie 1", "https://link-to-image.jpg", "https://link-to-backdrop-image.jpg", "A thrilling adventure in the unknown."),
        Movie("Movie 2", "https://link-to-image2.jpg", "https://link-to-backdrop-image2.jpg", "A heartwarming story about family.")
    )
    return listOf(
        Row("Trending", sampleMovies),
        Row("Popular", sampleMovies)
    )
}
