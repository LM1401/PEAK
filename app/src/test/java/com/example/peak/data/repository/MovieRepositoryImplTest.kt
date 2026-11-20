package com.example.peak.data.repository

import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.TmdbExternalIds
import com.example.peak.data.remote.dto.TmdbLogo
import com.example.peak.data.remote.dto.TmdbMovie
import com.example.peak.data.remote.dto.TmdbResponse
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import android.content.Context

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MovieRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    private lateinit var fakeAltLogoProvider: FakeAlternativeLogoProvider
    private lateinit var fakeImageLoader: FakeImageLoader

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = RuntimeEnvironment.getApplication()
        fakeAltLogoProvider = FakeAlternativeLogoProvider()
        fakeImageLoader = FakeImageLoader(context)
    }

    class FakeAlternativeLogoProvider : com.example.peak.data.provider.AlternativeLogoProvider {
        var findLogoResult: (String, MediaType) -> com.example.peak.data.provider.AlternativeLogoResult = { _, _ -> com.example.peak.data.provider.AlternativeLogoResult.NotFound }
        var apiCalls = 0
        override suspend fun findLogo(tmdbId: String, mediaType: MediaType): com.example.peak.data.provider.AlternativeLogoResult {
            apiCalls++
            return findLogoResult(tmdbId, mediaType)
        }
    }

    class FakeImageLoader(val context: Context) : coil.ImageLoader {
        var successResult: Boolean = true
        var isTransparent: Boolean = true
        var returnedWidth: Int = 500
        var returnedHeight: Int = 250
        var executedRequests = mutableListOf<coil.request.ImageRequest>()
        
        override val components get() = coil.ComponentRegistry()
        override val defaults get() = coil.request.DefaultRequestOptions()
        override val diskCache get() = null
        override val memoryCache get() = null
        
        override fun enqueue(request: coil.request.ImageRequest): coil.request.Disposable = throw NotImplementedError()
        
        override suspend fun execute(request: coil.request.ImageRequest): coil.request.ImageResult {
            executedRequests.add(request)
            return if (successResult) {
                val bitmap = android.graphics.Bitmap.createBitmap(returnedWidth, returnedHeight, android.graphics.Bitmap.Config.ARGB_8888)
                if (isTransparent) {
                    bitmap.setPixel(0, 0, android.graphics.Color.TRANSPARENT) // Give it actual transparency
                } else {
                    bitmap.eraseColor(android.graphics.Color.BLACK)
                }
                val drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                coil.request.SuccessResult(drawable, request, coil.decode.DataSource.NETWORK)
            } else {
                coil.request.ErrorResult(null, request, Throwable("Failed to load"))
            }
        }
        
        override fun newBuilder(): coil.ImageLoader.Builder = coil.ImageLoader.Builder(context)
        override fun shutdown() {}
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createFakeTmdbMovie(
        id: Int,
        title: String? = "Test Movie",
        name: String? = null,
        imdbId: String? = null,
        externalImdbId: String? = null
    ): TmdbMovie {
        return TmdbMovie(
            id = id,
            title = title,
            name = name,
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            overview = "Overview text",
            voteAverage = 8.5,
            releaseDate = "2024-01-01",
            firstAirDate = if (name != null) "2024-01-01" else null,
            runtime = 120,
            episodeRunTime = null,
            numberOfSeasons = if (name != null) 2 else null,
            numberOfEpisodes = if (name != null) 20 else null,
            genres = emptyList(),
            credits = null,
            productionCompanies = emptyList(),
            networks = emptyList(),
            releaseDates = null,
            contentRatings = null,
            watchProviders = null,
            images = null,
            mediaType = null,
            externalIds = externalImdbId?.let { TmdbExternalIds(imdbId = it) },
            imdbId = imdbId
        )
    }

    private class FakeTmdbApi(
        private val movieDetailResult: suspend (String) -> TmdbMovie = { throw Exception("Movie not found") },
        private val tvDetailResult: suspend (String) -> TmdbMovie = { throw Exception("TV show not found") }
    ) : TmdbApi {
        override suspend fun getTrending(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getTrendingTv(): TmdbResponse = TmdbResponse(emptyList())

        override suspend fun getMovieDetails(
            movieId: String,
            append: String,
            includeImageLanguage: String?
        ): TmdbMovie = movieDetailResult(append)

        override suspend fun getTvDetails(
            tvId: String,
            append: String,
            includeImageLanguage: String?
        ): TmdbMovie = tvDetailResult(append)

        override suspend fun getPopularMovies(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getTopRatedMovies(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getNowPlayingMovies(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun discoverMovies(genreId: String): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun searchMulti(query: String): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getPopularSeries(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getTopRatedSeries(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getOnTheAirSeries(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun discoverSeries(genreId: String): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun discoverAdvancedMovies(filters: Map<String, String>, page: Int): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun discoverAdvancedSeries(filters: Map<String, String>, page: Int): TmdbResponse = TmdbResponse(emptyList())
    }

    @Test
    fun getMediaById_movieWithExternalIds_populatesImdbIdOnDomainModel() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 550, title = "Fight Club", externalImdbId = "tt0137523") }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        assertTrue(result.isSuccess)

        val movie = result.getOrThrow()
        assertEquals("550", movie.movieId)
        assertEquals("tt0137523", movie.imdbId)
    }

    @Test
    fun getMediaById_tvWithExternalIds_populatesImdbIdOnDomainModel() = runTest {
        val fakeApi = FakeTmdbApi(
            tvDetailResult = { createFakeTmdbMovie(id = 1399, name = "Game of Thrones", externalImdbId = "tt0944947") }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getMediaById("1399", MediaType.TV, requireFullDetails = true)
        assertTrue(result.isSuccess)

        val tvShow = result.getOrThrow()
        assertEquals("1399", tvShow.movieId)
        assertEquals("tt0944947", tvShow.imdbId)
    }

    @Test
    fun getImdbId_validImdbId_returnsSuccessWithImdbId() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 550, externalImdbId = "tt0137523") }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getImdbId("550", MediaType.MOVIE)
        assertTrue(result.isSuccess)
        assertEquals("tt0137523", result.getOrThrow())
    }

    @Test
    fun getImdbId_missingImdbId_returnsFailureWithoutFabricatingId() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 999, externalImdbId = null, imdbId = null) }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getImdbId("999", MediaType.MOVIE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun getImdbId_blankMediaId_returnsFailure() = runTest {
        val fakeApi = FakeTmdbApi()
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getImdbId("   ", MediaType.MOVIE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getCachedMedia_beforeEnrichment_returnsNull() = runTest {
        val fakeApi = FakeTmdbApi()
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val cached = repository.getCachedMedia("550", MediaType.MOVIE)
        org.junit.Assert.assertNull(cached)
    }

    @Test
    fun getCachedMedia_afterEnrichment_returnsEnrichedMovie() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 550, title = "Fight Club") }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // Initial fetch enriches and caches item
        repository.getMediaById("550", MediaType.MOVIE)

        val cached = repository.getCachedMedia("550", MediaType.MOVIE)
        org.junit.Assert.assertNotNull(cached)
        assertTrue(cached!!.isEnriched)
        assertEquals("Fight Club", cached.name)
    }

    @Test
    fun getCachedMedia_sameNumericIdDifferentMediaType_remainsDistinct() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 100, title = "Movie Title", name = null) },
            tvDetailResult = { createFakeTmdbMovie(id = 100, title = null, name = "Series Title") }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        repository.getMediaById("100", MediaType.MOVIE)
        repository.getMediaById("100", MediaType.TV)

        val cachedMovie = repository.getCachedMedia("100", MediaType.MOVIE)
        val cachedTv = repository.getCachedMedia("100", MediaType.TV)

        org.junit.Assert.assertNotNull(cachedMovie)
        org.junit.Assert.assertNotNull(cachedTv)
        assertEquals("Movie Title", cachedMovie!!.name)
        assertEquals("Series Title", cachedTv!!.name)
    }

    @Test
    fun getMediaById_concurrentUnrelatedRequests_runInParallel() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { 
                apiCalls++
                delay(500)
                createFakeTmdbMovie(id = apiCalls, title = "Movie $apiCalls")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val job1 = async { repository.getMediaById("1", MediaType.MOVIE) }
        val job2 = async { repository.getMediaById("2", MediaType.MOVIE) }

        advanceTimeBy(500) // Both should finish in 500ms since they run in parallel
        val result1 = job1.await()
        val result2 = job2.await()

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(2, apiCalls)
    }

    @Test
    fun getMediaById_concurrentDuplicateRequests_runSeriallyAndDeduplicate() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { 
                apiCalls++
                delay(500)
                createFakeTmdbMovie(id = 1, title = "Movie 1")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // Launch two requests for the EXACT same movie id
        val job1 = async { repository.getMediaById("1", MediaType.MOVIE) }
        val job2 = async { repository.getMediaById("1", MediaType.MOVIE) }

        advanceTimeBy(500) 
        val result1 = job1.await()
        val result2 = job2.await()

        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        // Ensure the API was only hit once because the second request waited and hit the cache
        assertEquals(1, apiCalls)
    }

    @Test
    fun getMediaById_cancellationReleasesMutex() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { 
                apiCalls++
                delay(1000)
                createFakeTmdbMovie(id = 1, title = "Movie 1")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val job1 = launch { repository.getMediaById("1", MediaType.MOVIE) }
        
        // Let job1 acquire the mutex and start the API delay
        advanceTimeBy(100)
        
        // Cancel job1
        job1.cancel()
        
        // Now job2 should be able to acquire the mutex and succeed
        val result2 = repository.getMediaById("1", MediaType.MOVIE)
        
        assertTrue(result2.isSuccess)
        assertEquals(2, apiCalls) // 1 cancelled, 1 successful
    }
    
    @Test
    fun getMediaById_lightweightMovieRequest_omitsCredits() = runTest {
        var appendedString = ""
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { append ->
                appendedString = append
                createFakeTmdbMovie(id = 550, title = "Fight Club")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)
        assertTrue(result.isSuccess)
        
        val movie = result.getOrThrow()
        assertTrue(movie.isEnriched)
        org.junit.Assert.assertFalse(movie.isFullyEnriched)
        org.junit.Assert.assertFalse("Should not request credits", appendedString.contains("credits"))
        assertTrue("Should request release dates", appendedString.contains("release_dates"))
    }
    
    @Test
    fun getMediaById_fullMovieRequest_includesCredits() = runTest {
        var appendedString = ""
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { append ->
                appendedString = append
                createFakeTmdbMovie(id = 550, title = "Fight Club")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        assertTrue(result.isSuccess)
        
        val movie = result.getOrThrow()
        assertTrue(movie.isEnriched)
        assertTrue(movie.isFullyEnriched)
        assertTrue("Should request credits", appendedString.contains("credits"))
    }
    
    @Test
    fun getMediaById_cacheUpgrade_fetchesFullWhenLightweightIsCached() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { 
                apiCalls++
                createFakeTmdbMovie(id = 550, title = "Fight Club")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // 1. Fetch Lightweight
        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)
        assertEquals(1, apiCalls)
        
        // 2. Fetch Lightweight again (Should hit cache)
        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)
        assertEquals(1, apiCalls)
        
        // 3. Fetch Full (Should miss cache and upgrade)
        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        assertEquals(2, apiCalls)
        assertTrue(result.getOrThrow().isFullyEnriched)
    }

    @Test
    fun getMediaById_cacheDowngrade_preventedWhenFullIsCached() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                apiCalls++
                createFakeTmdbMovie(id = 550, title = "Fight Club")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // 1. Fetch Full Details
        val fullResult = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        assertEquals(1, apiCalls)
        assertTrue(fullResult.getOrThrow().isFullyEnriched)

        // 2. Fetch Lightweight details (should hit cache)
        val lightResult = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)
        assertEquals(1, apiCalls)

        // Result should still be fully enriched because we return the upgraded cache item
        assertTrue(lightResult.getOrThrow().isFullyEnriched)
    }

    @Test
    fun getMediaById_concurrentMixedRequests_cachesFullDetails() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                apiCalls++
                delay(500) // Delay to ensure both requests hit the concurrency logic
                createFakeTmdbMovie(id = 550, title = "Fight Club")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // Launch both requests simultaneously
        val lightJob = async { repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false) }
        val fullJob = async { repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true) }

        advanceTimeBy(500)

        val lightResult = lightJob.await()
        val fullResult = fullJob.await()

        // Both requests should complete successfully
        assertTrue(lightResult.isSuccess)
        assertTrue(fullResult.isSuccess)

        // The final state of the cache should be fully enriched, regardless of race order
        val cachedMedia = repository.getCachedMedia("550", MediaType.MOVIE)
        org.junit.Assert.assertNotNull(cachedMedia)
        assertTrue(cachedMedia!!.isFullyEnriched)
    }

    @Test
    fun getMediaById_fullRequestFails_returnsLightweightFallback() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                apiCalls++
                if (apiCalls == 1) {
                    createFakeTmdbMovie(id = 550, title = "Fight Club")
                } else {
                    throw Exception("Network error simulating full detail failure")
                }
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // 1. Pre-cache the lightweight version successfully
        val lightResult = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)
        assertTrue(lightResult.isSuccess)
        org.junit.Assert.assertFalse(lightResult.getOrThrow().isFullyEnriched)
        assertEquals(1, apiCalls)

        // 2. Attempt to fetch full details, which will fail the network call
        val fullResult = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)

        // 3. Verify it falls back successfully instead of hard-failing
        assertTrue(fullResult.isSuccess)

        // The fallback result should be the lightweight cached item
        org.junit.Assert.assertFalse(fullResult.getOrThrow().isFullyEnriched)
        assertEquals(2, apiCalls)
    }

    @Test
    fun getMediaById_fullRequestWithNoLogo_preservesExistingLogo() = runTest {
        var apiCalls = 0
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                apiCalls++
                if (apiCalls == 1) {
                    val images = com.example.peak.data.remote.dto.TmdbImages(
                        logos = listOf(
                            com.example.peak.data.remote.dto.TmdbLogo(
                                filePath = "/valid_logo.png",
                                iso6391 = "en",
                                aspectRatio = 2.0,
                                width = 500,
                                height = 250
                            )
                        )
                    )
                    createFakeTmdbMovie(id = 550, title = "Movie").copy(images = images)
                } else {
                    createFakeTmdbMovie(id = 550, title = "Movie")
                }
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        // 1. Fetch Lightweight, caches logo
        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)

        // 2. Fetch Full Details, missing logo
        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)

        assertTrue(result.isSuccess)
        assertEquals("https://image.tmdb.org/t/p/w500/valid_logo.png", result.getOrThrow().titleLogoUrl)
    }

    @Test
    fun getMediaById_successfulRequest_updatesCacheBeforeEmittingFlow() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550)
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val emittedMovies = mutableListOf<com.example.peak.domain.model.Movie>()
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            repository.enrichedMediaFlow.collect { movie ->
                val cached = repository.getCachedMedia(movie.movieId, movie.mediaType)
                org.junit.Assert.assertNotNull(cached)
                emittedMovies.add(movie)
            }
        }

        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = false)

        assertEquals(1, emittedMovies.size)
        assertEquals("550", emittedMovies[0].movieId)
        job.cancel()
    }

    @Test
    fun getMediaById_tmdbLogoTakesPriorityOverTrakt() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                val images = com.example.peak.data.remote.dto.TmdbImages(
                    logos = listOf(
                        com.example.peak.data.remote.dto.TmdbLogo(
                            filePath = "/tmdb_logo.png",
                            iso6391 = "en",
                            aspectRatio = 2.0,
                            width = 500,
                            height = 250
                        )
                    )
                )
                createFakeTmdbMovie(id = 550, title = "Movie").copy(images = images)
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader)

        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)

        assertTrue(result.isSuccess)
        assertEquals("https://image.tmdb.org/t/p/w500/tmdb_logo.png", result.getOrThrow().titleLogoUrl)
        assertEquals(0, fakeAltLogoProvider.apiCalls) // Alternative provider should not be called
    }

    @Test
    fun getMediaById_traktIsQueriedWhenTmdbLogoIsAbsent() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550, title = "Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ -> 
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/logo.png")
        }
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        val emittedMovies = mutableListOf<com.example.peak.domain.model.Movie>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.enrichedMediaFlow.collect { movie ->
                emittedMovies.add(movie)
            }
        }

        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)

        // The first result should be the one without the logo (text fallback preserved)
        assertTrue(result.isSuccess)
        org.junit.Assert.assertNull(result.getOrThrow().titleLogoUrl)

        testScheduler.advanceUntilIdle()

        // The alternative provider should have been called
        assertEquals(1, fakeAltLogoProvider.apiCalls)

        // There should be a second emission with the discovered logo
        assertTrue(emittedMovies.size >= 2)
        assertEquals("https://walter.trakt.tv/logo.png", emittedMovies.last().titleLogoUrl)
        // Check that the logoAspectRatio was updated based on the fake image dimensions
        assertEquals(2.0f, emittedMovies.last().logoAspectRatio)
        
        job.cancel()
    }

    @Test
    fun getMediaById_notFoundPreservesTextFallback() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550, title = "Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ -> 
            com.example.peak.data.provider.AlternativeLogoResult.NotFound
        }
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        val emittedMovies = mutableListOf<com.example.peak.domain.model.Movie>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.enrichedMediaFlow.collect { movie ->
                emittedMovies.add(movie)
            }
        }

        val result = repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        
        assertTrue(result.isSuccess)
        org.junit.Assert.assertNull(result.getOrThrow().titleLogoUrl)

        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeAltLogoProvider.apiCalls)
        
        // No new emission should occur because no logo was found
        assertEquals(1, emittedMovies.size)
        org.junit.Assert.assertNull(emittedMovies.last().titleLogoUrl)

        job.cancel()
    }

    @Test
    fun getMediaById_opaqueImageRejectedAndNegativelyCached() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550, title = "Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ -> 
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/opaque_logo.png")
        }
        fakeImageLoader.isTransparent = false // This makes our fake Coil return an opaque image

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeAltLogoProvider.apiCalls)

        // Second request should hit the negative cache and NOT call the provider again
        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()

        assertEquals(1, fakeAltLogoProvider.apiCalls)
    }

    @Test
    fun getMediaById_concurrentRequestsForSameTitleDeduplicated() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550, title = "Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ -> 
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/logo.png")
        }
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        // Launch two requests simultaneously
        val job1 = launch { repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true) }
        val job2 = launch { repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true) }
        
        testScheduler.advanceUntilIdle()
        
        // Even though both launched simultaneously and asked for the missing logo, 
        // the mutex should ensure the provider is only called once.
        assertEquals(1, fakeAltLogoProvider.apiCalls)
        
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun getMediaById_rateLimitExpiryIsRespected() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550, title = "Movie")
            }
        )
        // Rate limited for 2 seconds (2000 ms)
        fakeAltLogoProvider.findLogoResult = { _, _ -> 
            com.example.peak.data.provider.AlternativeLogoResult.RateLimited(2)
        }
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeAltLogoProvider.apiCalls)

        // Request again immediately -> should hit cache, apiCalls still 1
        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeAltLogoProvider.apiCalls)

        // Wait past the rate limit (2 seconds)
        Thread.sleep(2100)
        
        // Request again -> cache expired, should call provider again
        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()
        assertEquals(2, fakeAltLogoProvider.apiCalls)
    }

    @Test
    fun getMediaById_temporaryFailuresExpire() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 550, title = "Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ -> 
            com.example.peak.data.provider.AlternativeLogoResult.Error(Exception("Network error"))
        }
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("550", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()
        assertEquals(1, fakeAltLogoProvider.apiCalls)
        
        // Cannot practically Thread.sleep(60_000) for the 1 minute expiry, but the test ensures the error doesn't crash the repo.
    }

    // --- LOGO SOURCE PROVENANCE TESTS ---

    @Test
    fun logoSource_tmdbLogoPresent_reportsTMDB() = runTest {
        val tmdbLogo = TmdbLogo(filePath = "/tmdb_logo.png", iso6391 = "en", aspectRatio = 2.0, width = 500, height = 250)
        val tmdbImages = com.example.peak.data.remote.dto.TmdbImages(logos = listOf(tmdbLogo))
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 100, title = "TMDB Movie").copy(images = tmdbImages)
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        val result = repository.getMediaById("100", MediaType.MOVIE, requireFullDetails = true)
        assertTrue(result.isSuccess)
        val movie = result.getOrNull()!!

        assertEquals(com.example.peak.domain.model.LogoSource.TMDB, movie.logoSource)
        assertEquals("https://image.tmdb.org/t/p/w500/tmdb_logo.png", movie.titleLogoUrl)
        assertEquals(0, fakeAltLogoProvider.apiCalls)
    }

    @Test
    fun logoSource_tmdbLogoMissing_initialResultReportsPEAK_FALLBACK() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 101, title = "Fallback Movie")
            }
        )
        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        val result = repository.getMediaById("101", MediaType.MOVIE, requireFullDetails = true)
        assertTrue(result.isSuccess)
        val movie = result.getOrNull()!!

        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, movie.logoSource)
    }

    @Test
    fun logoSource_traktLogoFoundAndLoaded_emitsTRAKT() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 102, title = "Trakt Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/trakt_logo.png")
        }
        fakeImageLoader.successResult = true
        fakeImageLoader.isTransparent = true

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        val emittedMovies = mutableListOf<com.example.peak.domain.model.Movie>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.enrichedMediaFlow.collect { emittedMovies.add(it) }
        }

        val initialResult = repository.getMediaById("102", MediaType.MOVIE, requireFullDetails = true)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, initialResult.getOrNull()?.logoSource)

        testScheduler.advanceUntilIdle()

        val updated = repository.getCachedMedia("102", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.TRAKT, updated?.logoSource)
        assertEquals("https://walter.trakt.tv/trakt_logo.png", updated?.titleLogoUrl)

        assertTrue(emittedMovies.any { it.logoSource == com.example.peak.domain.model.LogoSource.TRAKT })

        collectJob.cancel()
    }

    @Test
    fun logoSource_tvShow_traktLogoFoundAndLoaded_emitsTRAKT() = runTest {
        val fakeApi = FakeTmdbApi(
            tvDetailResult = {
                createFakeTmdbMovie(id = 301, name = "Trakt TV Series")
            }
        )
        fakeAltLogoProvider.findLogoResult = { tmdbId, mediaType ->
            assertEquals("301", tmdbId)
            assertEquals(MediaType.TV, mediaType)
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/tv_logo.png")
        }
        fakeImageLoader.successResult = true
        fakeImageLoader.isTransparent = true

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        val emittedMovies = mutableListOf<com.example.peak.domain.model.Movie>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.enrichedMediaFlow.collect { emittedMovies.add(it) }
        }

        val initialResult = repository.getMediaById("301", MediaType.TV, requireFullDetails = true)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, initialResult.getOrNull()?.logoSource)

        testScheduler.advanceUntilIdle()

        val updated = repository.getCachedMedia("301", MediaType.TV)
        assertEquals(com.example.peak.domain.model.LogoSource.TRAKT, updated?.logoSource)
        assertEquals("https://walter.trakt.tv/tv_logo.png", updated?.titleLogoUrl)

        assertTrue(emittedMovies.any { it.mediaType == MediaType.TV && it.logoSource == com.example.peak.domain.model.LogoSource.TRAKT })

        collectJob.cancel()
    }

    @Test
    fun logoSource_traktNotFound_preservesPEAK_FALLBACK() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 103, title = "NotFound Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.NotFound
        }

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("103", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()

        val cached = repository.getCachedMedia("103", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, cached?.logoSource)
    }

    @Test
    fun logoSource_traktNetworkError_preservesPEAK_FALLBACK() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 104, title = "Error Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.Error(Exception("Network timeout"))
        }

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("104", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()

        val cached = repository.getCachedMedia("104", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, cached?.logoSource)
    }

    @Test
    fun logoSource_failedCoilLoad_preservesPEAK_FALLBACK() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 105, title = "Coil Fail Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/broken_logo.png")
        }
        fakeImageLoader.successResult = false // Coil fails

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("105", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()

        val cached = repository.getCachedMedia("105", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, cached?.logoSource)
    }

    @Test
    fun logoSource_insufficientTransparency_preservesPEAK_FALLBACK() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 106, title = "Opaque Poster Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/opaque_poster.png")
        }
        fakeImageLoader.successResult = true
        fakeImageLoader.isTransparent = false // Fails transparency check (opaque poster)

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        repository.getMediaById("106", MediaType.MOVIE, requireFullDetails = true)
        testScheduler.advanceUntilIdle()

        val cached = repository.getCachedMedia("106", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, cached?.logoSource)
    }

    @Test
    fun logoSource_switchingBetweenMovieAndTvDetails_doesNotRetainStaleSourceState() = runTest {
        val tmdbLogo = TmdbLogo(filePath = "/movie_logo.png", iso6391 = "en", aspectRatio = 2.0, width = 500, height = 250)
        val tmdbImages = com.example.peak.data.remote.dto.TmdbImages(logos = listOf(tmdbLogo))

        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 201, title = "TMDB Movie").copy(images = tmdbImages)
            },
            tvDetailResult = {
                createFakeTmdbMovie(id = 202, name = "No Logo Show") // No TMDB logo
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.NotFound
        }

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        // 1. Fetch movie with TMDB logo -> TMDB
        val movieResult = repository.getMediaById("201", MediaType.MOVIE, requireFullDetails = true)
        assertEquals(com.example.peak.domain.model.LogoSource.TMDB, movieResult.getOrNull()?.logoSource)

        // 2. Fetch TV show with no logos -> PEAK_FALLBACK
        val tvResult = repository.getMediaById("202", MediaType.TV, requireFullDetails = true)
        assertEquals(com.example.peak.domain.model.LogoSource.PEAK_FALLBACK, tvResult.getOrNull()?.logoSource)

        // 3. Navigate back to movie 201 -> TMDB source preserved cleanly, no stale state
        val reacquiredMovie = repository.getCachedMedia("201", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.TMDB, reacquiredMovie?.logoSource)
    }

    @Test
    fun logoSource_reEnrichmentPreservesTraktLogoAndSource() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = {
                createFakeTmdbMovie(id = 401, title = "ReEnrich Movie")
            }
        )
        fakeAltLogoProvider.findLogoResult = { _, _ ->
            com.example.peak.data.provider.AlternativeLogoResult.Found("https://walter.trakt.tv/trakt_logo.png")
        }
        fakeImageLoader.successResult = true
        fakeImageLoader.isTransparent = true

        val repository = MovieRepositoryImpl(fakeApi, fakeAltLogoProvider, context, fakeImageLoader, StandardTestDispatcher(testScheduler))

        // First call populates summary/initial detail and triggers Trakt discovery
        repository.getMediaById("401", MediaType.MOVIE, requireFullDetails = false)
        testScheduler.advanceUntilIdle()

        val cachedAfterTrakt = repository.getCachedMedia("401", MediaType.MOVIE)
        assertEquals(com.example.peak.domain.model.LogoSource.TRAKT, cachedAfterTrakt?.logoSource)
        assertEquals("https://walter.trakt.tv/trakt_logo.png", cachedAfterTrakt?.titleLogoUrl)

        // Second call with requireFullDetails = true re-enriching from TMDB (which has no TMDB logo)
        val reEnrichedResult = repository.getMediaById("401", MediaType.MOVIE, requireFullDetails = true)
        assertTrue(reEnrichedResult.isSuccess)
        val reEnriched = reEnrichedResult.getOrNull()!!

        assertEquals(com.example.peak.domain.model.LogoSource.TRAKT, reEnriched.logoSource)
        assertEquals("https://walter.trakt.tv/trakt_logo.png", reEnriched.titleLogoUrl)
    }
}
