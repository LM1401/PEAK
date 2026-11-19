package com.example.peak.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TraktImageUtilsTest {

    @Test
    fun `HTTPS URL preserved`() {
        val urls = listOf("https://walter.trakt.tv/images/movies/000/000/001/logos/original/logo.png.webp")
        assertEquals("https://walter.trakt.tv/images/movies/000/000/001/logos/original/logo.png.webp", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `HTTP URL converted to HTTPS`() {
        val urls = listOf("http://walter.trakt.tv/image.png")
        assertEquals("https://walter.trakt.tv/image.png", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Protocol-relative URL normalised`() {
        val urls = listOf("//walter.trakt.tv/image.png")
        assertEquals("https://walter.trakt.tv/image.png", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `png accepted`() {
        val urls = listOf("https://walter.trakt.tv/logo.png")
        assertEquals("https://walter.trakt.tv/logo.png", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `webp accepted`() {
        val urls = listOf("https://walter.trakt.tv/logo.webp")
        assertEquals("https://walter.trakt.tv/logo.webp", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `png_webp accepted`() {
        val urls = listOf("https://walter.trakt.tv/logo.png.webp")
        assertEquals("https://walter.trakt.tv/logo.png.webp", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Query parameters preserved and path checked correctly`() {
        val urls = listOf("https://walter.trakt.tv/logo.png.webp?size=500&width=100")
        assertEquals("https://walter.trakt.tv/logo.png.webp?size=500&width=100", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Blank URL rejected`() {
        val urls = listOf("   ")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Invalid extension rejected`() {
        val urls = listOf("https://walter.trakt.tv/logo.jpg")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Extension check targets path not query params`() {
        // "logo.jpg" path but query contains ".png"
        val urls = listOf("https://walter.trakt.tv/logo.jpg?test=.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Unsupported scheme rejected`() {
        val urls = listOf("file:///local/image.png", "data:image/png;base64,12345", "ftp://walter.trakt.tv/logo.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Malformed URLs safely rejected`() {
        val urls = listOf("https://walter.trakt.tv/logo.png:invalid:port")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Unexpected host rejected`() {
        val urls = listOf("https://malicious.com/image.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `walter-r2_trakt_tv accepted`() {
        val urls = listOf("https://walter-r2.trakt.tv/logo.png")
        assertEquals("https://walter-r2.trakt.tv/logo.png", urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Arbitrary unapproved trakt subdomains rejected`() {
        val urls = listOf("https://evil.trakt.tv/logo.png", "https://api.trakt.tv/logo.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `User info in URL rejected or parsed safely without matching host`() {
        val urls = listOf("https://admin:password@walter.trakt.tv.evil.com/logo.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Non-standard ports parsed safely but hostname must match`() {
        val urls = listOf("https://walter.trakt.tv:8443/logo.png")
        // The URI parser preserves the host as walter.trakt.tv and port as 8443. 
        // Our check only validates the host portion.
        assertEquals("https://walter.trakt.tv:8443/logo.png", urls.firstValidTraktLogoUrl())
        
        val invalidUrls = listOf("https://malicious.com:8443/logo.png")
        assertNull(invalidUrls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Lookalike host trakt_tv_evil_example rejected`() {
        val urls = listOf("https://trakt.tv.evil.example/image.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Lookalike host eviltrakt_tv rejected`() {
        val urls = listOf("https://eviltrakt.tv/image.png")
        assertNull(urls.firstValidTraktLogoUrl())
    }

    @Test
    fun `Multiple URLs selects first valid`() {
        val urls = listOf(
            "   ",
            "https://walter.trakt.tv/logo.jpg",
            "https://eviltrakt.tv/logo.png",
            "http://walter.trakt.tv/logo.png"
        )
        assertEquals("https://walter.trakt.tv/logo.png", urls.firstValidTraktLogoUrl())
    }
}
