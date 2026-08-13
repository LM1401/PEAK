package com.example.peak.data.repository

import com.example.peak.data.network.SafeApiCall
import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.dto.toEnrichedMovie
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.TvListType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [MovieRepository] with a hardened in-memory caching layer.
 */
class MovieRepositoryImpl(
    private val api: TmdbApi
) : MovieRepository {

    private val detailMutex = Mutex()
    private val listMutexMap = ConcurrentHashMap<String, Mutex>()
    
    private val movieDetailsCache = ConcurrentHashMap<String, Movie>()
    
    // Category Caching
    private val categoryCache = ConcurrentHashMap<String, List<Movie>>()
    private val categoryTimestamps = ConcurrentHashMap<String, Long>()
    
    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 1000L // 2 Minutes
    }

    private fun getCacheKey(id: String, type: MediaType): String = "${type.name}_$id"

    private fun getListMutex(key: String): Mutex {
        return listMutexMap.getOrPut(key) { Mutex() }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }

    private fun updateDetailsCache(movies: List<Movie>) {
        movies.forEach { movie ->
            val key = getCacheKey(movie.movieId, movie.mediaType)
            if (movieDetailsCache[key]?.isEnriched != true) {
                movieDetailsCache[key] = movie
            }
        }
    }

    override suspend fun getTrendingMovies(): Result<List<Movie>> {
        return getMovies(MovieListType.TRENDING)
    }

    override suspend fun getTrendingSeries(): Result<List<Movie>> {
        return getSeries(TvListType.TRENDING)
    }

    override suspend fun getMovies(type: MovieListType, genreId: Int?): Result<List<Movie>> {
        val key = if (type == MovieListType.GENRE) "MOVIE_GENRE_$genreId" else "MOVIE_${type.name}"
        
        categoryCache[key]?.let { cached ->
            if (!isExpired(categoryTimestamps[key] ?: 0L)) return Result.success(cached)
        }

        return getListMutex(key).withLock {
            categoryCache[key]?.let { cached ->
                if (!isExpired(categoryTimestamps[key] ?: 0L)) return@withLock Result.success(cached)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                when (type) {
                    MovieListType.TRENDING -> api.getTrending()
                    MovieListType.POPULAR -> api.getPopularMovies()
                    MovieListType.NOW_PLAYING -> api.getNowPlayingMovies()
                    MovieListType.TOP_RATED -> api.getTopRatedMovies()
                    MovieListType.GENRE -> api.discoverMovies(genreId.toString())
                }
            }

            return@withLock when {
                result.data != null -> {
                    val movies = result.data.results.map { it.toMovie(MediaType.MOVIE) }
                    categoryCache[key] = movies
                    categoryTimestamps[key] = System.currentTimeMillis()
                    updateDetailsCache(movies)
                    Result.success(movies)
                }
                categoryCache[key] != null -> Result.success(categoryCache[key]!!)
                else -> Result.success(emptyList())
            }
        }
    }

    override suspend fun getSeries(type: TvListType, genreId: Int?): Result<List<Movie>> {
        val key = if (type == TvListType.GENRE) "TV_GENRE_$genreId" else "TV_${type.name}"
        
        categoryCache[key]?.let { cached ->
            if (!isExpired(categoryTimestamps[key] ?: 0L)) return Result.success(cached)
        }

        return getListMutex(key).withLock {
            categoryCache[key]?.let { cached ->
                if (!isExpired(categoryTimestamps[key] ?: 0L)) return@withLock Result.success(cached)
            }

            val result = SafeApiCall.execute("MovieRepository") {
                when (type) {
                    TvListType.TRENDING -> api.getTrendingTv()
                    TvListType.POPULAR -> api.getPopularSeries()
                    TvListType.TOP_RATED -> api.getTopRatedSeries()
                    TvListType.ON_THE_AIR -> api.getOnTheAirSeries()
                    TvListType.GENRE -> api.discoverSeries(genreId.toString())
                }
            }

            return@withLock when {
                result.data != null -> {
                    val series = result.data.results.map { it.toMovie(MediaType.TV) }
                    categoryCache[key] = series
                    categoryTimestamps[key] = System.currentTimeMillis()
                    updateDetailsCache(series)
                    Result.success(series)
                }
                categoryCache[key] != null -> Result.success(categoryCache[key]!!)
                else -> Result.success(emptyList())
            }
        }
    }

    override suspend fun getMediaById(id: String, type: MediaType): Result<Movie> {
        val cacheKey = getCacheKey(id, type)
        
        movieDetailsCache[cacheKey]?.takeIf { it.isEnriched }?.let {
            return Result.success(it)
        }

        return detailMutex.withLock {
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
                    movieDetailsCache[cacheKey]?.let { Result.success(it) } 
                        ?: Result.failure(result.error ?: Exception("Media not found"))
                }
            }
        }
    }
}
