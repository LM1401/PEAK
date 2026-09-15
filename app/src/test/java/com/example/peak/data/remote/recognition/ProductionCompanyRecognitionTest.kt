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
    private val sonyPicturesCompany = TmdbCompany(id = 34, name = "Sony Pictures", logoPath = "/logo_sony.png", originCountry = "US")
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
        val columbia = TmdbCompany(id = 5, name = "Columbia Pictures", logoPath = "/logo_columbia.png", originCountry = "US")
        assertEquals("Sony Pictures", ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, null, null, MediaType.MOVIE)?.displayName)
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(universal), null, null, null, MediaType.MOVIE)?.displayName)
        assertEquals("MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm), null, null, null, MediaType.MOVIE)?.displayName)
    }

    @Test
    fun `BRANDING - Propagation of TMDB logos for non-canonical brands`() {
        val result = ProductionCompanyRecognition.findBestCompany(listOf(sonyPicturesCompany), null, null, null, MediaType.MOVIE)
        assertNotNull(result?.logoUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/logo_sony.png", result?.logoUrl)
    }

    @Test
    fun `CANONICAL ASSETS - Canonical local assets take precedence and suppress TMDB remote logo override`() {
        // Netflix has localResource, so TMDB logo must be null and localResource must be non-null
        val netflixResult = ProductionCompanyRecognition.findBestCompany(null, null, listOf(netflixProvider), null, MediaType.MOVIE)
        assertEquals("Netflix Original", netflixResult?.displayName)
        assertNotNull(netflixResult?.localResource)
        assertNull(netflixResult?.logoUrl)

        // Prime Video has localResource, so TMDB logo must be null and localResource must be non-null
        val primeResult = ProductionCompanyRecognition.findBestCompany(null, null, listOf(primeProvider), null, MediaType.MOVIE)
        assertEquals("Prime Exclusive", primeResult?.displayName)
        assertNotNull(primeResult?.localResource)
        assertNull(primeResult?.logoUrl)
    }

    @Test
    fun `BRANDING - Provider without logo falls back correctly`() {
        val providerNoLogo = TmdbProvider(id = 8, name = "Netflix", logoPath = null)
        val result = ProductionCompanyRecognition.findBestCompany(null, null, listOf(providerNoLogo), null, MediaType.MOVIE)
        
        assertEquals("Netflix Original", result?.displayName)
        assertNull(result?.logoUrl)
        assertNotNull(result?.localResource)
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
    fun `CANONICAL ASSETS - All curated brands with localResource return non-null localResource and null logoUrl`() {
        val testCases = listOf(
            Triple("Netflix Original", null, listOf(TmdbProvider(id = 8, name = "Netflix", logoPath = "/logo.png"))),
            Triple("Prime Exclusive", null, listOf(TmdbProvider(id = 119, name = "Prime Video", logoPath = "/logo.png"))),
            Triple("Disney", listOf(TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = "/logo.png", originCountry = "US")), null),
            Triple("Apple TV+", null, listOf(TmdbProvider(id = 350, name = "Apple TV+", logoPath = "/logo.png"))),
            Triple("Paramount+", null, listOf(TmdbProvider(id = 531, name = "Paramount+", logoPath = "/logo.png"))),
            Triple("MGM+", null, listOf(TmdbProvider(id = 34, name = "MGM+", logoPath = "/logo.png"))),
            Triple("Hulu", null, listOf(TmdbProvider(id = 15, name = "Hulu", logoPath = "/logo.png"))),
            Triple("Warner Bros.", listOf(TmdbCompany(id = 174, name = "Warner Bros.", logoPath = "/logo.png", originCountry = "US")), null),
            Triple("Universal", listOf(TmdbCompany(id = 33, name = "Universal Pictures", logoPath = "/logo.png", originCountry = "US")), null),
            Triple("Blumhouse", listOf(TmdbCompany(id = 3172, name = "Blumhouse", logoPath = "/logo.png", originCountry = "US")), null)
        )

        testCases.forEach { (expectedName, companies, providers) ->
            val result = ProductionCompanyRecognition.findBestCompany(
                productionCompanies = companies,
                networks = null,
                providers = providers,
                releaseNotes = null,
                mediaType = MediaType.TV
            )
            assertNotNull("Failed to resolve brand for $expectedName", result)
            assertEquals(expectedName, result?.displayName)
            assertNotNull("Canonical localResource missing for $expectedName", result?.localResource)
            assertNull("TMDB logoUrl should not override canonical local asset for $expectedName", result?.logoUrl)
        }
    }

    @Test
    fun `CANONICAL ASSETS - localResource preserved when multiple candidates merged`() {
        // Multiple candidates for Disney (studio + provider)
        val disneyStudio = TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = "/disney.png", originCountry = "US")
        val disneyProvider = TmdbProvider(id = 337, name = "Disney+", logoPath = "/disney_plus.png")

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(disneyStudio),
            networks = null,
            providers = listOf(disneyProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Disney", result?.displayName)
        assertNotNull(result?.localResource)
        assertNull(result?.logoUrl)
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
        assertEquals(first?.localResource, second?.localResource)
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
        // localResource should be Netflix local resource, logoUrl null
        assertEquals("Netflix Original", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
        assertNotNull(result?.localResource)
        assertNull(result?.logoUrl)
    }
}
