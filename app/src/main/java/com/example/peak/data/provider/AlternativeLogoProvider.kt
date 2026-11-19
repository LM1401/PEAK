package com.example.peak.data.provider

import com.example.peak.domain.model.MediaType

sealed class AlternativeLogoResult {
    data class Found(val url: String) : AlternativeLogoResult()
    object NotFound : AlternativeLogoResult()
    data class RateLimited(val retryAfterSeconds: Int? = null) : AlternativeLogoResult()
    data class Error(val exception: Throwable) : AlternativeLogoResult()
}

data class AltLogoCacheEntry(
    val result: AlternativeLogoResult,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * A stable identity key representing MediaType + TMDB ID.
 * Prevents movie and TV entries with the same numeric TMDB ID from colliding.
 * Suitable for future repository caching and request deduplication.
 */
data class AlternativeLogoKey(
    val mediaType: MediaType,
    val tmdbId: String
) {
    val value: String get() = "${mediaType.name}_$tmdbId"
}

interface AlternativeLogoProvider {
    /**
     * Finds an alternative title logo for the given TMDB ID.
     */
    suspend fun findLogo(tmdbId: String, mediaType: MediaType): AlternativeLogoResult
}
