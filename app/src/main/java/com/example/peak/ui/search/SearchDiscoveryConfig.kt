package com.example.peak.ui.search

import androidx.annotation.DrawableRes
import com.example.peak.R

object SearchDiscoveryConfig {

    data class DiscoveryCategory(
        val id: String,
        val label: String,
        val tmdbGenreIds: String,
        @param:DrawableRes val iconRes: Int? = null
    )

    // The extended list shown when clicking "Learn More"
    val allCategories = listOf(
        DiscoveryCategory("action", "Action", "28", R.drawable.ic_cat_action),
        DiscoveryCategory("adventure", "Adventure", "12", R.drawable.ic_cat_adventure),
        DiscoveryCategory("animation", "Animation", "16", R.drawable.ic_cat_animation),
        DiscoveryCategory("comedy", "Comedy", "35", R.drawable.ic_cat_comedy),
        DiscoveryCategory("crime", "Crime", "80", R.drawable.ic_cat_crime),
        DiscoveryCategory("documentary", "Documentary", "99", R.drawable.ic_cat_documentary),
        DiscoveryCategory("drama", "Drama", "18", R.drawable.ic_cat_drama),
        DiscoveryCategory("family", "Family", "10751", R.drawable.ic_cat_family),
        DiscoveryCategory("fantasy", "Fantasy", "14", R.drawable.ic_cat_fantasy),
        DiscoveryCategory("history", "History", "36", R.drawable.ic_cat_history),
        DiscoveryCategory("horror", "Horror", "27", R.drawable.ic_cat_horror),
        DiscoveryCategory("mystery", "Mystery", "9648", R.drawable.ic_cat_mystery),
        DiscoveryCategory("romance", "Romance", "10749", R.drawable.ic_cat_romance),
        DiscoveryCategory("scifi", "Science Fiction", "878", R.drawable.ic_cat_scifi),
        DiscoveryCategory("thriller", "Thriller", "53", R.drawable.ic_cat_thriller),
        DiscoveryCategory("war", "War", "10752", R.drawable.ic_cat_war),
        DiscoveryCategory("western", "Western", "37", R.drawable.ic_cat_western),
        DiscoveryCategory("biopic", "Biopic", "36,18", R.drawable.ic_cat_biopic),
        DiscoveryCategory("creature_feature", "Creature Feature", "27|878", R.drawable.ic_cat_creature_feature),
        DiscoveryCategory("superhero", "Superhero", "28,878", R.drawable.ic_cat_superhero)
    )

    // The primary 6 buttons on the Search Screen
    val mainCategories = listOf(
        allCategories.first { it.id == "horror" },
        allCategories.first { it.id == "scifi" },
        allCategories.first { it.id == "comedy" },
        allCategories.first { it.id == "action" },
        allCategories.first { it.id == "thriller" },
        allCategories.first { it.id == "mystery" }
    )
}
