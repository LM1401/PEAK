package com.example.peak.ui.screens.home

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.peak.data.continuewatching.ContinueWatchingStorage
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.TvListType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFocusTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, String>()
        override fun getAll(): MutableMap<String, *> = HashMap(map)
        override fun getString(key: String?, defValue: String?): String? = map[key] ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = defValue
        override fun getLong(key: String?, defValue: Long): Long = defValue
        override fun getFloat(key: String?, defValue: Float): Float = defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(map)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private class FakeEditor(private val map: MutableMap<String, String>) : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) map[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor { map.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { map.clear(); return this }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }

    private class FakeContext : ContextWrapper(null) {
        private val prefs = FakeSharedPreferences()
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private inner class FakeMovieRepository : MovieRepository {
        val trendingDeferred = CompletableDeferred<Result<List<Movie>>>()
        val popularMoviesDeferred = CompletableDeferred<Result<List<Movie>>>()
        val popularSeriesDeferred = CompletableDeferred<Result<List<Movie>>>()
        val topRatedMoviesDeferred = CompletableDeferred<Result<List<Movie>>>()

        override suspend fun getTrendingMovies(): Result<List<Movie>> = trendingDeferred.await()
        override suspend fun getTrendingSeries(): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getMovies(type: MovieListType, genreId: Int?): Result<List<Movie>> {
            return when (type) {
                MovieListType.POPULAR -> popularMoviesDeferred.await()
                MovieListType.TOP_RATED -> topRatedMoviesDeferred.await()
                else -> Result.success(emptyList())
            }
        }
        override suspend fun getSeries(type: TvListType, genreId: Int?): Result<List<Movie>> {
            return when (type) {
                TvListType.POPULAR -> popularSeriesDeferred.await()
                else -> Result.success(emptyList())
            }
        }
        override suspend fun getMediaById(id: String, type: MediaType): Result<Movie> {
            val movie = createMovie(id, "Enriched $id", type)
            return Result.success(movie)
        }
    }

    private fun createMovie(id: String, name: String, type: MediaType = MediaType.MOVIE): Movie {
        return Movie(
            movieId = id,
            name = name,
            imageUrl = "http://example.com/$id.jpg",
            backdropUrl = "http://example.com/${id}_bg.jpg",
            description = "Description",
            mediaType = type,
            isEnriched = true
        )
    }

    @Test
    fun test1_secondaryRowLoadsFirst_thenHigherPriorityRow0Arrives_focusMovesToRow0() = runTest {
        val fakeRepo = FakeMovieRepository()
        val fakeContext = FakeContext()
        val cwStorage = ContinueWatchingStorage(fakeContext)
        val cwRepo = ContinueWatchingRepository(cwStorage)

        val viewModel = HomeViewModel(fakeRepo, cwRepo)
        testScheduler.advanceUntilIdle()

        // 1. Popular Series (secondary row) completes first
        val seriesList = listOf(createMovie("s1", "Popular Series 1", MediaType.TV))
        fakeRepo.popularSeriesDeferred.complete(Result.success(seriesList))
        testScheduler.advanceUntilIdle()

        // Initial focus lands on popular_series
        assertEquals("popular_series", viewModel.focusState.value?.rowId)
        assertEquals("s1", viewModel.focusState.value?.movieId)

        // Automatic focus callback fires during composition
        viewModel.onMovieFocused("popular_series", "s1", MediaType.TV)
        testScheduler.advanceUntilIdle()

        // 2. Higher-priority Trending row completes second
        val trendingList = listOf(createMovie("t1", "Trending 1", MediaType.MOVIE))
        fakeRepo.trendingDeferred.complete(Result.success(trendingList))
        testScheduler.advanceUntilIdle()

        // Verify focus automatically moves to Row 0 (trending)
        assertEquals("trending", viewModel.focusState.value?.rowId)
        assertEquals("t1", viewModel.focusState.value?.movieId)
    }

    @Test
    fun test2_secondaryRowLoadsFirst_userPressesDpadBeforeRow0Arrives_userSelectionPreserved() = runTest {
        val fakeRepo = FakeMovieRepository()
        val fakeContext = FakeContext()
        val cwStorage = ContinueWatchingStorage(fakeContext)
        val cwRepo = ContinueWatchingRepository(cwStorage)

        val viewModel = HomeViewModel(fakeRepo, cwRepo)
        testScheduler.advanceUntilIdle()

        // 1. Popular Series completes first
        val seriesList = listOf(createMovie("s1", "Popular Series 1", MediaType.TV))
        fakeRepo.popularSeriesDeferred.complete(Result.success(seriesList))
        testScheduler.advanceUntilIdle()

        assertEquals("popular_series", viewModel.focusState.value?.rowId)

        // 2. User genuinely interacts (D-pad movement / selection)
        viewModel.onUserNavigate()

        // 3. Trending arrives later
        val trendingList = listOf(createMovie("t1", "Trending 1", MediaType.MOVIE))
        fakeRepo.trendingDeferred.complete(Result.success(trendingList))
        testScheduler.advanceUntilIdle()

        // Verify focus stays on user's chosen row (popular_series) and does NOT move
        assertEquals("popular_series", viewModel.focusState.value?.rowId)
        assertEquals("s1", viewModel.focusState.value?.movieId)
    }

    @Test
    fun test3_freshLaunch_withAllRowsLoadingNormally_focusEndsOnHighestPriorityRow() = runTest {
        val fakeRepo = FakeMovieRepository()
        val fakeContext = FakeContext()
        val cwStorage = ContinueWatchingStorage(fakeContext)
        val cwRepo = ContinueWatchingRepository(cwStorage)

        val viewModel = HomeViewModel(fakeRepo, cwRepo)

        val trendingList = listOf(createMovie("t1", "Trending 1"))
        val popularList = listOf(createMovie("p1", "Popular 1"))
        fakeRepo.trendingDeferred.complete(Result.success(trendingList))
        fakeRepo.popularMoviesDeferred.complete(Result.success(popularList))
        fakeRepo.popularSeriesDeferred.complete(Result.success(emptyList()))
        fakeRepo.topRatedMoviesDeferred.complete(Result.success(emptyList()))

        testScheduler.advanceUntilIdle()

        // First row is trending
        assertEquals("trending", viewModel.focusState.value?.rowId)
        assertEquals("t1", viewModel.focusState.value?.movieId)
    }

    @Test
    fun test4_detailScreenToBackRestoration_focusPreservation() = runTest {
        val fakeRepo = FakeMovieRepository()
        val fakeContext = FakeContext()
        val cwStorage = ContinueWatchingStorage(fakeContext)
        val cwRepo = ContinueWatchingRepository(cwStorage)

        val viewModel = HomeViewModel(fakeRepo, cwRepo)
        val trendingList = listOf(createMovie("t1", "Trending 1"))
        val popularList = listOf(createMovie("p1", "Popular 1"), createMovie("p2", "Popular 2"))
        fakeRepo.trendingDeferred.complete(Result.success(trendingList))
        fakeRepo.popularMoviesDeferred.complete(Result.success(popularList))
        fakeRepo.popularSeriesDeferred.complete(Result.success(emptyList()))
        fakeRepo.topRatedMoviesDeferred.complete(Result.success(emptyList()))

        testScheduler.advanceUntilIdle()

        // User navigates down to popular_movies and selects p2
        viewModel.onUserNavigate()
        viewModel.onMovieFocused("popular_movies", "p2", MediaType.MOVIE)
        testScheduler.advanceUntilIdle()

        assertEquals("popular_movies", viewModel.focusState.value?.rowId)
        assertEquals("p2", viewModel.focusState.value?.movieId)

        // When returning from DetailScreen, the ViewModel's focusState retains p2
        assertEquals("popular_movies", viewModel.focusState.value?.rowId)
        assertEquals("p2", viewModel.focusState.value?.movieId)
    }

    @Test
    fun test5_normalDpadNavigationAfterStartup_remainsUnchanged() = runTest {
        val fakeRepo = FakeMovieRepository()
        val fakeContext = FakeContext()
        val cwStorage = ContinueWatchingStorage(fakeContext)
        val cwRepo = ContinueWatchingRepository(cwStorage)

        val viewModel = HomeViewModel(fakeRepo, cwRepo)
        val trendingList = listOf(createMovie("t1", "Trending 1"))
        fakeRepo.trendingDeferred.complete(Result.success(trendingList))
        fakeRepo.popularMoviesDeferred.complete(Result.success(emptyList()))
        fakeRepo.popularSeriesDeferred.complete(Result.success(emptyList()))
        fakeRepo.topRatedMoviesDeferred.complete(Result.success(emptyList()))

        testScheduler.advanceUntilIdle()

        assertEquals("trending", viewModel.focusState.value?.rowId)

        // User moves focus manually
        viewModel.onUserNavigate()
        viewModel.onMovieFocused("trending", "t1", MediaType.MOVIE)

        assertEquals("trending", viewModel.focusState.value?.rowId)
        assertEquals("t1", viewModel.focusState.value?.movieId)
    }

    @Test
    fun test6_horizontalNavigationWithinRow_preservedOnDataUpdate() = runTest {
        val fakeRepo = FakeMovieRepository()
        val fakeContext = FakeContext()
        val cwStorage = ContinueWatchingStorage(fakeContext)
        val cwRepo = ContinueWatchingRepository(cwStorage)

        val viewModel = HomeViewModel(fakeRepo, cwRepo)
        val trendingList = listOf(createMovie("t1", "Trending 1"), createMovie("t2", "Trending 2"), createMovie("t3", "Trending 3"))
        fakeRepo.trendingDeferred.complete(Result.success(trendingList))
        fakeRepo.popularMoviesDeferred.complete(Result.success(emptyList()))
        fakeRepo.popularSeriesDeferred.complete(Result.success(emptyList()))
        fakeRepo.topRatedMoviesDeferred.complete(Result.success(emptyList()))

        testScheduler.advanceUntilIdle()

        assertEquals("trending", viewModel.focusState.value?.rowId)
        assertEquals("t1", viewModel.focusState.value?.movieId)

        // User navigates horizontally to t3 (triggering onUserNavigate() from D-pad key event)
        viewModel.onUserNavigate()
        viewModel.onMovieFocused("trending", "t3", MediaType.MOVIE)
        testScheduler.advanceUntilIdle()

        // Verify focus stays on t3
        assertEquals("trending", viewModel.focusState.value?.rowId)
        assertEquals("t3", viewModel.focusState.value?.movieId)
    }
}
