package com.example.peak.data.repository

import android.util.Log
import com.example.peak.data.network.SafeApiCall
import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.provider.AlternativeLogoProvider
import com.example.peak.data.provider.AlternativeLogoResult
import com.example.peak.data.provider.AltLogoCacheEntry
import com.example.peak.data.provider.AlternativeLogoKey
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.data.remote.dto.toEnrichedMovie
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.TvListType
import com.example.peak.ui.image.HeroLogoTrace
import com.example.peak.ui.image.hasActualTransparency
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [MovieRepository] with a hardened in-memory caching layer.
 */
class MovieRepositoryImpl(
    private val api: TmdbApi,
    private val alternativeLogoProvider: com.example.peak.data.provider.AlternativeLogoProvider,
    private val context: android.content.Context,
    private val imageLoader: coil.ImageLoader
) : MovieRepository {

    private val detailMutexMap = ConcurrentHashMap<String, Mutex>()
    private val listMutexMap = ConcurrentHashMap<String, Mutex>()
    
    private val movieDetailsCache = ConcurrentHashMap<String, Movie>()
    
    // Category Caching
    private val categoryCache = ConcurrentHashMap<String, List<Movie>>()
    private val categoryTimestamps = ConcurrentHashMap<String, Long>()
    
    // Alternative Logo Caching
    private val altLogoCache = ConcurrentHashMap<String, com.example.peak.data.provider.AltLogoCacheEntry>()
    private val altLogoMutexMap = ConcurrentHashMap<String, Mutex>()
    
    private val _enrichedMediaFlow = MutableSharedFlow<Movie>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val enrichedMediaFlow: SharedFlow<Movie> = _enrichedMediaFlow.asSharedFlow()

    private val inFlightPrefetch = ConcurrentHashMap<String, String>()

    private val prefetchChannel = Channel<PrefetchTask>(
        capacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ) { task ->
        inFlightPrefetch.remove(task.cacheKey, task.token)
    }

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        repeat(2) {
            repoScope.launch {
                for (task in prefetchChannel) {
                    try {
                        if (movieDetailsCache[task.cacheKey]?.isEnriched != true) {
                            getMediaById(task.movie.movieId, task.movie.mediaType, requireFullDetails = false)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Suppress background errors
                    } finally {
                        inFlightPrefetch.remove(task.cacheKey, task.token)
                    }
                }
            }
        }
    }

    private data class PrefetchTask(val movie: Movie, val cacheKey: String, val token: String)

    override fun prefetchMetadata(movies: List<Movie>) {
        movies.forEach { movie ->
            val cacheKey = getCacheKey(movie.movieId, movie.mediaType)
            if (movieDetailsCache[cacheKey]?.isEnriched == true) return@forEach

            val token = UUID.randomUUID().toString()
            if (inFlightPrefetch.putIfAbsent(cacheKey, token) == null) {
                val task = PrefetchTask(movie, cacheKey, token)
                if (!prefetchChannel.trySend(task).isSuccess) {
                    inFlightPrefetch.remove(cacheKey, token)
                }
            }
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 1000L // 2 Minutes
    }

    private fun getCacheKey(id: String, type: MediaType): String = AlternativeLogoKey(type, id).value

    private fun getListMutex(key: String): Mutex {
        return listMutexMap.getOrPut(key) { Mutex() }
    }

    private fun getDetailMutex(key: String): Mutex {
        return detailMutexMap.getOrPut(key) { Mutex() }
    }

    private fun isExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }

    private fun isAltLogoExpired(entry: com.example.peak.data.provider.AltLogoCacheEntry): Boolean {
        val expirationTime = entry.timestamp + when (entry.result) {
            is com.example.peak.data.provider.AlternativeLogoResult.Found -> 24 * 60 * 60 * 1000L // 24 hours
            is com.example.peak.data.provider.AlternativeLogoResult.NotFound -> 2 * 60 * 60 * 1000L // 2 hours
            is com.example.peak.data.provider.AlternativeLogoResult.RateLimited -> {
                val retry = entry.result.retryAfterSeconds?.toLong()?.times(1000L) ?: (5 * 60 * 1000L)
                retry
            }
            is com.example.peak.data.provider.AlternativeLogoResult.Error -> 1 * 60 * 1000L // 1 minute
        }
        return System.currentTimeMillis() > expirationTime
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

    override suspend fun searchMulti(query: String): Result<List<Movie>> {
        val result = SafeApiCall.execute("MovieRepository") {
            api.searchMulti(query)
        }
        return when {
            result.data != null -> {
                val movies = result.data.results.mapNotNull { it.toMovie() }
                updateDetailsCache(movies)
                Result.success(movies)
            }
            else -> Result.success(emptyList())
        }
    }

    override suspend fun discoverMedia(genreIds: String): Result<List<Movie>> {
        val key = "DISCOVER_$genreIds"
        
        categoryCache[key]?.let { cached ->
            if (!isExpired(categoryTimestamps[key] ?: 0L)) return Result.success(cached)
        }

        return getListMutex(key).withLock {
            categoryCache[key]?.let { cached ->
                if (!isExpired(categoryTimestamps[key] ?: 0L)) return@withLock Result.success(cached)
            }

            // Execute movie and tv discovery concurrently
            return coroutineScope {
                val moviesDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    SafeApiCall.execute("MovieRepository") { api.discoverMovies(genreIds) }
                }
                val tvDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                    SafeApiCall.execute("MovieRepository") { api.discoverSeries(genreIds) }
                }

                val moviesResult = moviesDeferred.await()
                val tvResult = tvDeferred.await()

                val allItems = mutableListOf<Movie>()
                moviesResult.data?.results?.mapNotNull { it.toMovie(MediaType.MOVIE) }?.let { movies -> allItems.addAll(movies) }
                tvResult.data?.results?.mapNotNull { it.toMovie(MediaType.TV) }?.let { tvs -> allItems.addAll(tvs) }

                // Sort combined results by rating descending to ensure the highest-quality content rises to the top
                val sorted = allItems.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }

                categoryCache[key] = sorted
                categoryTimestamps[key] = System.currentTimeMillis()
                updateDetailsCache(sorted)

                Result.success(sorted)
            }
        }
    }

    override suspend fun getMovies(type: MovieListType, genreId: String?): Result<List<Movie>> {
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
                    MovieListType.GENRE -> api.discoverMovies(genreId ?: "")
                }
            }

            return@withLock when {
                result.data != null -> {
                    val movies = result.data.results.mapNotNull { it.toMovie(MediaType.MOVIE) }
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

    override suspend fun getSeries(type: TvListType, genreId: String?): Result<List<Movie>> {
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
                    TvListType.GENRE -> api.discoverSeries(genreId ?: "")
                }
            }

            return@withLock when {
                result.data != null -> {
                    val series = result.data.results.mapNotNull { it.toMovie(MediaType.TV) }
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

    private fun triggerAlternativeLogoDiscovery(movie: Movie, cacheKey: String, id: String, type: MediaType) {
        if (movie.titleLogoUrl.isNullOrBlank()) {
            repoScope.launch {
                val altLogoMutex = altLogoMutexMap.getOrPut(cacheKey) { Mutex() }
                altLogoMutex.withLock {
                    val cachedState = altLogoCache[cacheKey]
                    if (cachedState != null && !isAltLogoExpired(cachedState)) {
                        return@withLock
                    }
                    
                    val latestMovie = movieDetailsCache[cacheKey]
                    if (latestMovie != null && !latestMovie.titleLogoUrl.isNullOrBlank()) {
                        return@withLock
                    }
                    
                    val altResult = alternativeLogoProvider.findLogo(id, type)
                    
                    if (altResult is com.example.peak.data.provider.AlternativeLogoResult.Found) {
                        val altUrl = altResult.url
                        val transformation = com.example.peak.ui.image.AutoTrimTransparentBordersTransformation()
                        val logoMemoryKey = com.example.peak.ui.image.PeakImageLoader.getLogoMemoryCacheKey(altUrl, transformation.cacheKey)

                        val request = coil.request.ImageRequest.Builder(context)
                            .data(altUrl)
                            .memoryCacheKey(logoMemoryKey)
                            .diskCacheKey(altUrl)
                            .transformations(transformation)
                            .allowHardware(true)
                            .build()

                        val coilResult = imageLoader.execute(request)

                        if (coilResult is coil.request.SuccessResult) {
                            val drawable = coilResult.drawable
                            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap

                            if (bitmap != null && bitmap.hasActualTransparency()) {
                                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

                                getDetailMutex(cacheKey).withLock {
                                    val latest = movieDetailsCache[cacheKey] ?: return@withLock
                                    if (latest.titleLogoUrl.isNullOrBlank()) {
                                        val updatedMovie = latest.copy(
                                            titleLogoUrl = altUrl,
                                            logoAspectRatio = aspectRatio
                                        )
                                        movieDetailsCache[cacheKey] = updatedMovie
                                        _enrichedMediaFlow.tryEmit(updatedMovie)
                                    }
                                }
                            } else {
                                altLogoCache[cacheKey] = com.example.peak.data.provider.AltLogoCacheEntry(com.example.peak.data.provider.AlternativeLogoResult.NotFound)
                            }
                        } else {
                            altLogoCache[cacheKey] = com.example.peak.data.provider.AltLogoCacheEntry(com.example.peak.data.provider.AlternativeLogoResult.Error(Exception("Coil load failed")))
                        }
                    } else {
                        altLogoCache[cacheKey] = com.example.peak.data.provider.AltLogoCacheEntry(altResult)
                    }
                }
            }
        }
    }

    override fun getCachedMedia(id: String, type: MediaType): Movie? {
        val cacheKey = getCacheKey(id, type)
        return movieDetailsCache[cacheKey]?.takeIf { it.isEnriched }
    }

    override suspend fun getMediaById(id: String, type: MediaType, requireFullDetails: Boolean): Result<Movie> {
        val cacheKey = getCacheKey(id, type)
        getCachedMedia(id, type)?.let {
            if (!requireFullDetails || it.isFullyEnriched) {
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.DETAIL_CACHE_HIT,
                    mediaType = type,
                    mediaId = id
                )
                Log.e("PEAK_DIAGNOSTIC", "Returning cached enriched movie for $id (requestedFull=$requireFullDetails, isFull=${it.isFullyEnriched})")
                triggerAlternativeLogoDiscovery(it, cacheKey, id, type)
                return Result.success(it)
            }
        }

        return getDetailMutex(cacheKey).withLock {
            getCachedMedia(id, type)?.let {
                if (!requireFullDetails || it.isFullyEnriched) {
                    HeroLogoTrace.record(
                        event = HeroLogoTrace.Event.DETAIL_CACHE_HIT,
                        mediaType = type,
                        mediaId = id
                    )
                    Log.e("PEAK_DIAGNOSTIC", "Returning cached enriched movie from double-check lock for $id")
                    triggerAlternativeLogoDiscovery(it, cacheKey, id, type)
                    return@withLock Result.success(it)
                }
            }

            val appendStr = if (requireFullDetails) {
                if (type == MediaType.MOVIE) "credits,release_dates,watch/providers,images,external_ids" 
                else "credits,content_ratings,watch/providers,images,external_ids"
            } else {
                if (type == MediaType.MOVIE) "release_dates,watch/providers,images" 
                else "content_ratings,watch/providers,images"
            }

            HeroLogoTrace.record(
                event = HeroLogoTrace.Event.DETAIL_NETWORK_STARTED,
                mediaType = type,
                mediaId = id
            )
            Log.e("PEAK_DIAGNOSTIC", "Fetching detail data for $id from API (requireFullDetails=$requireFullDetails)")
            val result = try {
                SafeApiCall.execute("MovieRepository") {
                    if (type == MediaType.MOVIE) api.getMovieDetails(id, appendStr) else api.getTvDetails(id, appendStr)
                }
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.DETAIL_REQUEST_CANCELLED,
                    mediaType = type,
                    mediaId = id,
                    outcome = HeroLogoTrace.Outcome.CANCELLED
                )
                throw cancellation
            } catch (exception: Exception) {
                HeroLogoTrace.record(
                    event = HeroLogoTrace.Event.DETAIL_REQUEST_FAILED,
                    mediaType = type,
                    mediaId = id,
                    outcome = HeroLogoTrace.Outcome.FAILED
                )
                throw exception
            }
            HeroLogoTrace.record(
                event = if (result.data != null) {
                    HeroLogoTrace.Event.DETAIL_RESPONSE_RECEIVED
                } else {
                    HeroLogoTrace.Event.DETAIL_REQUEST_FAILED
                },
                mediaType = type,
                mediaId = id,
                outcome = if (result.data != null) null else HeroLogoTrace.Outcome.FAILED
            )

            return@withLock when {
                result.data != null -> {
                    Log.e("PEAK_DIAGNOSTIC", "Successfully fetched detail for $id. Mapping to enriched.")
                    val newEnriched = result.data.toEnrichedMovie(type)
                    val existing = movieDetailsCache[cacheKey]

                    val preservedLogoUrl = if (existing != null && newEnriched.titleLogoUrl.isNullOrBlank() && !existing.titleLogoUrl.isNullOrBlank()) {
                        existing.titleLogoUrl
                    } else {
                        newEnriched.titleLogoUrl
                    }

                    val movie = newEnriched.copy(
                        titleLogoUrl = preservedLogoUrl,
                        isFullyEnriched = requireFullDetails || (existing?.isFullyEnriched == true)
                    )

                    movieDetailsCache[cacheKey] = movie
                    _enrichedMediaFlow.tryEmit(movie)

                    triggerAlternativeLogoDiscovery(movie, cacheKey, id, type)

                    Result.success(movie)
                }
                else -> {
                    Log.e("PEAK_DIAGNOSTIC", "Failed to fetch detail for $id: ${result.error?.message}")
                    movieDetailsCache[cacheKey]?.let { Result.success(it) } 
                        ?: Result.failure(result.error ?: Exception("Media not found"))
                }
            }
        }
    }

    override suspend fun getImdbId(id: String, type: MediaType): Result<String> {
        val trimmedId = id.trim()
        if (trimmedId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Media ID cannot be blank"))
        }

        val mediaResult = getMediaById(trimmedId, type)
        return mediaResult.fold(
            onSuccess = { movie ->
                val imdb = movie.imdbId?.trim()
                if (!imdb.isNullOrEmpty()) {
                    Result.success(imdb)
                } else {
                    Result.failure(NoSuchElementException("No valid IMDb ID found for media ID $trimmedId"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
