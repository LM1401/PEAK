package com.example.peak.data.repository

import com.example.peak.data.network.SafeApiCall
import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.dto.toEnrichedMovie
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [MovieRepository] with a hardened in-memory caching layer.
 */
class MovieRepositoryImpl(
    private val api: TmdbApi
) : MovieRepository {

    private val listMutex = Mutex()
    private val detailMutex = Mutex()
    
    private val movieDetailsCache = ConcurrentHashMap<String, Movie>()
    
    private var trendingMoviesCache: List<Movie>? = null
    private var trendingSeriesCache: List<Movie>? = null

    private var trendingMoviesTimestamp = 0L
    private var trendingSeriesTimestamp = 0L
    
    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 1000L 
    }

    private fun getCacheKey(id: String, type: MediaType): String = "${type.name}_$id"

    override suspend fun getTrendingMovies(): Result<List<Movie>> {
        val cached = trendingMoviesCache
        if (cached != null && !isExpired(trendingMoviesTimestamp)) {
            return Result.success(cached)
        }

        return listMutex.withLock {
            if (trendingMoviesCache != null && !isExpired(trendingMoviesTimestamp)) {
                return@withLock Result.success(trendingMoviesCache!!)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                api.getTrending()
            }

            return@withLock when {
                result.data != null -> {
                    val movies = result.data.results.map { it.toMovie(MediaType.MOVIE) }
                    trendingMoviesCache = movies
                    trendingMoviesTimestamp = System.currentTimeMillis()
                    // Update cache but don't overwrite enriched data with summary data
                    movies.forEach { movie ->
                        val key = getCacheKey(movie.movieId, movie.mediaType)
                        if (movieDetailsCache[key]?.isEnriched != true) {
                            movieDetailsCache[key] = movie
                        }
                    }
                    Result.success(movies)
                }
                trendingMoviesCache != null -> Result.success(trendingMoviesCache!!)
                else -> Result.success(emptyList())
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
                    val series = result.data.results.map { it.toMovie(MediaType.TV) }
                    trendingSeriesCache = series
                    trendingSeriesTimestamp = System.currentTimeMillis()
                    series.forEach { movie ->
                        val key = getCacheKey(movie.movieId, movie.mediaType)
                        if (movieDetailsCache[key]?.isEnriched != true) {
                            movieDetailsCache[key] = movie
                        }
                    }
                    Result.success(series)
                }
                trendingSeriesCache != null -> Result.success(trendingSeriesCache!!)
                else -> Result.success(emptyList())
            }
        }
    }

    override suspend fun getMediaById(id: String, type: MediaType): Result<Movie> {
        val cacheKey = getCacheKey(id, type)
        
        // Return if it's already enriched
        movieDetailsCache[cacheKey]?.takeIf { it.isEnriched }?.let {
            return Result.success(it)
        }

        return detailMutex.withLock {
            // Re-check after lock
            movieDetailsCache[cacheKey]?.takeIf { it.isEnriched }?.let {
                return@withLock Result.success(it)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                if (type == MediaType.MOVIE) api.getMovieDetails(id) else api.getTvDetails(id)
            }

            return@withLock when {
                result.data != null -> {
                    val movie = result.data.toEnrichedMovie(type)
                    movieDetailsCache[cacheKey] = movie
                    Result.success(movie)
                }
                else -> {
                    // Fallback to thin cached data if available
                    movieDetailsCache[cacheKey]?.let { Result.success(it) } 
                        ?: Result.failure(result.error ?: Exception("Media not found"))
                }
            }
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }
}
