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
     * Enriches the model with Netflix-style metadata for expanded focus states.
     */
    fun toMovie(): Movie {
        // Only generate a subtitle if it's episodic content
        val sub = if (season != null && episode != null) {
            "S$season E$episode"
        } else {
            null
        }
        
        val remainingMs = durationMs - positionMs
        val remainingMin = remainingMs / 60000
        val infoText = if (remainingMin > 0) "${remainingMin}m left" else null

        return Movie(
            movieId = movieId,
            name = title,
            imageUrl = posterPath ?: "",
            backdropUrl = backdropPath ?: "",
            description = "",
            subTitle = sub,
            info = infoText
        )
    }
}
