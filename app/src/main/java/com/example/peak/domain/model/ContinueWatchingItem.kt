package com.example.peak.domain.model

/**
 * Data model for an item in the Continue Watching system.
 * Designed to be future-proof for both Movies and TV Shows.
 */
data class ContinueWatchingItem(
    val movieId: String,
    val tmdbId: String? = null,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val mediaType: String, // "movie" or "tv"
    val season: Int? = null,
    val episode: Int? = null,
    val durationMs: Long,
    val positionMs: Long,
    val lastWatched: Long = System.currentTimeMillis(),
    val completed: Boolean = false
) {
    /**
     * Calculates the progress as a float between 0.0 and 1.0.
     */
    val progress: Float
        get() = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f

    /**
     * Rule: Automatically marked as completed if position is >= 95% of duration.
     */
    fun isEffectivelyCompleted(): Boolean {
        if (durationMs <= 0) return false
        return positionMs >= (durationMs * 0.95)
    }

    /**
     * Maps this item back to a domain [Movie] model for reuse in existing UI components.
     */
    fun toMovie(): Movie {
        return Movie(
            movieId = movieId,
            name = title,
            imageUrl = posterPath ?: "",
            backdropUrl = backdropPath ?: "",
            description = "" // Not needed for row display
        )
    }
}
