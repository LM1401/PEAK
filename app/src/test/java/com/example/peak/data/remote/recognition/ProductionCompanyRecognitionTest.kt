package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.data.remote.dto.TmdbProvider
import com.example.peak.domain.model.BrandConfidence
import com.example.peak.domain.model.BrandPresentationStyle
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.formatBrandingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verification of the PEAK Branding Resolver and Presentation Formatting.
 * Tests focus on deterministic mapping of TMDB entities to PEAK display brands and presentation text.
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

    // EXTRA COMPANIES FOR DISPLAY TESTS
    private val netflixStudio = TmdbCompany(id = 178464, name = "Netflix Studios", logoPath = "/netflix_wordmark.png", originCountry = "US")


    @Test
    fun `BRANDING - Resolution of curated PEAK brands`() {
        val columbia = TmdbCompany(id = 5, name = "Columbia Pictures", logoPath = "/logo_columbia.png", originCountry = "US")
        assertEquals("Sony Pictures", ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, null, null, MediaType.MOVIE)?.displayName)
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(universal), null, null, null, MediaType.MOVIE)?.displayName)
        assertEquals("MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm), null, null, null, MediaType.MOVIE)?.displayName)
    }

    @Test
    fun `BRANDING - Resolution of Sony Pictures company`() {
        val result = ProductionCompanyRecognition.findBestCompany(listOf(sonyPicturesCompany), null, null, null, MediaType.MOVIE)
        assertNotNull(result)
        assertEquals("Sony Pictures", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
    }

    @Test
    fun `TEXT PRESENTATION - Formats brand names into authentic presentation text`() {
        val netflixResult = ProductionCompanyRecognition.findBestCompany(null, null, listOf(netflixProvider), null, MediaType.MOVIE)
        assertEquals("A Netflix Original", netflixResult?.formatPresentationText())

        val blumhouse = TmdbCompany(id = 3172, name = "Blumhouse", logoPath = null, originCountry = "US")
        val blumhouseResult = ProductionCompanyRecognition.findBestCompany(listOf(blumhouse), null, null, null, MediaType.MOVIE)
        assertEquals("A Blumhouse Production", blumhouseResult?.formatPresentationText())

        val disneyStudio = TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = null, originCountry = "US")
        val disneyResult = ProductionCompanyRecognition.findBestCompany(listOf(disneyStudio), null, null, null, MediaType.MOVIE)
        assertEquals("A Disney Production", disneyResult?.formatPresentationText())

        val universalResult = ProductionCompanyRecognition.findBestCompany(listOf(universal), null, null, null, MediaType.MOVIE)
        assertEquals("Universal Pictures Presents", universalResult?.formatPresentationText())

        val warnerCompany = TmdbCompany(id = 174, name = "Warner Bros.", logoPath = null, originCountry = "US")
        val warnerResult = ProductionCompanyRecognition.findBestCompany(listOf(warnerCompany), null, null, null, MediaType.MOVIE)
        assertEquals("Warner Bros. Pictures Presents", warnerResult?.formatPresentationText())

        val appleProvider = TmdbProvider(id = 350, name = "Apple TV+", logoPath = null)
        val appleResult = ProductionCompanyRecognition.findBestCompany(null, null, listOf(appleProvider), null, MediaType.MOVIE)
        assertEquals("An Apple Original", appleResult?.formatPresentationText())

        val a24Company = TmdbCompany(id = 41077, name = "A24", logoPath = null, originCountry = "US")
        val a24Result = ProductionCompanyRecognition.findBestCompany(listOf(a24Company), null, null, null, MediaType.MOVIE)
        assertEquals("A24 Presents", a24Result?.formatPresentationText())
    }

    @Test
    fun `GRAMMAR & PHONETIC ARTICLES - Indefinite articles work correctly`() {
        assertEquals("An Apple Original", formatBrandingText("Apple", BrandPresentationStyle.ORIGINAL))
        assertEquals("An HBO Original", formatBrandingText("HBO", BrandPresentationStyle.ORIGINAL))
        assertEquals("An AMC Original", formatBrandingText("AMC", BrandPresentationStyle.ORIGINAL))
        assertEquals("An FX Original", formatBrandingText("FX", BrandPresentationStyle.ORIGINAL))
        assertEquals("An ITV Production", formatBrandingText("ITV", BrandPresentationStyle.PRODUCTION))
        assertEquals("An MGM+ Original", formatBrandingText("MGM+", BrandPresentationStyle.ORIGINAL))
        assertEquals("A Blumhouse Production", formatBrandingText("Blumhouse", BrandPresentationStyle.PRODUCTION))
        assertEquals("A Disney Production", formatBrandingText("Disney", BrandPresentationStyle.PRODUCTION))
        assertEquals("Universal Pictures Presents", formatBrandingText("Universal Pictures", BrandPresentationStyle.PRESENTS))
    }

    @Test
    fun `AMAZON DUAL IDENTITY - Presentation metadata distinguishes company vs provider`() {
        // Provider Mode (Prime Video Provider)
        val providerResult = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = null,
            networks = null,
            providers = listOf(primeProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )
        assertEquals("Prime Exclusive", providerResult?.displayName)
        assertEquals("A Prime Exclusive", providerResult?.formatPresentationText())

        // Company Mode (Amazon Studios)
        val amazonStudios = TmdbCompany(id = 20580, name = "Amazon Studios", logoPath = null, originCountry = "US")
        val companyResult = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(amazonStudios),
            networks = null,
            providers = null,
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )
        assertEquals("Prime Video", companyResult?.displayName)
        assertEquals("A Prime Video Original", companyResult?.formatPresentationText())
    }

    @Test
    fun `DEFENSIVE FORMATTING - Prevents duplicate suffixes on legacy or pre-formatted strings`() {
        assertEquals("Skydance Pictures Presents", formatBrandingText("Skydance Pictures Presents", BrandPresentationStyle.PRODUCTION))
        assertEquals("A Netflix Original", formatBrandingText("Netflix Original", BrandPresentationStyle.ORIGINAL))
        assertEquals("A Blumhouse Production", formatBrandingText("A Blumhouse Production", BrandPresentationStyle.PRODUCTION))
    }

    @Test
    fun `CATALOG COVERAGE - All 34 catalog brands have valid presentation metadata`() {
        assertEquals(34, BrandCatalog.brands.size)
        BrandCatalog.brands.forEach { brandDef ->
            val companyName = brandDef.getPresentationName(isNetwork = false)
            val networkName = brandDef.getPresentationName(isNetwork = true)
            
            assertTrue("Brand ${brandDef.key} company presentation name must not be blank", companyName.isNotBlank())
            assertTrue("Brand ${brandDef.key} network presentation name must not be blank", networkName.isNotBlank())

            val companyText = formatBrandingText(companyName, brandDef.getPresentationStyle(isNetwork = false))
            val networkText = formatBrandingText(networkName, brandDef.getPresentationStyle(isNetwork = true))

            assertTrue("Brand ${brandDef.key} company formatted presentation must not be blank", companyText.isNotBlank())
            assertTrue("Brand ${brandDef.key} network formatted presentation must not be blank", networkText.isNotBlank())
        }
    }

    @Test
    fun `CONFIDENCE - Unknown company is NOT displayable`() {
        val unknown = TmdbCompany(id = 999, name = "Unknown Studio", logoPath = null, originCountry = "US")
        val result = ProductionCompanyRecognition.findBestCompany(listOf(unknown), null, null, null, MediaType.MOVIE)
        assertNull(result)
    }

    @Test
    fun `PRIORITY - Provider beats network and company`() {
        val amazonStudios = TmdbCompany(id = 20580, name = "Amazon Studios", logoPath = null, originCountry = "US")

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
        val warner = TmdbCompany(id = 174, name = "Warner Bros.", logoPath = null, originCountry = "US")
        assertEquals("Warner Bros.", ProductionCompanyRecognition.findBestCompany(listOf(warner), null, null, null, MediaType.MOVIE)?.displayName)

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
        assertEquals("A Netflix Original", result?.formatPresentationText())
    }

    @Test
    fun `MUTINY - Verification for TMDB ID 1288445`() {
        val punchPalace = TmdbCompany(id = 218150, name = "Punch Palace Productions", logoPath = null, originCountry = "US")
        val madRiver = TmdbCompany(id = 73492, name = "MadRiver Pictures", logoPath = null, originCountry = "US")
        
        val resultNoNotes = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(punchPalace, madRiver),
            networks = null,
            providers = null,
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )
        assertNull(resultNoNotes)

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
        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(amazonMGM),
            networks = null,
            providers = listOf(primeProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Amazon MGM Studios", result?.displayName)
    }

    @Test
    fun `RECOGNITION - All curated brands resolve correctly`() {
        val testCases = listOf(
            Triple("Netflix Original", null, listOf(TmdbProvider(id = 8, name = "Netflix", logoPath = null))),
            Triple("Prime Exclusive", null, listOf(TmdbProvider(id = 119, name = "Prime Video", logoPath = null))),
            Triple("Disney", listOf(TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = null, originCountry = "US")), null),
            Triple("Apple TV+", null, listOf(TmdbProvider(id = 350, name = "Apple TV+", logoPath = null))),
            Triple("Paramount+", null, listOf(TmdbProvider(id = 531, name = "Paramount+", logoPath = null))),
            Triple("MGM+", null, listOf(TmdbProvider(id = 34, name = "MGM+", logoPath = null))),
            Triple("Hulu", null, listOf(TmdbProvider(id = 15, name = "Hulu", logoPath = null))),
            Triple("Warner Bros.", listOf(TmdbCompany(id = 174, name = "Warner Bros.", logoPath = null, originCountry = "US")), null),
            Triple("Universal", listOf(TmdbCompany(id = 33, name = "Universal Pictures", logoPath = null, originCountry = "US")), null),
            Triple("Blumhouse", listOf(TmdbCompany(id = 3172, name = "Blumhouse", logoPath = null, originCountry = "US")), null)
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
        }
    }

    @Test
    fun `MERGING - Preserves highest priority identity when multiple candidates merged`() {
        val disneyStudio = TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = null, originCountry = "US")
        val disneyProvider = TmdbProvider(id = 337, name = "Disney+", logoPath = null)

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(disneyStudio),
            networks = null,
            providers = listOf(disneyProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Disney", result?.displayName)
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
    }

    @Test
    fun `UNRELATED - Unrelated studio does not hijack provider identity`() {
        val unrelatedStudio = TmdbCompany(id = 999, name = "Unrelated Studio", logoPath = null, originCountry = "US")

        val result = ProductionCompanyRecognition.findBestCompany(
            productionCompanies = listOf(unrelatedStudio),
            networks = null,
            providers = listOf(netflixProvider),
            releaseNotes = null,
            mediaType = MediaType.MOVIE
        )

        assertEquals("Netflix Original", result?.displayName)
        assertEquals(BrandConfidence.HIGH, result?.confidence)
    }
}
