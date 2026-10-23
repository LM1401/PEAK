package com.example.peak.data.addon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddonStreamApiClientTest {

    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockApiClient(statusCode: Int, responseJson: String): AddonStreamApiClient {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body(responseJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()
        return AddonStreamApiClient(client, testDispatcher)
    }

    @Test
    fun buildStreamUrl_standardManifestUrl_constructsCorrectStreamEndpoint() {
        val url = AddonStreamApiClient.buildStreamUrl("https://v4-cinemeta.strem.io/manifest.json", "movie", "tt0111161")
        assertEquals("https://v4-cinemeta.strem.io/stream/movie/tt0111161.json", url)
    }

    @Test
    fun buildStreamUrl_configuredManifestUrl_preservesConfiguration() {
        val url = AddonStreamApiClient.buildStreamUrl("https://torrentio.strem.fun/providers=yts,eztv/manifest.json", "series", "tt0944947:1:1")
        assertEquals("https://torrentio.strem.fun/providers=yts,eztv/stream/series/tt0944947:1:1.json", url)
    }

    @Test
    fun buildStreamUrl_stremioScheme_convertsToHttps() {
        val url = AddonStreamApiClient.buildStreamUrl("stremio://torrentio.strem.fun/manifest.json", "movie", "tt15239678")
        assertEquals("https://torrentio.strem.fun/stream/movie/tt15239678.json", url)
    }

    @Test
    fun fetchStreams_directVideoUrlStream_parsesSuccessfully() = runTest {
        val json = """
            {
              "streams": [
                {
                  "name": "Torrentio 1080p",
                  "title": "Dune.Part.Two.2024.1080p.WEBRip",
                  "url": "https://debrid.provider.com/dl/v12345/video.m3u8",
                  "quality": "1080p"
                }
              ]
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, json)
        val result = apiClient.fetchStreams("https://torrentio.strem.fun/manifest.json", "movie", "tt15239678")

        assertTrue(result.isSuccess)
        val streams = result.getOrThrow().streams
        assertEquals(1, streams.size)

        val stream = streams.first()
        assertEquals("Torrentio 1080p", stream.name)
        assertEquals("https://debrid.provider.com/dl/v12345/video.m3u8", stream.url)
        assertEquals("1080p", stream.quality)
    }

    @Test
    fun fetchStreams_externalUrlStream_parsesSuccessfully() = runTest {
        val json = """
            {
              "streams": [
                {
                  "name": "External Player",
                  "title": "Watch on YouTube",
                  "externalUrl": "https://youtube.com/watch?v=12345"
                }
              ]
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, json)
        val result = apiClient.fetchStreams("https://external.addon.org/manifest.json", "movie", "tt1234567")

        assertTrue(result.isSuccess)
        val stream = result.getOrThrow().streams.first()
        assertEquals("External Player", stream.name)
        assertEquals("https://youtube.com/watch?v=12345", stream.externalUrl)
        assertNull(stream.url)
    }

    @Test
    fun fetchStreams_torrentStreamWithInfoHashAndFileIdx_parsesSuccessfully() = runTest {
        val json = """
            {
              "streams": [
                {
                  "name": "Torrentio 4K",
                  "title": "Dune.Part.Two.2024.2160p",
                  "infoHash": "a1b2c3d4e5f67890123456789abcdef012345678",
                  "fileIdx": 0
                }
              ]
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, json)
        val result = apiClient.fetchStreams("https://torrentio.strem.fun/manifest.json", "movie", "tt15239678")

        assertTrue(result.isSuccess)
        val stream = result.getOrThrow().streams.first()
        assertEquals("a1b2c3d4e5f67890123456789abcdef012345678", stream.infoHash)
        assertEquals(0, stream.fileIdx)
        assertNull(stream.url)
    }

    @Test
    fun fetchStreams_behaviorHintsAndProxyHeaders_parsesSuccessfully() = runTest {
        val json = """
            {
              "streams": [
                {
                  "name": "Custom Headers Stream",
                  "url": "https://stream.server.org/video.mp4",
                  "behaviorHints": {
                    "notVideo": false,
                    "proxyHeaders": {
                      "request": {
                        "User-Agent": "PEAK/1.0",
                        "Referer": "https://origin.server.org"
                      }
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, json)
        val result = apiClient.fetchStreams("https://custom.addon.org/manifest.json", "movie", "tt0000001")

        assertTrue(result.isSuccess)
        val stream = result.getOrThrow().streams.first()
        assertNotNull(stream.behaviorHints)
        assertEquals("PEAK/1.0", stream.behaviorHints?.proxyHeaders?.request?.get("User-Agent"))
        assertEquals("https://origin.server.org", stream.behaviorHints?.proxyHeaders?.request?.get("Referer"))
    }

    @Test
    fun fetchStreams_emptyStreamsArray_returnsEmptyListSuccessfully() = runTest {
        val json = """
            {
              "streams": []
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, json)
        val result = apiClient.fetchStreams("https://empty.addon.org/manifest.json", "movie", "tt9999999")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().streams.isEmpty())
    }

    @Test
    fun fetchStreams_blankParameters_returnsFailure() = runTest {
        val apiClient = createMockApiClient(200, "{}")
        val result = apiClient.fetchStreams("   ", "movie", "tt12345")

        assertTrue(result.isFailure)
        assertEquals("Manifest URL, media type, and content ID cannot be blank", result.exceptionOrNull()?.message)
    }

    @Test
    fun fetchStreams_http404Error_returnsFailure() = runTest {
        val apiClient = createMockApiClient(404, "Not Found")
        val result = apiClient.fetchStreams("https://missing.addon.org/manifest.json", "movie", "tt0000000")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("HTTP error 404") == true)
    }

    @Test
    fun fetchStreams_malformedJson_returnsFailure() = runTest {
        val apiClient = createMockApiClient(200, "{ invalid json }")
        val result = apiClient.fetchStreams("https://malformed.addon.org/manifest.json", "movie", "tt0000000")

        assertTrue(result.isFailure)
    }

    @Test
    fun fetchStreams_invalidUrlScheme_returnsFailure() = runTest {
        val apiClient = createMockApiClient(200, "{}")
        val result = apiClient.fetchStreams("ftp://unsupported.addon.org/manifest.json", "movie", "tt0000000")

        assertTrue(result.isFailure)
        assertEquals("Unsupported URL scheme. Only HTTP and HTTPS are allowed.", result.exceptionOrNull()?.message)
    }
}
