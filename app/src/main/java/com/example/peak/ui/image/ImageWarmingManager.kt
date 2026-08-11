package com.example.peak.ui.image

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie

/**
 * Manager responsible for pre-decoding images into memory.
 * This prevents the "first-frame flash" or decode delay during D-pad navigation.
 */
object ImageWarmingManager {

    // Simple session cache to avoid duplicate warming requests
    private val warmedIds = mutableSetOf<String>()

    /**
     * Enqueues image requests to warm the memory cache using unified identity keys.
     */
    fun warm(
        context: Context,
        imageLoader: ImageLoader,
        movies: List<Movie>
    ) {
        movies.forEach { movie ->
            val cacheKey = "${movie.mediaType.name}_${movie.movieId}"
            if (warmedIds.contains(cacheKey)) return@forEach
            warmedIds.add(cacheKey)

            // 1. WARM POSTER (Key: mediaType_movieId)
            if (movie.imageUrl.isNotBlank()) {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(movie.imageUrl)
                        .memoryCacheKey(cacheKey)
                        .diskCacheKey(cacheKey)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                )
            }

            // 2. WARM BACKDROP (Key: mediaType_movieId + "_backdrop")
            if (movie.backdropUrl.isNotBlank()) {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(movie.backdropUrl)
                        .memoryCacheKey(cacheKey + "_backdrop")
                        .diskCacheKey(cacheKey + "_backdrop")
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                )
            }
        }
    }
}
