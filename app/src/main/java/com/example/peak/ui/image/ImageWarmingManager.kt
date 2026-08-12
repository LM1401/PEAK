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

    // Separate sets to track what has been warmed
    private val warmedPosters = mutableSetOf<String>()
    private val warmedBackdrops = mutableSetOf<String>()

    /**
     * Enqueues image requests to warm the memory cache using unified identity keys.
     */
    fun warm(
        context: Context,
        imageLoader: ImageLoader,
        movies: List<Movie>,
        warmBackdrops: Boolean = false
    ) {
        movies.forEach { movie ->
            val cacheKey = "${movie.mediaType.name}_${movie.movieId}"
            
            val needsPoster = !warmedPosters.contains(cacheKey)
            val needsBackdrop = warmBackdrops && !warmedBackdrops.contains(cacheKey)

            if (!needsPoster && !needsBackdrop) return@forEach

            // 1. WARM POSTER (Key: mediaType_movieId)
            if (needsPoster && movie.imageUrl.isNotBlank()) {
                warmedPosters.add(cacheKey)
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
            if (needsBackdrop && movie.backdropUrl.isNotBlank()) {
                warmedBackdrops.add(cacheKey)
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
