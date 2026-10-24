package com.example.peak.data.repository

import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.TmdbExternalIds
import com.example.peak.data.remote.dto.TmdbMovie
import com.example.peak.data.remote.dto.TmdbResponse
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        private val movieDetailResult: (String) -> TmdbMovie = { throw Exception("Movie not found") },
        private val tvDetailResult: (String) -> TmdbMovie = { throw Exception("TV show not found") }
    ) : TmdbApi {
        override suspend fun getTrending(): TmdbResponse = TmdbResponse(emptyList())
        override suspend fun getTrendingTv(): TmdbResponse = TmdbResponse(emptyList())

        override suspend fun getMovieDetails(
            movieId: String,
            append: String,
            includeImageLanguage: String
        ): TmdbMovie = movieDetailResult(movieId)

        override suspend fun getTvDetails(
            tvId: String,
            append: String,
            includeImageLanguage: String
        ): TmdbMovie = tvDetailResult(tvId)

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
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getMediaById("550", MediaType.MOVIE)
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
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getMediaById("1399", MediaType.TV)
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
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getImdbId("550", MediaType.MOVIE)
        assertTrue(result.isSuccess)
        assertEquals("tt0137523", result.getOrThrow())
    }

    @Test
    fun getImdbId_missingImdbId_returnsFailureWithoutFabricatingId() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 999, externalImdbId = null, imdbId = null) }
        )
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getImdbId("999", MediaType.MOVIE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun getImdbId_blankMediaId_returnsFailure() = runTest {
        val fakeApi = FakeTmdbApi()
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getImdbId("   ", MediaType.MOVIE)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getCachedMedia_beforeEnrichment_returnsNull() = runTest {
        val fakeApi = FakeTmdbApi()
        val repository = MovieRepositoryImpl(fakeApi)

        val cached = repository.getCachedMedia("550", MediaType.MOVIE)
        org.junit.Assert.assertNull(cached)
    }

    @Test
    fun getCachedMedia_afterEnrichment_returnsEnrichedMovie() = runTest {
        val fakeApi = FakeTmdbApi(
            movieDetailResult = { createFakeTmdbMovie(id = 550, title = "Fight Club") }
        )
        val repository = MovieRepositoryImpl(fakeApi)

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
        val repository = MovieRepositoryImpl(fakeApi)

        repository.getMediaById("100", MediaType.MOVIE)
        repository.getMediaById("100", MediaType.TV)

        val cachedMovie = repository.getCachedMedia("100", MediaType.MOVIE)
        val cachedTv = repository.getCachedMedia("100", MediaType.TV)

        org.junit.Assert.assertNotNull(cachedMovie)
        org.junit.Assert.assertNotNull(cachedTv)
        assertEquals("Movie Title", cachedMovie!!.name)
        assertEquals("Series Title", cachedTv!!.name)
    }
}
