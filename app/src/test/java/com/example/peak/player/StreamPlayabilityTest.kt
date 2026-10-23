package com.example.peak.player

import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.AddonStreamBehaviorHints
import com.example.peak.domain.model.AddonStreamProxyHeaders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamPlayabilityTest {

    @Test
    fun classifyPlayability_directHttpUrl_returnsPlayable() {
        val stream = AddonStream(
            name = "Torrentio 1080p",
            title = "Big Buck Bunny",
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        )

        val result = stream.classifyPlayability()
        assertTrue(result is StreamPlayability.Playable)

        val playable = result as StreamPlayability.Playable
        assertEquals("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", playable.url)
        assertTrue(playable.headers.isEmpty())
        assertEquals("Torrentio 1080p", playable.streamName)
        assertEquals("Big Buck Bunny", playable.streamTitle)
    }

    @Test
    fun classifyPlayability_directHttpsUrlWithProxyHeaders_returnsPlayableWithHeaders() {
        val headers = mapOf("User-Agent" to "Stremio", "Referer" to "https://strem.fun")
        val stream = AddonStream(
            name = "Proxy Stream",
            url = "https://proxy.example.com/video.m3u8",
            behaviorHints = AddonStreamBehaviorHints(
                proxyHeaders = AddonStreamProxyHeaders(request = headers)
            )
        )

        val result = stream.classifyPlayability()
        assertTrue(result is StreamPlayability.Playable)

        val playable = result as StreamPlayability.Playable
        assertEquals("https://proxy.example.com/video.m3u8", playable.url)
        assertEquals(headers, playable.headers)
    }

    @Test
    fun classifyPlayability_notVideoFlag_returnsUnsupported() {
        val stream = AddonStream(
            url = "https://example.com/audio.mp3",
            behaviorHints = AddonStreamBehaviorHints(notVideo = true)
        )

        val result = stream.classifyPlayability()
        assertTrue(result is StreamPlayability.Unsupported)
    }

    @Test
    fun classifyPlayability_infoHashOnly_returnsUnsupported() {
        val stream = AddonStream(
            infoHash = "b415c913643e5ff49fe37d304bbb5e6e11ad5100",
            url = null
        )

        val result = stream.classifyPlayability()
        assertTrue(result is StreamPlayability.Unsupported)

        val unsupported = result as StreamPlayability.Unsupported
        assertTrue(unsupported.reason.contains("infohash"))
    }

    @Test
    fun classifyPlayability_externalUrlOnly_returnsUnsupported() {
        val stream = AddonStream(
            externalUrl = "https://external-website.com/watch",
            url = null
        )

        val result = stream.classifyPlayability()
        assertTrue(result is StreamPlayability.Unsupported)

        val unsupported = result as StreamPlayability.Unsupported
        assertTrue(unsupported.reason.contains("External web links"))
    }

    @Test
    fun classifyPlayability_blankUrl_returnsUnsupported() {
        val stream = AddonStream(url = "   ")

        val result = stream.classifyPlayability()
        assertTrue(result is StreamPlayability.Unsupported)
    }
}
