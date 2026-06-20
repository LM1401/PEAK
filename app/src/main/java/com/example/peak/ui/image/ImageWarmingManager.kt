package com.example.peak.ui.image

import android.content.Context
import coil.ImageLoader
import coil.request.ImageRequest

/**
 * Manager responsible for pre-decoding images into memory.
 * This prevents the "first-frame flash" or decode delay during D-pad navigation.
 */
object ImageWarmingManager {

    /**
     * Enqueues image requests to warm the memory cache.
     */
    fun warm(
        context: Context,
        imageLoader: ImageLoader,
        urls: List<String>
    ) {
        urls.forEach { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(true)
                .memoryCacheKey(url)
                .diskCacheKey(url)
                // Use a lower priority for warming to avoid interfering with current UI loads
                .build()

            imageLoader.enqueue(request)
        }
    }
}
