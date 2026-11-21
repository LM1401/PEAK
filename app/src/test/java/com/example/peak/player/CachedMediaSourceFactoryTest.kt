package com.example.peak.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class CachedMediaSourceFactoryTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun sanitizeHeaders_nullOrEmptyMap_returnsEmptyMap() {
        val nullResult = CachedMediaSourceFactory.sanitizeHeaders(null)
        val emptyResult = CachedMediaSourceFactory.sanitizeHeaders(emptyMap())

        assertTrue(nullResult.isEmpty())
        assertTrue(emptyResult.isEmpty())
    }

    @Test
    fun sanitizeHeaders_blankKeysAndValues_filtersThemOut() {
        val input = mapOf(
            "" to "validValue",
            "   " to "validValue2",
            "ValidKey" to "",
            "ValidKey2" to "   ",
            "  ValidKey3  " to "  ValidValue3  "
        )

        val sanitized = CachedMediaSourceFactory.sanitizeHeaders(input)

        assertEquals(1, sanitized.size)
        assertEquals("ValidValue3", sanitized["ValidKey3"])
    }

    @Test
    fun sanitizeHeaders_caseInsensitiveRangeHeaders_filtersThemOut() {
        val input = mapOf(
            "Range" to "bytes=0-1024",
            "range" to "bytes=500-",
            "RANGE" to "bytes=10-20",
            "User-Agent" to "Stremio/1.0",
            "Referer" to "https://stremio.com"
        )

        val sanitized = CachedMediaSourceFactory.sanitizeHeaders(input)

        assertEquals(2, sanitized.size)
        assertEquals("Stremio/1.0", sanitized["User-Agent"])
        assertEquals("https://stremio.com", sanitized["Referer"])
        assertTrue(sanitized.keys.none { it.equals("Range", ignoreCase = true) })
    }

    @Test
    fun defaultUserAgent_hasExpectedValue() {
        assertEquals("PEAK/1.0 (Android TV; Media3)", CachedMediaSourceFactory.DEFAULT_USER_AGENT)
    }

    @Test
    fun createMediaSource_withEmptyHeaders_returnsValidMediaSource() {
        val context: Context = RuntimeEnvironment.getApplication()
        val mediaSource = CachedMediaSourceFactory.createMediaSource(
            context = context,
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        )

        assertNotNull(mediaSource)
        assertEquals(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            mediaSource.mediaItem.localConfiguration?.uri.toString()
        )
    }

    @Test
    fun createMediaSource_withCustomHeaders_returnsValidMediaSource() {
        val context: Context = RuntimeEnvironment.getApplication()
        val headers = mapOf(
            "User-Agent" to "CustomTVUserAgent/2.0",
            "Referer" to "https://provider.example.com",
            "  Authorization  " to "  Bearer test_token  ",
            "Range" to "bytes=0-100" // Should be filtered out by sanitizeHeaders
        )

        val mediaSource = CachedMediaSourceFactory.createMediaSource(
            context = context,
            streamUrl = "https://provider.example.com/stream.m3u8",
            headers = headers
        )

        assertNotNull(mediaSource)
        assertEquals(
            "https://provider.example.com/stream.m3u8",
            mediaSource.mediaItem.localConfiguration?.uri.toString()
        )
    }

    @Test
    fun getInstance_returnsSingletonInstance() {
        val context: Context = RuntimeEnvironment.getApplication()
        val instance1 = CachedMediaSourceFactory.getInstance(context)
        val instance2 = CachedMediaSourceFactory.getInstance(context)

        assertNotNull(instance1)
        assertEquals(instance1, instance2)
    }

    // --- MOCK WEBSERVER WIRE-LEVEL HTTP REQUEST TESTS ---

    @Test
    fun httpWireTest_defaultUserAgent_sentOnNetworkRequest() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("test_stream_data"))

        val videoUrl = mockWebServer.url("/video.mp4").toString()
        val sanitized = CachedMediaSourceFactory.sanitizeHeaders(emptyMap())

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val hasCustomUserAgent = sanitized.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUserAgent) {
            httpDataSourceFactory(httpFactory)
        }

        val dataSource = httpFactory.createDataSource()
        dataSource.open(DataSpec(Uri.parse(videoUrl)))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PEAK/1.0 (Android TV; Media3)", recordedRequest.getHeader("User-Agent"))
    }

    @Test
    fun httpWireTest_customUserAgent_takesPrecedenceOnNetworkRequest() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("test_stream_data"))

        val videoUrl = mockWebServer.url("/stream.m3u8").toString()
        val rawHeaders = mapOf("user-agent" to "ProviderSpecialUserAgent/3.0")
        val sanitized = CachedMediaSourceFactory.sanitizeHeaders(rawHeaders)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val hasCustomUserAgent = sanitized.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUserAgent) {
            httpDataSourceFactory(httpFactory)
        }
        httpFactory.setDefaultRequestProperties(sanitized)

        val dataSource = httpFactory.createDataSource()
        dataSource.open(DataSpec(Uri.parse(videoUrl)))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("ProviderSpecialUserAgent/3.0", recordedRequest.getHeader("user-agent"))
    }

    @Test
    fun httpWireTest_refererAndAuthHeaders_reachServer() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("test_stream_data"))

        val videoUrl = mockWebServer.url("/video.mp4").toString()
        val rawHeaders = mapOf(
            "Referer" to "https://strem.fun",
            "Authorization" to "Bearer token_xyz_123"
        )
        val sanitized = CachedMediaSourceFactory.sanitizeHeaders(rawHeaders)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val hasCustomUserAgent = sanitized.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUserAgent) {
            httpDataSourceFactory(httpFactory)
        }
        httpFactory.setDefaultRequestProperties(sanitized)

        val dataSource = httpFactory.createDataSource()
        dataSource.open(DataSpec(Uri.parse(videoUrl)))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("https://strem.fun", recordedRequest.getHeader("Referer"))
        assertEquals("Bearer token_xyz_123", recordedRequest.getHeader("Authorization"))
        assertEquals("PEAK/1.0 (Android TV; Media3)", recordedRequest.getHeader("User-Agent"))
    }

    @Test
    fun httpWireTest_manualRangeHeader_filteredBeforeNetworkRequest() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("test_stream_data"))

        val videoUrl = mockWebServer.url("/video.mp4").toString()
        val rawHeaders = mapOf(
            "Range" to "bytes=0-100",
            "X-Custom-Header" to "PEAK-Header"
        )
        val sanitized = CachedMediaSourceFactory.sanitizeHeaders(rawHeaders)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)

        val hasCustomUserAgent = sanitized.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUserAgent) {
            httpDataSourceFactory(httpFactory)
        }
        if (sanitized.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(sanitized)
        }

        val dataSource = httpFactory.createDataSource()
        dataSource.open(DataSpec(Uri.parse(videoUrl)))

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("PEAK-Header", recordedRequest.getHeader("X-Custom-Header"))
        assertNull(recordedRequest.getHeader("Range"))
    }

    @Test
    fun httpWireTest_crossProtocolRedirect_followedSuccessfully() {
        val redirectTarget = mockWebServer.url("/target.mp4").toString()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", redirectTarget)
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("redirected_video_data"))

        val videoUrl = mockWebServer.url("/initial.mp4").toString()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(CachedMediaSourceFactory.DEFAULT_USER_AGENT)

        val dataSource = httpFactory.createDataSource()
        dataSource.open(DataSpec(Uri.parse(videoUrl)))

        val request1 = mockWebServer.takeRequest()
        val request2 = mockWebServer.takeRequest()

        assertEquals("/initial.mp4", request1.path)
        assertEquals("/target.mp4", request2.path)
        assertEquals("PEAK/1.0 (Android TV; Media3)", request1.getHeader("User-Agent"))
        assertEquals("PEAK/1.0 (Android TV; Media3)", request2.getHeader("User-Agent"))
    }

    private fun httpDataSourceFactory(factory: DefaultHttpDataSource.Factory) {
        factory.setUserAgent(CachedMediaSourceFactory.DEFAULT_USER_AGENT)
    }
}
