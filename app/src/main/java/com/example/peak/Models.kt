package com.example.peak

// Data classes
data class Movie(
    val name: String, 
    val imageUrl: String,
    val description: String = "",
    val ageRating: String = "PG-13" // Added for cinematic hero display
)

data class Row(val title: String, val movies: List<Movie>)

// Sample data for testing your UI
fun sampleRows(): List<Row> {
    val sampleMovies = listOf(
        Movie("Movie 1", "https://link-to-image.jpg", "A thrilling adventure in the unknown."),
        Movie("Movie 2", "https://link-to-image2.jpg", "A heartwarming story about family.")
    )
    return listOf(
        Row("Trending", sampleMovies),
        Row("Popular", sampleMovies)
    )
}
