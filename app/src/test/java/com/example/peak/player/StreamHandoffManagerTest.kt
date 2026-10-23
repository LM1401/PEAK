package com.example.peak.player

import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.MediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StreamHandoffManagerTest {

    @Before
    fun setUp() {
        StreamHandoffManager.clear()
    }

    @After
    fun tearDown() {
        StreamHandoffManager.clear()
    }

    @Test
    fun setStream_storesStreamAndPeekReturnsItForCorrectIdentity() {
        val stream = AddonStream(name = "Test Stream", url = "https://example.com/video.mp4")
        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream)

        val peeked = StreamHandoffManager.peekStream("550", MediaType.MOVIE)
        assertNotNull(peeked)
        assertEquals(stream, peeked)
    }

    @Test
    fun getAndClearStream_matchingIdentity_returnsStreamAndClearsIt() {
        val stream = AddonStream(name = "Test Stream", url = "https://example.com/video.mp4")
        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream)

        val retrieved = StreamHandoffManager.getAndClearStream("550", MediaType.MOVIE)
        assertNotNull(retrieved)
        assertEquals(stream, retrieved)

        // Subsequent calls return null after clearing
        assertNull(StreamHandoffManager.peekStream("550", MediaType.MOVIE))
        assertNull(StreamHandoffManager.getAndClearStream("550", MediaType.MOVIE))
    }

    @Test
    fun getAndClearStream_mismatchedMediaId_returnsNullAndPreservesStream() {
        val stream = AddonStream(name = "Test Stream", url = "https://example.com/video.mp4")
        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream)

        // Query with different media ID ("999")
        val retrieved = StreamHandoffManager.getAndClearStream("999", MediaType.MOVIE)
        assertNull(retrieved)

        // Stream remains preserved for original media ID ("550")
        val correctPeek = StreamHandoffManager.peekStream("550", MediaType.MOVIE)
        assertNotNull(correctPeek)
        assertEquals(stream, correctPeek)
    }

    @Test
    fun getAndClearStream_mismatchedMediaType_returnsNullAndPreservesStream() {
        val stream = AddonStream(name = "Test Stream", url = "https://example.com/video.mp4")
        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream)

        // Query with different media type (TV instead of MOVIE)
        val retrieved = StreamHandoffManager.getAndClearStream("550", MediaType.TV)
        assertNull(retrieved)

        // Stream remains preserved for MOVIE type
        val correctPeek = StreamHandoffManager.peekStream("550", MediaType.MOVIE)
        assertNotNull(correctPeek)
        assertEquals(stream, correctPeek)
    }

    @Test
    fun setStream_replacesPendingStream() {
        val stream1 = AddonStream(name = "Stream 1", url = "https://example.com/1.mp4")
        val stream2 = AddonStream(name = "Stream 2", url = "https://example.com/2.mp4")

        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream1)
        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream2)

        val peeked = StreamHandoffManager.peekStream("550", MediaType.MOVIE)
        assertEquals(stream2, peeked)
    }

    @Test
    fun clear_resetsActiveStream() {
        val stream = AddonStream(name = "Test Stream", url = "https://example.com/video.mp4")
        StreamHandoffManager.setStream("550", MediaType.MOVIE, stream)
        StreamHandoffManager.clear()

        assertNull(StreamHandoffManager.peekStream("550", MediaType.MOVIE))
    }
}
