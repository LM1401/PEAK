package com.example.peak.ui.screens.movies

import com.example.peak.data.remote.recognition.BrandDefinition
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Row

/**
 * Optional technical audio/video playback capabilities for Hero items.
 * Fields are optional and default to false to prevent fabricating technical specs.
 */
data class TechnicalQualityFlags(
    val has4k: Boolean = false,
    val hasHdr: Boolean = false,
    val hasDolbyVision: Boolean = false,
    val hasDolbyAtmos: Boolean = false
)

/**
 * State for the Hero section carousel.
 */
data class HeroSectionState(
    val featuredMovies: List<Movie> = emptyList(),
    val selectedIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentMovie: Movie? get() = featuredMovies.getOrNull(selectedIndex)
}

/**
 * Data model for a brand collection discovery tile (e.g., Disney, Marvel, Star Wars).
 */
data class CollectionItem(
    val id: String,
    val name: String,
    val brandKey: String,
    val brandDefinition: BrandDefinition? = null,
    val logoUrl: String? = null,
    val backdropUrl: String? = null
)

/**
 * State for the Collections section carousel.
 */
data class CollectionsSectionState(
    val items: List<CollectionItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Ranked movie item for Top 10 carousel with deterministic 1-based ordering.
 */
data class RankedMovie(
    val rank: Int,
    val movie: Movie
)

/**
 * State for the Top 10 section.
 */
data class Top10SectionState(
    val title: String = "Top 10 in the UK Today",
    val items: List<RankedMovie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Generic state holder for horizontal movie carousels.
 */
data class SectionState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Single state holder for the Movies Screen (V2 Architecture).
 */
data class MoviesUiState(
    val heroState: HeroSectionState = HeroSectionState(),
    val collectionsState: CollectionsSectionState = CollectionsSectionState(),
    val trendingState: SectionState<Movie> = SectionState(),
    val top10State: Top10SectionState = Top10SectionState(),
    val categoryRows: List<Row> = emptyList(),
    val loading: Boolean = false,
    val selectedMovie: Movie? = null,
    // Backwards compatibility field for existing MoviesScreen
    val rows: List<Row> = emptyList()
)
