package com.example.peak.ui.image

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to preload image assets into Coil's cache.
 */
object ImagePreloader {
    /**
     * Preloads a list of URLs into the cache without triggering UI recomposition.
     */
    suspend fun preload(context: Context, urls: List<String>) {
        withContext(Dispatchers.IO) {
            val loader = PeakImageLoader.getInstance(context)
            urls.forEach { url ->
                val request = PeakImageLoader.buildRequest(context, url)
                loader.enqueue(request)
            }
        }
    }
}
