package com.example.peak.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource

/**
 * Singleton Factory for providing MediaSources with transparent disk caching.
 * Prevents redundant object creation and ensures upstream stability.
 */
@OptIn(UnstableApi::class)
object CachedMediaSourceFactory {

    const val DEFAULT_USER_AGENT = "PEAK/1.0 (Android TV; Media3)"
    
    private var instance: DefaultMediaSourceFactory? = null

    /**
     * Sanitizes HTTP headers map:
     * - Trims and removes empty or blank header keys/values.
     * - Filters out manual "Range" / "range" headers to let ExoPlayer manage byte-range requests safely.
     */
    fun sanitizeHeaders(headers: Map<String, String>?): Map<String, String> {
        if (headers.isNullOrEmpty()) return emptyMap()
        val sanitized = LinkedHashMap<String, String>()
        headers.forEach { (rawKey, rawValue) ->
            val key = rawKey.trim()
            val value = rawValue.trim()
            if (key.isNotEmpty() && value.isNotEmpty() && !key.equals("Range", ignoreCase = true)) {
                sanitized[key] = value
            }
        }
        return sanitized
    }

    /**
     * Returns the singleton MediaSourceFactory configured with disk caching and default network settings.
     */
    @Synchronized
    fun getInstance(context: Context): DefaultMediaSourceFactory {
        if (instance == null) {
            val appContext = context.applicationContext
            
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(DEFAULT_USER_AGENT)
                .setAllowCrossProtocolRedirects(true)

            val defaultDataSourceFactory = DefaultDataSource.Factory(appContext, httpDataSourceFactory)

            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(MediaCacheManager.getInstance(appContext))
                .setUpstreamDataSourceFactory(defaultDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            instance = DefaultMediaSourceFactory(appContext)
                .setDataSourceFactory(cacheDataSourceFactory)
        }
        return instance!!
    }

    /**
     * Creates a MediaSource for a specific media URL and HTTP request headers
     * with transparent disk caching and sanitized header handling.
     */
    fun createMediaSource(
        context: Context,
        streamUrl: String,
        headers: Map<String, String> = emptyMap()
    ): MediaSource {
        val appContext = context.applicationContext
        val sanitizedHeaders = sanitizeHeaders(headers)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        // Set default User-Agent fallback if no custom User-Agent is provided in headers
        val hasCustomUserAgent = sanitizedHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUserAgent) {
            httpDataSourceFactory.setUserAgent(DEFAULT_USER_AGENT)
        }

        if (sanitizedHeaders.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(sanitizedHeaders)
        }

        val defaultDataSourceFactory = DefaultDataSource.Factory(appContext, httpDataSourceFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(MediaCacheManager.getInstance(appContext))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(cacheDataSourceFactory)
            .createMediaSource(MediaItem.fromUri(streamUrl))
    }
}
