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
            if (warmedIds.contains(movie.movieId)) return@forEach
            warmedIds.add(movie.movieId)

            // 1. WARM POSTER (Key: movieId)
            if (movie.imageUrl.isNotBlank()) {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(movie.imageUrl)
                        .memoryCacheKey(movie.movieId)
                        .diskCacheKey(movie.movieId)
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                )
            }

            // 2. WARM BACKDROP (Key: movieId + "_backdrop")
            if (movie.backdropUrl.isNotBlank()) {
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(movie.backdropUrl)
                        .memoryCacheKey(movie.movieId + "_backdrop")
                        .diskCacheKey(movie.movieId + "_backdrop")
                        .allowHardware(true)
                        .crossfade(false)
                        .build()
                )
            }
        }
    }
}
