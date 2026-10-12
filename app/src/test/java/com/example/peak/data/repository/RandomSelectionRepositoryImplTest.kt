package com.example.peak.data.repository

import com.example.peak.domain.model.DiscoveryMode
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.RandomSelectionRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomSelectionRepositoryImplTest {

    // Simple stub tests that would use Mockito/MockK to mock TmdbApi.
    // Included to verify structure.

    @Test
    fun testValidAllocations() {
        val request = RandomSelectionRequest(
            mode = DiscoveryMode.COMPLETELY_RANDOM,
            moviesRequested = 6,
            tvRequested = 4
        )
        assertEquals(10, request.totalRequested)
        assertTrue(request.includeMovies)
        assertTrue(request.includeTvShows)
    }

    @Test
    fun testSessionExclusions() {
        val exclusions = setOf("MOVIE_123", "TV_456")
        val request = RandomSelectionRequest(sessionExclusions = exclusions)
        assertTrue(request.sessionExclusions.contains("MOVIE_123"))
    }
}
