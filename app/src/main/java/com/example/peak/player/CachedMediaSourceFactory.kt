package com.example.peak.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * Singleton Factory for providing MediaSources with transparent disk caching.
 * Prevents redundant object creation and ensures upstream stability.
 */
@OptIn(UnstableApi::class)
object CachedMediaSourceFactory {
    
    private var instance: DefaultMediaSourceFactory? = null

    /**
     * Returns the singleton MediaSourceFactory configured with disk caching.
     */
    @Synchronized
    fun getInstance(context: Context): DefaultMediaSourceFactory {
        if (instance == null) {
            val appContext = context.applicationContext
            
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
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
     * Creates a custom MediaSource for a specific media URL and HTTP request headers
     * with transparent disk caching enabled.
     */
    fun createMediaSource(
        context: Context,
        streamUrl: String,
        headers: Map<String, String> = emptyMap()
    ): androidx.media3.exoplayer.source.MediaSource {
        val appContext = context.applicationContext
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        if (headers.isNotEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(headers)
        }

        val defaultDataSourceFactory = DefaultDataSource.Factory(appContext, httpDataSourceFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(MediaCacheManager.getInstance(appContext))
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DefaultMediaSourceFactory(appContext)
            .setDataSourceFactory(cacheDataSourceFactory)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(streamUrl))
    }
}
