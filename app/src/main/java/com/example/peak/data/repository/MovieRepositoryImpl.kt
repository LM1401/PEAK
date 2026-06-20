package com.example.peak.data.repository

import android.util.Log
import com.example.peak.data.network.SafeApiCall
import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [MovieRepository] with a hardened in-memory caching layer.
 * Optimized for Android TV performance by reducing redundant API calls and 
 * pre-warming the detail cache from trending lists.
 */
class MovieRepositoryImpl(
    private val api: TmdbApi
) : MovieRepository {

    private val mutex = Mutex()
    
    // FIX 5: Use ConcurrentHashMap to allow safe reads without locking mutex
    // Individual movie cache is long-lived to support instant detail loading (no TTL)
    private val movieDetailsCache = ConcurrentHashMap<String, Movie>()
    
    private var trendingMoviesCache: List<Movie>? = null
    private var trendingSeriesCache: List<Movie>? = null

    private var trendingMoviesTimestamp = 0L
    private var trendingSeriesTimestamp = 0L
    
    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 1000L // 2 Minutes for lists
    }

    override suspend fun getTrendingMovies(): Result<List<Movie>> {
        // FAST PATH: Read-only check outside mutex
        val cached = trendingMoviesCache
        if (cached != null && !isExpired(trendingMoviesTimestamp)) {
            return Result.success(cached)
        }

        return mutex.withLock {
            // Re-check after acquiring lock to prevent duplicate concurrent fetches
            if (trendingMoviesCache != null && !isExpired(trendingMoviesTimestamp)) {
                return@withLock Result.success(trendingMoviesCache!!)
            }

            // SAFE CALL: SSL / Network errors are non-fatal
            val result = SafeApiCall.execute("MovieRepository") {
                api.getTrending()
            }

            return@withLock when {
                result.data != null -> {
                    Log.d("MovieRepo", "NETWORK data used (Movies)")
                    val movies = result.data.results.map { it.toMovie() }
                    trendingMoviesCache = movies
                    trendingMoviesTimestamp = System.currentTimeMillis()
                    movies.forEach { movieDetailsCache[it.movieId] = it }
                    Result.success(movies)
                }
                trendingMoviesCache != null -> {
                    Log.d("MovieRepo", "CACHE fallback used (Movies)")
                    Result.success(trendingMoviesCache!!)
                }
                else -> {
                    Log.d("MovieRepo", "EMPTY fallback used (Movies)")
                    Result.success(emptyList())
                }
            }
        }
    }

    override suspend fun getTrendingSeries(): Result<List<Movie>> {
        val cached = trendingSeriesCache
        if (cached != null && !isExpired(trendingSeriesTimestamp)) {
            return Result.success(cached)
        }

        return mutex.withLock {
            if (trendingSeriesCache != null && !isExpired(trendingSeriesTimestamp)) {
                return@withLock Result.success(trendingSeriesCache!!)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                api.getTrendingTv()
            }

            return@withLock when {
                result.data != null -> {
                    Log.d("MovieRepo", "NETWORK data used (Series)")
                    val series = result.data.results.map { it.toMovie() }
                    trendingSeriesCache = series
                    trendingSeriesTimestamp = System.currentTimeMillis()
                    series.forEach { movieDetailsCache[it.movieId] = it }
                    Result.success(series)
                }
                trendingSeriesCache != null -> {
                    Log.d("MovieRepo", "CACHE fallback used (Series)")
                    Result.success(trendingSeriesCache!!)
                }
                else -> {
                    Log.d("MovieRepo", "EMPTY fallback used (Series)")
                    Result.success(emptyList())
                }
            }
        }
    }

    override suspend fun getMovieById(movieId: String): Result<Movie> {
        // FIX 1: CONSISTENT CACHE HIT - Always return immediately if exists
        movieDetailsCache[movieId]?.let {
            return Result.success(it)
        }

        return mutex.withLock {
            // Re-check after lock
            movieDetailsCache[movieId]?.let {
                return@withLock Result.success(it)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                api.getMovieDetails(movieId)
            }

            return@withLock when {
                result.data != null -> {
                    Log.d("MovieRepo", "NETWORK data used (ID: $movieId)")
                    val movie = result.data.toMovie()
                    movieDetailsCache[movieId] = movie
                    Result.success(movie)
                }
                else -> {
                    Log.d("MovieRepo", "ERROR/EMPTY fallback (ID: $movieId)")
                    Result.failure(result.error ?: Exception("Movie not found or network error"))
                }
            }
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }
}
