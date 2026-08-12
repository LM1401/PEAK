package com.example.peak.data.repository

import com.example.peak.data.network.SafeApiCall
import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import android.util.Log
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

    private val listMutex = Mutex()
    private val detailMutex = Mutex()
    
    // Composite cache key: "TYPE_ID" (e.g. "MOVIE_123" or "TV_123")
    private val movieDetailsCache = ConcurrentHashMap<String, Movie>()
    
    private var trendingMoviesCache: List<Movie>? = null
    private var trendingSeriesCache: List<Movie>? = null

    private var trendingMoviesTimestamp = 0L
    private var trendingSeriesTimestamp = 0L
    
    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 1000L // 2 Minutes for lists
    }

    private fun getCacheKey(id: String, type: MediaType): String = "${type.name}_$id"

    override suspend fun getTrendingMovies(): Result<List<Movie>> {
        // FAST PATH: Read-only check outside mutex
        val cached = trendingMoviesCache
        if (cached != null && !isExpired(trendingMoviesTimestamp)) {
            return Result.success(cached)
        }

        return listMutex.withLock {
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
                    val movies = result.data.results.map { it.toMovie(MediaType.MOVIE) }
                    trendingMoviesCache = movies
                    trendingMoviesTimestamp = System.currentTimeMillis()
                    movies.forEach { movieDetailsCache[getCacheKey(it.movieId, it.mediaType)] = it }
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

        return listMutex.withLock {
            if (trendingSeriesCache != null && !isExpired(trendingSeriesTimestamp)) {
                return@withLock Result.success(trendingSeriesCache!!)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                api.getTrendingTv()
            }

            return@withLock when {
                result.data != null -> {
                    Log.d("MovieRepo", "NETWORK data used (Series)")
                    val series = result.data.results.map { it.toMovie(MediaType.TV) }
                    trendingSeriesCache = series
                    trendingSeriesTimestamp = System.currentTimeMillis()
                    series.forEach { movieDetailsCache[getCacheKey(it.movieId, it.mediaType)] = it }
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

    override suspend fun getMediaById(id: String, type: MediaType): Result<Movie> {
        val cacheKey = getCacheKey(id, type)
        
        // FIX 1: CONSISTENT CACHE HIT - Always return immediately if exists
        movieDetailsCache[cacheKey]?.let {
            return Result.success(it)
        }

        return detailMutex.withLock {
            // Re-check after lock
            movieDetailsCache[cacheKey]?.let {
                return@withLock Result.success(it)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                if (type == MediaType.MOVIE) api.getMovieDetails(id) else api.getTvDetails(id)
            }

            return@withLock when {
                result.data != null -> {
                    Log.d("MovieRepo", "NETWORK data used ($type ID: $id)")
                    val movie = result.data.toMovie(type)
                    movieDetailsCache[cacheKey] = movie
                    Result.success(movie)
                }
                else -> {
                    Log.d("MovieRepo", "ERROR/EMPTY fallback ($type ID: $id)")
                    Result.failure(result.error ?: Exception("Media not found or network error"))
                }
            }
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }
}
