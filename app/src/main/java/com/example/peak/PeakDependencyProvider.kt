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
}
