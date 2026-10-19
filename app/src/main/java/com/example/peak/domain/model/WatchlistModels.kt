package com.example.peak.domain.model

/**
 * Domain representation of a single Watchlist/List.
 */
data class Watchlist(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val items: List<Movie> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val titleCount: Int
        get() = items.size
}

/**
 * Lightweight persisted representation of a title in a watchlist.
 */
data class WatchlistItem(
    val movieId: String,
    val mediaType: MediaType = MediaType.MOVIE,
    val movie: Movie? = null,
    val addedAt: Long = System.currentTimeMillis()
)
