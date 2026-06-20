package com.example.peak.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import java.io.File

/**
 * Singleton manager for Media3 Disk Caching.
 * Hardened to ensure Context safety and atomic initialization.
 */
@OptIn(UnstableApi::class)
object MediaCacheManager {
    private var cache: SimpleCache? = null
    private const val CACHE_SIZE = 500 * 1024 * 1024L // 500MB Cache

    /**
     * Returns the singleton SimpleCache instance.
     * Uses applicationContext internally to prevent Activity leaks.
     */
    @Synchronized
    fun getInstance(context: Context): SimpleCache {
        val appContext = context.applicationContext
        if (cache == null) {
            val cacheDir = File(appContext.cacheDir, "media_cache")
            val databaseProvider = StandaloneDatabaseProvider(appContext)
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            
            // SimpleCache initialization is expensive; singleton ensures it's done once.
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }
}
