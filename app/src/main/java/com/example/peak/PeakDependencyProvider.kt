package com.example.peak

import android.content.Context
import com.example.peak.data.remote.retrofit.RetrofitInstance
import com.example.peak.data.repository.MovieRepositoryImpl
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.data.continuewatching.ContinueWatchingStorage
import com.example.peak.domain.repository.MovieRepository

/**
 * A lightweight Service Locator to ensure singletons for expensive dependencies.
 * This avoids unnecessary object reconstruction across compositions.
 */
object PeakDependencyProvider {

    private var movieRepository: MovieRepository? = null
    private var continueWatchingRepository: ContinueWatchingRepository? = null
    private var watchlistRepository: com.example.peak.data.repository.WatchlistRepository? = null

    fun getMovieRepository(): MovieRepository {
        return movieRepository ?: synchronized(this) {
            movieRepository ?: MovieRepositoryImpl(RetrofitInstance.api).also { movieRepository = it }
        }
    }

    fun getContinueWatchingRepository(context: Context): ContinueWatchingRepository {
        return continueWatchingRepository ?: synchronized(this) {
            continueWatchingRepository ?: ContinueWatchingRepository(
                ContinueWatchingStorage(context.applicationContext)
            ).also { continueWatchingRepository = it }
        }
    }

    fun getWatchlistRepository(context: Context): com.example.peak.data.repository.WatchlistRepository {
        return watchlistRepository ?: synchronized(this) {
            watchlistRepository ?: com.example.peak.data.repository.WatchlistRepository(
                com.example.peak.data.watchlist.WatchlistStorage(context.applicationContext)
            ).also { watchlistRepository = it }
        }
    }

    fun getRandomSelectionRepository(context: Context): com.example.peak.domain.repository.RandomSelectionRepository {
        return com.example.peak.data.repository.RandomSelectionRepositoryImpl(
            RetrofitInstance.api,
            getContinueWatchingRepository(context)
        )
    }

    private var settingsRepository: com.example.peak.data.repository.SettingsRepository? = null

    fun getSettingsRepository(context: Context): com.example.peak.data.repository.SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            settingsRepository ?: com.example.peak.data.repository.SettingsRepository(
                com.example.peak.data.settings.SettingsStorage(context.applicationContext)
            ).also { settingsRepository = it }
        }
    }

    private var addonRepository: com.example.peak.data.repository.AddonRepository? = null

    fun getAddonRepository(context: Context): com.example.peak.data.repository.AddonRepository {
        return addonRepository ?: synchronized(this) {
            addonRepository ?: com.example.peak.data.repository.AddonRepository(
                com.example.peak.data.addon.AddonStorage(context.applicationContext)
            ).also { addonRepository = it }
        }
    }

    private var addonApiClient: com.example.peak.data.addon.AddonApiClient? = null

    fun getAddonApiClient(): com.example.peak.data.addon.AddonApiClient {
        return addonApiClient ?: synchronized(this) {
            addonApiClient ?: com.example.peak.data.addon.AddonApiClient().also { addonApiClient = it }
        }
    }

    private var addonStreamApiClient: com.example.peak.data.addon.AddonStreamApiClient? = null

    fun getAddonStreamApiClient(): com.example.peak.data.addon.AddonStreamApiClient {
        return addonStreamApiClient ?: synchronized(this) {
            addonStreamApiClient ?: com.example.peak.data.addon.AddonStreamApiClient().also { addonStreamApiClient = it }
        }
    }
}
