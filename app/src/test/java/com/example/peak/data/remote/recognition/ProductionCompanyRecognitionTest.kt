package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.data.remote.dto.TmdbProvider
import com.example.peak.domain.model.BrandConfidence
import com.example.peak.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verification of the PEAK Branding Resolver.
 * Tests focus on deterministic mapping of TMDB entities to PEAK display brands.
 */
class ProductionCompanyRecognitionTest {

    // COMPANIES
    private val columbia = TmdbCompany(id = 5, name = "Columbia Pictures", logoPath = "/logo_columbia.png", originCountry = "US")
    private val mgm = TmdbCompany(id = 21, name = "Metro-Goldwyn-Mayer", logoPath = "/logo_mgm.png", originCountry = "US")
    private val universal = TmdbCompany(id = 33, name = "Universal Pictures", logoPath = null, originCountry = "US")
    private val lionsgate = TmdbCompany(id = 1632, name = "Lionsgate", logoPath = null, originCountry = "US")
    private val amazonMGM = TmdbCompany(id = 210099, name = "Amazon MGM Studios", logoPath = null, originCountry = "US")

    // NETWORKS
    private val amc = TmdbCompany(id = 174, name = "AMC", logoPath = null, originCountry = "US")
    private val primeVideoNetwork = TmdbCompany(id = 1024, name = "Prime Video", logoPath = "/prime_network_logo.png", originCountry = "US")

    // PROVIDERS
    private val netflixProvider = TmdbProvider(id = 8, name = "Netflix", logoPath = "/p69BYX927Y6pY686p69BYX927Y6.png")
    private val primeProvider = TmdbProvider(id = 119, name = "Amazon Prime Video", logoPath = "/pE356A2U7Z8D9E0F1G2H3I4J5K6.png")

    // EXTRA COMPANIES FOR DISPLAY ASSET TESTS
    private val netflixStudio = TmdbCompany(id = 178464, name = "Netflix Studios", logoPath = "/netflix_wordmark.png", originCountry = "US")


    @Test
    fun `BRANDING - Resolution of curated PEAK brands`() {
        assertEquals("Sony Pictures", ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, null, null, MediaType.MOVIE)?.displayName)
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(universal), null, null, null, MediaType.MOVIE)?.displayName)
        assertEquals("MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm), null, null, null, MediaType.MOVIE)?.displayName)
    }

    @Test
    fun `BRANDING - Propagation of TMDB logos`() {
        val result = ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, null, null, MediaType.MOVIE)
        assertNotNull(result?.logoUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/logo_columbia.png", result?.logoUrl)
    }

    @Test
    fun `BRANDING - Propagation of TMDB provider logos`() {
        // Netflix
        val netflixResult = ProductionCompanyRecognition.findBestCompany(null, null, listOf(netflixProvider), null, MediaType.MOVIE)
        assertEquals("https://image.tmdb.org/t/p/w500/p69BYX927Y6pY686p69BYX927Y6.png", netflixResult?.logoUrl)

        // Prime Video
        val primeResult = ProductionCompanyRecognition.findBestCompany(null, null, listOf(primeProvider), null, MediaType.MOVIE)
        assertEquals("https://image.tmdb.org/t/p/w500/pE356A2U7Z8D9E0F1G2H3I4J5K6.png", primeResult?.logoUrl)
    }

    @Test
    fun `BRANDING - Provider without logo falls back correctly`() {
        val providerNoLogo = TmdbProvider(id = 8, name = "Netflix", logoPath = null)
        val result = ProductionCompanyRecognition.findBestCompany(null, null, listOf(providerNoLogo), null, MediaType.MOVIE)
        
        assertEquals("Netflix Original", result?.displayName)
        assertNull(result?.logoUrl)
    }

    @Test
    fun `CONFIDENCE - Unknown company with logo is LOW confidence and NOT displayable`() {
        val unknownWithLogo = TmdbCompany(id = 999, name = "Unknown Studio", logoPath = "/path.png", originCountry = "US")
        val result = ProductionCompanyRecognition.findBestCompany(listOf(unknownWithLogo), null, null, null, MediaType.MOVIE)
        
        // It should NOT be returned by findBestCompany because findBestCompany filters for displayable brands
        assertNull(result)
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
        assertEquals("Prime Exclusive", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
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
        assertEquals("Lionsgate", result?.displayName)
        assertEquals(BrandConfidence.MEDIUM, result?.confidence)
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
        assertEquals("Universal", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
    }

    @Test
    fun `NAMESPACE - Isolation of entity IDs between Movie and TV`() {
        // ID 174 -> Warner Bros (Company)
        val warner = TmdbCompany(id = 174, name = "Warner Bros.", logoPath = null, originCountry = "US")
        assertEquals("Warner Bros.", ProductionCompanyRecognition.findBestCompany(listOf(warner), null, null, null, MediaType.MOVIE)?.displayName)

        // ID 174 -> AMC (Network)
        assertEquals("AMC", ProductionCompanyRecognition.findBestCompany(null, listOf(amc), null, null, MediaType.TV)?.displayName)
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
        assertEquals("Netflix Original", result?.displayName)
    }

    @Test
    fun `MUTINY - Verification for TMDB ID 1288445`() {
        val punchPalace = TmdbCompany(id = 218150, name = "Punch Palace Productions", logoPath = "/logo1.png", originCountry = "US")
        val madRiver = TmdbCompany(id = 73492, name = "MadRiver Pictures", logoPath = "/logo2.png", originCountry = "US")
        
        // 1. Without release notes, it should resolve to nothing displayable (Punch Palace/MadRiver not recognized, and unknown logos are LOW confidence)
        val resultNoNotes = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(punchPalace, madRiver),
            networks = null,
            providers = null,
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )
        assertNull(resultNoNotes)

        // 2. With "Lionsgate" in release notes, it should resolve to Lionsgate
        val resultWithNotes = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(punchPalace, madRiver),
            networks = null,
            providers = null,
            releaseNotes = listOf("Lionsgate"),
            mediaType = MediaType.MOVIE
        )
        assertEquals("Lionsgate", resultWithNotes?.displayName)
        assertEquals(BrandConfidence.MEDIUM, resultWithNotes?.confidence)
    }

    @Test
    fun `REGRESSION - Challengers resolves to Amazon MGM Studios`() {
        assertEquals("Amazon MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm, amazonMGM), null, null, null, MediaType.MOVIE)?.displayName)
    }

    @Test
    fun `PRIORITY - Brand Priority beats Source Priority (Specific beats Generic)`() {
        // Amazon Provider (ID 119) -> Brand "amazon" (Priority 0, Source Priority 10)
        // Amazon MGM Studios (ID 210099) -> Brand "amazon_mgm" (Priority 1, Source Priority 0)

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(amazonMGM),
            networks = null,
            providers = listOf(primeProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        // Amazon MGM should win because it has higher Brand Priority (1 vs 0)
        assertEquals("Amazon MGM Studios", result?.displayName)
    }

    @Test
    fun `DISPLAY_ASSET - Disney matches Pixar and uses its logo`() {
        val pixar = TmdbCompany(id = 3, name = "Pixar", logoPath = "/pixar_logo.png", originCountry = "US")
        
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(pixar),
            networks = null,
            providers = null,
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Disney", result?.displayName)
        assertEquals("https://image.tmdb.org/t/p/w500/pixar_logo.png", result?.logoUrl)
    }

    @Test
    fun `DISPLAY_ASSET - Disney matches WD Animation and uses its logo`() {
        val wdAnimation = TmdbCompany(id = 6125, name = "Walt Disney Animation Studios", logoPath = "/wdas_logo.png", originCountry = "US")

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(wdAnimation),
            networks = null,
            providers = null,
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Disney", result?.displayName)
        assertEquals("https://image.tmdb.org/t/p/w500/wdas_logo.png", result?.logoUrl)
    }

    @Test
    fun `DISPLAY_ASSET - Netflix prefers wordmark over provider icon`() {
        // Netflix Provider (Icon) + Netflix Studio (Wordmark)
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(netflixStudio),
            networks = null,
            providers = listOf(netflixProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Netflix Original", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
        // Should prefer the company logo over the provider icon
        assertEquals("https://image.tmdb.org/t/p/w500/netflix_wordmark.png", result?.logoUrl)
    }

    @Test
    fun `DISPLAY_ASSET - Prime Video prefers network logo over provider icon`() {
        // Prime Provider (Icon) + Prime Video Network (Logo)
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = null,
            networks = listOf(primeVideoNetwork),
            providers = listOf(primeProvider),
            releaseNotes = null,
            mediaType = MediaType.TV
        )

        assertEquals("Prime Exclusive", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
        // Should prefer the network logo over the provider icon
        assertEquals("https://image.tmdb.org/t/p/w500/prime_network_logo.png", result?.logoUrl)
    }

    @Test
    fun `DISPLAY_ASSET - Disney prefers studio logo`() {
        val disneyStudio = TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = "/disney_wordmark.png", originCountry = "US")
        val disneyProvider = TmdbProvider(id = 337, name = "Disney+", logoPath = "/disney_plus_icon.png")

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(disneyStudio),
            networks = null,
            providers = listOf(disneyProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Disney", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
        assertEquals("https://image.tmdb.org/t/p/w500/disney_wordmark.png", result?.logoUrl)
    }

    @Test
    fun `LOCAL_ASSET - Netflix uses local resource fallback`() {
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = null,
            networks = null,
            providers = listOf(netflixProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Netflix Original", result?.displayName)
        // Should have the local resource ID from BrandCatalog
        assertNotNull(result?.localResource)
    }

    @Test
    fun `LOCAL_ASSET - Disney uses local resource fallback`() {
        val disneyProvider = TmdbProvider(id = 337, name = "Disney+", logoPath = null)
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = null,
            networks = null,
            providers = listOf(disneyProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Disney", result?.displayName)
        assertNotNull(result?.localResource)
    }

    @Test
    fun `DETERMINISM - Same input produces identical brand identity`() {
        val input = {
            ProductionCompanyRecognition.findBestCompany(
                productionCompanies = listOf(netflixStudio),
                networks = null,
                providers = listOf(netflixProvider),
                releaseNotes = null,
                mediaType = MediaType.MOVIE
            )
        }

        val first = input()
        val second = input()

        assertEquals(first, second)
        assertEquals(first?.logoUrl, second?.logoUrl)
    }

    @Test
    fun `UNRELATED - Unrelated studio does not hijack provider identity`() {
        val unrelatedStudio = TmdbCompany(id = 999, name = "Unrelated Studio", logoPath = "/unrelated.png", originCountry = "US")

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(unrelatedStudio),
            networks = null,
            providers = listOf(netflixProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        // Identity should still be Netflix (HIGH confidence)
        // Logo should still be Netflix provider icon (since unrelated studio is NOT recognized as Netflix)
        assertEquals("Netflix Original", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
        assertEquals("https://image.tmdb.org/t/p/w500/p69BYX927Y6pY686p69BYX927Y6.png", result?.logoUrl)
    }
}

