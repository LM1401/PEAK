package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.data.remote.dto.TmdbProvider
import com.example.peak.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verification of the PEAK Branding Resolver.
 * Tests focus on deterministic mapping of TMDB entities to PEAK display brands.
 */
class ProductionCompanyRecognitionTest {

    // COMPANIES
    private val columbia = TmdbCompany(id = 5, name = "Columbia Pictures", logoPath = null, originCountry = "US")
    private val mgm = TmdbCompany(id = 21, name = "Metro-Goldwyn-Mayer", logoPath = null, originCountry = "US")
    private val universal = TmdbCompany(id = 33, name = "Universal Pictures", logoPath = null, originCountry = "US")
    private val lionsgate = TmdbCompany(id = 1632, name = "Lionsgate", logoPath = null, originCountry = "US")
    private val amazonMGM = TmdbCompany(id = 210099, name = "Amazon MGM Studios", logoPath = null, originCountry = "US")

    // NETWORKS
    private val amc = TmdbCompany(id = 174, name = "AMC", logoPath = null, originCountry = "US")
    private val primeVideoNetwork = TmdbCompany(id = 1024, name = "Prime Video", logoPath = null, originCountry = "US")

    // PROVIDERS
    private val netflixProvider = TmdbProvider(id = 8, name = "Netflix")
    private val primeProvider = TmdbProvider(id = 119, name = "Amazon Prime Video")

    @Test
    fun `BRANDING - Resolution of curated PEAK brands`() {
        assertEquals("Sony Pictures", ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, null, null, MediaType.MOVIE))
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(universal), null, null, null, MediaType.MOVIE))
        assertEquals("MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm), null, null, null, MediaType.MOVIE))
    }

    @Test
    fun `PRIORITY - Provider beats network and company`() {
        val amazonStudios = TmdbCompany(id = 20580, name = "Amazon Studios", logoPath = null, originCountry = "US")

        // Amazon Studios (Company) + Prime Video (Network) + Prime Video (Provider)
        // Provider (Streaming Original) should win and use "Prime Exclusive" branding for TV
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(amazonStudios),
            networks = listOf(primeVideoNetwork),
            providers = listOf(primeProvider),
            releaseNotes = null,
            mediaType = MediaType.TV
        )
        assertEquals("Prime Exclusive", result)
    }

    @Test
    fun `PRIORITY - Release note is used as fallback for movies`() {
        val obscureCompany = TmdbCompany(id = 999999, name = "Obscure Productions", logoPath = null, originCountry = "US")
        val releaseNotes = listOf("Lionsgate")

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(obscureCompany),
            networks = null,
            providers = null,
            releaseNotes = releaseNotes,
            mediaType = MediaType.MOVIE
        )
        assertEquals("Lionsgate", result)
    }

    @Test
    fun `PRIORITY - Stronger signal beats weak release note`() {
        val releaseNotes = listOf("Lionsgate")

        // Universal (Company) vs Lionsgate (Note) -> Universal should win
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(universal),
            networks = null,
            providers = null,
            releaseNotes = releaseNotes,
            mediaType = MediaType.MOVIE
        )
        assertEquals("Universal", result)
    }

    @Test
    fun `NAMESPACE - Isolation of entity IDs between Movie and TV`() {
        // ID 174 -> Warner Bros (Company) - via name pattern match in this synthetic case or ID
        val warner = TmdbCompany(id = 174, name = "Warner Bros.", logoPath = null, originCountry = "US")
        assertEquals("Warner Bros.", ProductionCompanyRecognition.findBestCompany(listOf(warner), null, null, null, MediaType.MOVIE))

        // ID 174 -> AMC (Network)
        assertEquals("AMC", ProductionCompanyRecognition.findBestCompany(null, listOf(amc), null, null, MediaType.TV))
    }

    @Test
    fun `STREAMING - Netflix provider resolves to Netflix Original`() {
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = null,
            networks = null,
            providers = listOf(netflixProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )
        assertEquals("Netflix Original", result)
    }

    @Test
    fun `REGRESSION - Challengers resolves to Amazon MGM Studios`() {
        assertEquals("Amazon MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm, amazonMGM), null, null, null, MediaType.MOVIE))
    }
}
