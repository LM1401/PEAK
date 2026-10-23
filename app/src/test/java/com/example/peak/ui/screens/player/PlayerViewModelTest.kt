package com.example.peak.ui.screens.player

import android.content.ContextWrapper
import com.example.peak.data.continuewatching.ContinueWatchingStorage
import com.example.peak.data.repository.ContinueWatchingRepository
import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.AddonStreamBehaviorHints
import com.example.peak.domain.model.AddonStreamProxyHeaders
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.MovieRepository
import com.example.peak.domain.repository.TvListType
import com.example.peak.player.StreamHandoffManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var continueWatchingRepo: ContinueWatchingRepository

    private inner class FakeSharedPreferences : android.content.SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): android.content.SharedPreferences.Editor = EditorImpl()

        override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private inner class EditorImpl : android.content.SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor {
                if (key != null) map[key] = value ?: ""
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): android.content.SharedPreferences.Editor {
                if (key != null) map[key] = values ?: mutableSetOf<String>()
                return this
            }
            override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor {
                if (key != null) map[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor {
                if (key != null) map[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor {
                if (key != null) map[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor {
                if (key != null) map[key] = value
                return this
            }
            override fun remove(key: String?): android.content.SharedPreferences.Editor {
                if (key != null) map.remove(key)
                return this
            }
            override fun clear(): android.content.SharedPreferences.Editor {
                map.clear()
                return this
            }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }

    private inner class FakeContext : android.content.ContextWrapper(null) {
        private val prefs = FakeSharedPreferences()
        override fun getSharedPreferences(name: String?, mode: Int): android.content.SharedPreferences = prefs
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        StreamHandoffManager.clear()
        val storage = ContinueWatchingStorage(FakeContext())
        continueWatchingRepo = ContinueWatchingRepository(storage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        StreamHandoffManager.clear()
    }

    private class FakeMovieRepository(
        private val fallbackUrl: String? = "https://example.com/fallback.mp4"
    ) : MovieRepository {
        override suspend fun getTrendingMovies(): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getTrendingSeries(): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getMovies(type: MovieListType, genreId: String?): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun getSeries(type: TvListType, genreId: String?): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun searchMulti(query: String): Result<List<Movie>> = Result.success(emptyList())
        override suspend fun discoverMedia(genreIds: String): Result<List<Movie>> = Result.success(emptyList())

        override suspend fun getMediaById(id: String, type: MediaType): Result<Movie> {
            val movie = Movie(
                movieId = id,
                mediaType = type,
                name = "Test Movie $id",
                imageUrl = "https://example.com/poster.jpg",
                backdropUrl = "https://example.com/bg.jpg",
                videoUrl = fallbackUrl
            )
            return Result.success(movie)
        }

        override suspend fun getImdbId(id: String, type: MediaType): Result<String> {
            return Result.success("tt$id")
        }
    }

    @Test
    fun loadMedia_withPlayableHandoffStream_usesHandoffUrlAndHeaders() = runTest {
        val headers = mapOf("User-Agent" to "PEAK-TV")
        val handoffStream = AddonStream(
            name = "1080p Torrentio",
            url = "https://stream.provider.com/video.mp4",
            behaviorHints = AddonStreamBehaviorHints(
                proxyHeaders = AddonStreamProxyHeaders(request = headers)
            )
        )
        StreamHandoffManager.setStream("550", MediaType.MOVIE, handoffStream)

        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = PlayerViewModel(continueWatchingRepo, fakeMovieRepo, StreamHandoffManager)

        viewModel.loadMedia("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val session = viewModel.playbackSession.value
        assertNotNull(session)
        assertEquals("550", session!!.mediaId)
        assertEquals("https://stream.provider.com/video.mp4", session.videoUrl)
        assertEquals(headers, session.headers)
        assertEquals("1080p Torrentio", session.title)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun loadMedia_withUnsupportedHandoffStream_setsErrorMessage() = runTest {
        val handoffStream = AddonStream(
            infoHash = "b415c913643e5ff49fe37d304bbb5e6e11ad5100",
            url = null
        )
        StreamHandoffManager.setStream("550", MediaType.MOVIE, handoffStream)

        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = PlayerViewModel(continueWatchingRepo, fakeMovieRepo, StreamHandoffManager)

        viewModel.loadMedia("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val session = viewModel.playbackSession.value
        assertNull(session)
        assertFalse(viewModel.isLoading.value)
        assertNotNull(viewModel.errorMessage.value)
        assertTrue(viewModel.errorMessage.value!!.contains("infohash"))
    }

    @Test
    fun loadMedia_withoutHandoffStream_fallsBackToMediaRepositoryUrl() = runTest {
        val fakeMovieRepo = FakeMovieRepository(fallbackUrl = "https://example.com/fallback.mp4")
        val viewModel = PlayerViewModel(continueWatchingRepo, fakeMovieRepo, StreamHandoffManager)

        viewModel.loadMedia("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val session = viewModel.playbackSession.value
        assertNotNull(session)
        assertEquals("https://example.com/fallback.mp4", session!!.videoUrl)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun setPlaybackError_updatesErrorMessage() = runTest {
        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = PlayerViewModel(continueWatchingRepo, fakeMovieRepo, StreamHandoffManager)

        viewModel.setPlaybackError("Playback Failed: HTTP 403 Forbidden")

        assertEquals("Playback Failed: HTTP 403 Forbidden", viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }
}
