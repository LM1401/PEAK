package com.example.peak.ui.image

import android.content.Context
import coil.ImageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.peak.domain.model.Movie
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager responsible for pre-decoding images into memory.
 * Uses thread-safe in-flight tracking and direct MemoryCache verification.
 */
object ImageWarmingManager {

    private val inFlightKeys = ConcurrentHashMap.newKeySet<String>()
    private val inFlightDisposables = ConcurrentHashMap<String, coil.request.Disposable>()

    fun cancelWarming(movie: Movie, cancelBackdropOnly: Boolean = false) {
        val baseKey = "${movie.mediaType.name}_${movie.movieId}"
        val backdropKey = "${baseKey}_backdrop"
        
        inFlightDisposables.remove(backdropKey)?.dispose()
        inFlightKeys.remove(backdropKey)
        
        if (!cancelBackdropOnly) {
            inFlightDisposables.remove(baseKey)?.dispose()
            inFlightKeys.remove(baseKey)
        }
    }

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
            val baseKey = "${movie.mediaType.name}_${movie.movieId}"
            val posterKey = baseKey
            val backdropKey = "${baseKey}_backdrop"

            // 1. WARM POSTER (Key: mediaType_movieId)
            if (movie.imageUrl.isNotBlank()) {
                val isInMemory = imageLoader.memoryCache?.get(MemoryCache.Key(posterKey)) != null
                if (!isInMemory && inFlightKeys.add(posterKey)) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(movie.imageUrl)
                            .memoryCacheKey(posterKey)
                            .diskCacheKey(posterKey)
                            .allowHardware(true)
                            .crossfade(false)
                            .listener(
                                onSuccess = { _, _ -> inFlightKeys.remove(posterKey); inFlightDisposables.remove(posterKey) },
                                onError = { _, _ -> inFlightKeys.remove(posterKey); inFlightDisposables.remove(posterKey) },
                                onCancel = { _ -> inFlightKeys.remove(posterKey); inFlightDisposables.remove(posterKey) }
                            )
                            .build()
                        inFlightDisposables[posterKey] = imageLoader.enqueue(request)
                    } catch (_: Throwable) {
                        inFlightKeys.remove(posterKey)
                        inFlightDisposables.remove(posterKey)
                    }
                }
            }

            // 2. WARM BACKDROP (Key: mediaType_movieId + "_backdrop")
            if (warmBackdrops && movie.backdropUrl.isNotBlank()) {
                val isInMemory = imageLoader.memoryCache?.get(MemoryCache.Key(backdropKey)) != null
                if (!isInMemory && inFlightKeys.add(backdropKey)) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(movie.backdropUrl)
                            .memoryCacheKey(backdropKey)
                            .diskCacheKey(backdropKey)
                            .allowHardware(true)
                            .crossfade(false)
                            .listener(
                                onSuccess = { _, _ -> inFlightKeys.remove(backdropKey); inFlightDisposables.remove(backdropKey) },
                                onError = { _, _ -> inFlightKeys.remove(backdropKey); inFlightDisposables.remove(backdropKey) },
                                onCancel = { _ -> inFlightKeys.remove(backdropKey); inFlightDisposables.remove(backdropKey) }
                            )
                            .build()
                        inFlightDisposables[backdropKey] = imageLoader.enqueue(request)
                    } catch (_: Throwable) {
                        inFlightKeys.remove(backdropKey)
                        inFlightDisposables.remove(backdropKey)
                    }
                }
            }

            // 3. WARM TITLE LOGO (Key: titleLogoUrl)
            val logoUrl = movie.titleLogoUrl
            if (movie.isEnriched && !logoUrl.isNullOrBlank()) {
                val logoKey = logoUrl
                val isInMemory = imageLoader.memoryCache?.get(MemoryCache.Key(logoKey)) != null
                if (!isInMemory && inFlightKeys.add(logoKey)) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(logoUrl)
                            .memoryCacheKey(logoKey)
                            .diskCacheKey(logoKey)
                            .allowHardware(true)
                            .crossfade(false)
                            .listener(
                                onSuccess = { _, _ -> inFlightKeys.remove(logoKey); inFlightDisposables.remove(logoKey) },
                                onError = { _, _ -> inFlightKeys.remove(logoKey); inFlightDisposables.remove(logoKey) },
                                onCancel = { _ -> inFlightKeys.remove(logoKey); inFlightDisposables.remove(logoKey) }
                            )
                            .build()
                        inFlightDisposables[logoKey] = imageLoader.enqueue(request)
                    } catch (_: Throwable) {
                        inFlightKeys.remove(logoKey)
                        inFlightDisposables.remove(logoKey)
                    }
                }
            }
        }
    }
}
