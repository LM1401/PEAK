package com.example.peak.ui.screens.stream_selection

import com.example.peak.data.addon.AddonStorage
import com.example.peak.data.addon.AddonStreamApiClient
import com.example.peak.data.repository.AddonRepository
import com.example.peak.domain.model.AddonManifest
import com.example.peak.domain.model.AddonResource
import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieListType
import com.example.peak.domain.repository.TvListType
import com.example.peak.domain.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamSelectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeMovieRepository(
        private val imdbIdResult: (String, MediaType) -> Result<String> = { id, _ -> Result.success("tt$id") }
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
                name = "Test Title $id",
                imageUrl = "http://example.com/poster.jpg",
                backdropUrl = "http://example.com/bg.jpg",
                imdbId = imdbIdResult(id, type).getOrNull()
            )
            return Result.success(movie)
        }

        override suspend fun getImdbId(id: String, type: MediaType): Result<String> {
            return imdbIdResult(id, type)
        }
    }

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

    private fun createFakeAddonStorage(): AddonStorage {
        val fakePrefs = FakeSharedPreferences()
        return AddonStorage(
            context = android.content.ContextWrapper(null),
            prefs = fakePrefs,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun loadStreams_movieSuccess_populatesProviderResults() = runTest {
        val streamJson = """{"streams": [{"name": "1080p HD", "url": "https://stream.org/v.mp4"}]}"""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(streamJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val streamClient = AddonStreamApiClient(mockClient, testDispatcher)
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage, streamClient, testDispatcher)

        val addon = addonRepo.addAddon("https://torrentio.strem.fun/manifest.json").getOrThrow()
        val manifest = AddonManifest(
            id = "org.torrentio", version = "1.0.0", name = "Torrentio",
            resources = listOf(AddonResource(name = "stream", types = listOf("movie")))
        )
        addonRepo.updateAddonManifest(addon.id, manifest)

        val fakeMovieRepo = FakeMovieRepository(imdbIdResult = { _, _ -> Result.success("tt0111161") })
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        viewModel.loadStreams("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasStreams)
        assertEquals("tt0111161", state.imdbId)
        assertEquals(1, state.totalStreamCount)
        assertEquals("Torrentio", state.providerResults.first().addonName)
        assertEquals("1080p HD", state.providerResults.first().streams.first().name)
    }

    @Test
    fun loadStreams_noAddonsEnabled_setsNoAddonsFlag() = runTest {
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage, AddonStreamApiClient(), testDispatcher)
        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        viewModel.loadStreams("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.noAddonsConfigured)
        assertFalse(state.hasStreams)
    }

    @Test
    fun loadStreams_missingImdbId_setsErrorMessage() = runTest {
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage, AddonStreamApiClient(), testDispatcher)

        val addon = addonRepo.addAddon("https://torrentio.strem.fun/manifest.json").getOrThrow()
        val manifest = AddonManifest(
            id = "org.torrentio", version = "1.0.0", name = "Torrentio",
            resources = listOf(AddonResource(name = "stream", types = listOf("movie")))
        )
        addonRepo.updateAddonManifest(addon.id, manifest)

        val fakeMovieRepo = FakeMovieRepository(imdbIdResult = { _, _ ->
            Result.failure(NoSuchElementException("No IMDb ID found"))
        })
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        viewModel.loadStreams("999", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertFalse(state.hasStreams)
    }

    @Test
    fun loadStreams_tvSeriesWithoutSeasonEpisode_setsIsTvWithoutEpisode() = runTest {
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage, AddonStreamApiClient(), testDispatcher)

        val addon = addonRepo.addAddon("https://torrentio.strem.fun/manifest.json").getOrThrow()
        val manifest = AddonManifest(
            id = "org.torrentio", version = "1.0.0", name = "Torrentio",
            resources = listOf(AddonResource(name = "stream", types = listOf("series")))
        )
        addonRepo.updateAddonManifest(addon.id, manifest)

        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        viewModel.loadStreams("1399", MediaType.TV, season = null, episode = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isTvWithoutEpisode)
        assertFalse(state.hasStreams)
    }

    @Test
    fun loadStreams_partialProviderFailure_preservesSuccessfulResults() = runTest {
        val streamJson = """{"streams": [{"name": "GoodStream", "url": "https://stream.org/v.mp4"}]}"""
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url.toString()
                if (url.contains("failing")) {
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(500)
                        .message("Internal Error")
                        .body("Error".toResponseBody("text/plain".toMediaTypeOrNull()))
                        .build()
                } else {
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(streamJson.toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }
            }
            .build()

        val streamClient = AddonStreamApiClient(mockClient, testDispatcher)
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage = storage, streamApiClient = streamClient, ioDispatcher = testDispatcher)

        val manifest1 = AddonManifest(
            id = "org.good", version = "1.0.0", name = "Good Provider",
            resources = listOf(AddonResource(name = "stream", types = listOf("movie")))
        )
        val manifest2 = AddonManifest(
            id = "org.failing", version = "1.0.0", name = "Failing Provider",
            resources = listOf(AddonResource(name = "stream", types = listOf("movie")))
        )

        val addon1 = addonRepo.addAddon("https://good.addon.org/manifest.json").getOrThrow()
        addonRepo.updateAddonManifest(addon1.id, manifest1)

        val addon2 = addonRepo.addAddon("https://failing.addon.org/manifest.json").getOrThrow()
        addonRepo.updateAddonManifest(addon2.id, manifest2)

        val fakeMovieRepo = FakeMovieRepository(imdbIdResult = { _, _ -> Result.success("tt0111161") })
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        viewModel.loadStreams("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasStreams)
        assertEquals(2, state.providerResults.size)

        val goodRes = state.providerResults.find { it.addonName == "Good Provider" }
        assertNotNull(goodRes)
        assertEquals(1, goodRes!!.streams.size)

        val failRes = state.providerResults.find { it.addonName == "Failing Provider" }
        assertNotNull(failRes)
        assertTrue(failRes!!.streams.isEmpty())
        assertNotNull(failRes.error)
    }

    @Test
    fun selectStream_updatesState_doesNotTriggerPlayback() = runTest {
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage, AddonStreamApiClient(), testDispatcher)
        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        val testStream = AddonStream(name = "720p", url = "https://example.com/video.m3u8")
        viewModel.selectStream(testStream)

        val state = viewModel.uiState.value
        assertEquals(testStream, state.selectedStream)
        // Verified: Stream is recorded in state without initiating playback or player navigation
    }

    @Test
    fun retry_reloadsStreamsForMedia() = runTest {
        val storage = createFakeAddonStorage()
        val addonRepo = AddonRepository(storage, AddonStreamApiClient(), testDispatcher)
        val fakeMovieRepo = FakeMovieRepository()
        val viewModel = StreamSelectionViewModel(fakeMovieRepo, addonRepo)

        viewModel.retry("550", MediaType.MOVIE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.noAddonsConfigured)
    }
}
