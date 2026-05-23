package com.example.peak.ui.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest

/**
 * Singleton ImageLoader provider for optimized TV caching.
 */
object PeakImageLoader {
    private var instance: ImageLoader? = null

    @Synchronized
    fun getInstance(context: Context): ImageLoader {
        if (instance == null) {
            instance = ImageLoader.Builder(context.applicationContext)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .allowHardware(true)
                .crossfade(true)
                .build()
        }
        return instance!!
    }

    /**
     * Helper to build a standard, cached ImageRequest.
     */
    fun buildRequest(context: Context, url: String?): ImageRequest {
        return ImageRequest.Builder(context)
            .data(url)
            .diskCacheKey(url)
            .memoryCacheKey(url)
            .build()
    }
}
