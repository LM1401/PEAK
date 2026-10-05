package com.example.peak.ui.search

object SearchDiscoveryConfig {

    data class DiscoveryCategory(
        val id: String,
        val label: String,
        val tmdbGenreIds: String
    )

    // The primary 6 buttons on the Search Screen
    val mainCategories = listOf(
        DiscoveryCategory("adrenaline", "I'm craving adrenaline", "28|12"),
        DiscoveryCategory("fantasy", "Fantasy adventure", "14|12"),
        DiscoveryCategory("soundtracks", "Memorable soundtracks", "10402"),
        DiscoveryCategory("vacations", "Virtual vacations", "12|99"),
        DiscoveryCategory("thrillers", "Psychological thrillers", "53|9648"),
        DiscoveryCategory("comedies", "Feel-good comedies", "35|10751")
    )

    // The extended list shown when clicking "Learn More"
    val allCategories = listOf(
        DiscoveryCategory("action", "Action", "28"),
        DiscoveryCategory("adventure", "Adventure", "12"),
        DiscoveryCategory("animation", "Animation", "16"),
        DiscoveryCategory("comedy", "Comedy", "35"),
        DiscoveryCategory("crime", "Crime", "80"),
        DiscoveryCategory("documentary", "Documentary", "99"),
        DiscoveryCategory("drama", "Drama", "18"),
        DiscoveryCategory("family", "Family", "10751"),
        DiscoveryCategory("fantasy", "Fantasy", "14"),
        DiscoveryCategory("history", "History", "36"),
        DiscoveryCategory("horror", "Horror", "27"),
        DiscoveryCategory("music", "Music", "10402"),
        DiscoveryCategory("mystery", "Mystery", "9648"),
        DiscoveryCategory("romance", "Romance", "10749"),
        DiscoveryCategory("scifi", "Science Fiction", "878"),
        DiscoveryCategory("thriller", "Thriller", "53"),
        DiscoveryCategory("war", "War", "10752"),
        DiscoveryCategory("western", "Western", "37")
    )
}
