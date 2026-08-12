package com.example.peak.ui.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Singleton ImageLoader provider for optimized TV caching.
 */
object PeakImageLoader {
    private var instance: ImageLoader? = null

    @Synchronized
    fun getInstance(context: Context): ImageLoader {
        if (instance == null) {
            val okHttpClient = okhttp3.OkHttpClient.Builder()
                .dispatcher(okhttp3.Dispatcher().apply {
                    maxRequestsPerHost = 15
                })
                .build()

            instance = ImageLoader.Builder(context.applicationContext)
                .okHttpClient(okHttpClient)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.05)
                        .minimumMaxSizeBytes(256 * 1024 * 1024L) // 256MB
                        .build()
                }
                // TV Optimizations
                .allowHardware(true)
                .crossfade(false)
                .build()
        }
        return instance!!
    }

    /**
     * Helper to build a standard, cached ImageRequest.
     * Ensures memory and disk cache policies are explicitly enabled.
     */
    fun buildRequest(context: Context, url: String?): ImageRequest {
        val safeUrl = url?.takeIf { it.isNotBlank() } ?: ""
        return ImageRequest.Builder(context)
            .data(safeUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
}
