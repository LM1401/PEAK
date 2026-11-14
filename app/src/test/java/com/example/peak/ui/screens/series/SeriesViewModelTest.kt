package com.example.peak.ui.screens.series

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
class SeriesViewModelTest {

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

        val trendingSeriesDeferred = CompletableDeferred<Result<List<Movie>>>()
        val popularSeriesDeferred = CompletableDeferred<Result<List<Movie>>>()

        var requestedType: MediaType? = null
        var requestedFullDetails: Boolean? = null

        override fun getCachedMedia(id: String, type: MediaType): Movie? = null

        override suspend fun getTrendingMovies(): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getTrendingSeries(): Result<List<Movie>> = trendingSeriesDeferred.await()
        override suspend fun getMovies(type: MovieListType, genreId: String?): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getSeries(type: TvListType, genreId: String?): Result<List<Movie>> {
            return when (type) {
                TvListType.POPULAR -> popularSeriesDeferred.await()
                else -> Result.success(emptyList())
            }
        }
        override suspend fun searchMulti(query: String): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun discoverMedia(genreIds: String): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getMediaById(id: String, type: MediaType, requireFullDetails: Boolean): Result<Movie> {
            requestedType = type
            requestedFullDetails = requireFullDetails
            val series = Movie(movieId = id, name = "Series $id", imageUrl = "", backdropUrl = "", mediaType = type, isEnriched = true, isFullyEnriched = requireFullDetails)
            return Result.success(series)
        }
        override suspend fun getImdbId(id: String, type: MediaType): Result<String> = Result.success("tt$id")
    }

    @Test
    fun series_fullDetailRequest_usesTvMediaType() = runTest {
        val fakeRepo = FakeMovieRepository()
        val viewModel = SeriesViewModel(fakeRepo)

        val s1 = Movie(movieId = "s1", name = "Series 1", imageUrl = "", backdropUrl = "", mediaType = MediaType.TV, isEnriched = false)
        fakeRepo.trendingSeriesDeferred.complete(Result.success(listOf(s1)))
        fakeRepo.popularSeriesDeferred.complete(Result.success(emptyList()))
        testScheduler.advanceUntilIdle()

        viewModel.onMovieFocused("series_trending", s1)
        testScheduler.advanceUntilIdle()

        assertEquals(MediaType.TV, fakeRepo.requestedType)
        assertEquals(true, fakeRepo.requestedFullDetails)
    }
}
