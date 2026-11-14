package com.example.peak.ui.screens.movies

import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.TvListType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoviesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeMovieRepository : MovieRepository {
        override val enrichedMediaFlow = MutableSharedFlow<Movie>()
        override fun prefetchMetadata(movies: List<Movie>) {}

        val trendingDeferred = CompletableDeferred<Result<List<Movie>>>()
        val popularMoviesDeferred = CompletableDeferred<Result<List<Movie>>>()

        val cache = mutableMapOf<String, Movie>()

        override fun getCachedMedia(id: String, type: MediaType): Movie? = cache["${type.name}_$id"]

        override suspend fun getTrendingMovies(): Result<List<Movie>> = trendingDeferred.await()
        override suspend fun getTrendingSeries(): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getMovies(type: MovieListType, genreId: String?): Result<List<Movie>> {
            return when (type) {
                MovieListType.POPULAR -> popularMoviesDeferred.await()
                else -> Result.success(emptyList())
            }
        }
        override suspend fun getSeries(type: TvListType, genreId: String?): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun searchMulti(query: String): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun discoverMedia(genreIds: String): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getMediaById(id: String, type: MediaType, requireFullDetails: Boolean): Result<Movie> {
            val movie = Movie(movieId = id, name = "Enriched $id", imageUrl = "", backdropUrl = "", mediaType = type, isEnriched = true, isFullyEnriched = requireFullDetails)
            cache["${type.name}_$id"] = movie
            return Result.success(movie)
        }
        override suspend fun getImdbId(id: String, type: MediaType): Result<String> = Result.success("tt$id")
    }

    @Test
    fun movies_featuredMedia_receivesEnrichmentUpdates() = runTest {
        val fakeRepo = FakeMovieRepository()
        val viewModel = MoviesViewModel(fakeRepo)

        val m1 = Movie(movieId = "m1", name = "Movie 1", imageUrl = "", backdropUrl = "", mediaType = MediaType.MOVIE, isEnriched = false)
        fakeRepo.trendingDeferred.complete(Result.success(listOf(m1)))
        fakeRepo.popularMoviesDeferred.complete(Result.success(emptyList()))
        testScheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.heroState.featuredMovies.size)
        assertEquals(false, viewModel.uiState.value.heroState.featuredMovies[0].isEnriched)

        // Emit enrichment event for m1
        val enrichedM1 = m1.copy(isEnriched = true, titleLogoUrl = "http://logo.png")
        fakeRepo.cache["MOVIE_m1"] = enrichedM1
        fakeRepo.enrichedMediaFlow.emit(enrichedM1)
        testScheduler.advanceUntilIdle()

        // Verify featured media updated
        assertEquals("http://logo.png", viewModel.uiState.value.heroState.featuredMovies[0].titleLogoUrl)
    }
}
