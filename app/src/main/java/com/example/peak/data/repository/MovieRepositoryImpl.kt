package com.example.peak.data.repository

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

            try {
                val response = api.getTrending()
                val movies = response.results.map { it.toMovie() }
                
                // FIX 2 & 3: Only update on success and warm detail cache
                if (movies.isNotEmpty()) {
                    trendingMoviesCache = movies
                    trendingMoviesTimestamp = System.currentTimeMillis()
                    
                    // WARM DETAIL CACHE: Pre-inject trending movies into detail cache
                    movies.forEach { movieDetailsCache[it.movieId] = it }
                }
                
                Result.success(movies)
            } catch (e: Exception) {
                // Return stale cache if available on failure, otherwise return error
                trendingMoviesCache?.let { Result.success(it) } ?: Result.failure(e)
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

            try {
                val response = api.getTrendingTv()
                val series = response.results.map { it.toMovie() }
                
                if (series.isNotEmpty()) {
                    trendingSeriesCache = series
                    trendingSeriesTimestamp = System.currentTimeMillis()
                    series.forEach { movieDetailsCache[it.movieId] = it }
                }
                
                Result.success(series)
            } catch (e: Exception) {
                trendingSeriesCache?.let { Result.success(it) } ?: Result.failure(e)
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

            try {
                val response = api.getMovieDetails(movieId)
                val movie = response.toMovie()
                
                // FIX 2: Success only update
                movieDetailsCache[movieId] = movie
                
                Result.success(movie)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }
}
