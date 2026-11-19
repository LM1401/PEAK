package com.example.peak.data.provider

import com.example.peak.data.remote.api.TraktApi
import com.example.peak.data.remote.dto.TraktIdsDto
import com.example.peak.data.remote.dto.TraktImagesDto
import com.example.peak.data.remote.dto.TraktMediaItemDto
import com.example.peak.data.remote.dto.TraktSearchResponseDto
import com.example.peak.domain.model.MediaType
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class TraktLogoProviderImplTest {

    private lateinit var fakeApi: FakeTraktApi
    private lateinit var provider: TraktLogoProviderImpl

    @Before
    fun setUp() {
        fakeApi = FakeTraktApi()
        provider = TraktLogoProviderImpl(fakeApi)
    }

    @Test
    fun `Successful movie logo lookup`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = listOf("https://walter.trakt.tv/logo.png"))
                    )
                )
            )
        )

        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.Found)
        assertEquals("https://walter.trakt.tv/logo.png", (result as AlternativeLogoResult.Found).url)
        assertEquals("movie", fakeApi.lastType)
    }

    @Test
    fun `Successful TV logo lookup`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "show",
                    show = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 456),
                        images = TraktImagesDto(logo = listOf("https://walter.trakt.tv/show.png"))
                    )
                )
            )
        )

        val result = provider.findLogo("456", MediaType.TV)
        assertTrue(result is AlternativeLogoResult.Found)
        assertEquals("https://walter.trakt.tv/show.png", (result as AlternativeLogoResult.Found).url)
        assertEquals("show", fakeApi.lastType)
    }

    @Test
    fun `Wrong TMDB ID rejected`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 999), // Different ID
                        images = TraktImagesDto(logo = listOf("https://walter.trakt.tv/logo.png"))
                    )
                )
            )
        )

        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `Wrong result type rejected`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "show", // Returned show but we asked for movie
                    show = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = listOf("https://walter.trakt.tv/logo.png"))
                    )
                )
            )
        )

        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `Empty response returns NotFound`() = runBlocking {
        fakeApi.mockResponse = Response.success(emptyList())
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `Missing nested payload returns NotFound`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(type = "movie") // No movie object
            )
        )
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `Missing logo returns NotFound`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = emptyList())
                    )
                )
            )
        )
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `Invalid logo URL returns NotFound`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = listOf("https://invalid.com/logo.jpg"))
                    )
                )
            )
        )
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `HTTP 404 returns NotFound`() = runBlocking {
        fakeApi.mockResponse = Response.error(404, "".toResponseBody(null))
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `HTTP 429 returns RateLimited`() = runBlocking {
        val okHttpResponse = okhttp3.Response.Builder()
            .code(429)
            .message("Too Many Requests")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .request(okhttp3.Request.Builder().url("https://api.trakt.tv").build())
            .header("Retry-After", "30")
            .build()
            
        fakeApi.mockResponse = Response.error(
            "".toResponseBody(null),
            okHttpResponse
        )
        
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.RateLimited)
        assertEquals(30, (result as AlternativeLogoResult.RateLimited).retryAfterSeconds)
    }

    @Test
    fun `HTTP 500 returns Error`() = runBlocking {
        fakeApi.mockResponse = Response.error(500, "".toResponseBody(null))
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.Error)
    }

    @Test
    fun `Network exception returns Error`() = runBlocking {
        fakeApi.shouldThrow = true
        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.Error)
    }

    @Test
    fun `CancellationException is rethrown and not swallowed`() {
        fakeApi.shouldThrowCancellation = true
        var exceptionThrown = false
        try {
            runBlocking {
                provider.findLogo("123", MediaType.MOVIE)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            exceptionThrown = true
        }
        assertTrue("CancellationException should have been thrown", exceptionThrown)
    }

    @Test
    fun `Examines all matching results until valid logo found`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = emptyList()) // No logo
                    )
                ),
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = listOf("https://invalid.com/logo.jpg")) // Invalid logo
                    )
                ),
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = listOf("https://walter.trakt.tv/logo.png")) // Valid logo
                    )
                )
            )
        )

        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.Found)
        assertEquals("https://walter.trakt.tv/logo.png", (result as AlternativeLogoResult.Found).url)
    }

    @Test
    fun `No matching result has a valid logo returns NotFound`() = runBlocking {
        fakeApi.mockResponse = Response.success(
            listOf(
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = emptyList())
                    )
                ),
                TraktSearchResponseDto(
                    type = "movie",
                    movie = TraktMediaItemDto(
                        ids = TraktIdsDto(tmdb = 123),
                        images = TraktImagesDto(logo = listOf("https://invalid.com/logo.jpg"))
                    )
                )
            )
        )

        val result = provider.findLogo("123", MediaType.MOVIE)
        assertTrue(result is AlternativeLogoResult.NotFound)
    }

    @Test
    fun `Movie and TV cache keys do not collide`() {
        val movieKey = AlternativeLogoKey(MediaType.MOVIE, "123").value
        val tvKey = AlternativeLogoKey(MediaType.TV, "123").value
        assertNotEquals(movieKey, tvKey)
        assertEquals("MOVIE_123", movieKey)
        assertEquals("TV_123", tvKey)
    }

    class FakeTraktApi : TraktApi {
        var mockResponse: Response<List<TraktSearchResponseDto>>? = null
        var shouldThrow = false
        var shouldThrowCancellation = false
        var lastTmdbId: String? = null
        var lastType: String? = null

        override suspend fun searchByTmdbId(
            tmdbId: String,
            type: String,
            extended: String
        ): Response<List<TraktSearchResponseDto>> {
            if (shouldThrowCancellation) throw kotlinx.coroutines.CancellationException("Cancelled")
            if (shouldThrow) throw java.io.IOException("Network failure")
            lastTmdbId = tmdbId
            lastType = type
            return mockResponse ?: Response.success(emptyList())
        }
    }
}
