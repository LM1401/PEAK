package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionCompanyRecognitionTest {

    // PREMIER
    private val lucasfilm = TmdbCompany(id = 1, name = "Lucasfilm Ltd.", logoPath = null, originCountry = "US")
    private val columbia = TmdbCompany(id = 5, name = "Columbia Pictures", logoPath = null, originCountry = "US")
    private val mgm = TmdbCompany(id = 21, name = "Metro-Goldwyn-Mayer", logoPath = null, originCountry = "US")
    private val fox20th = TmdbCompany(id = 25, name = "20th Century Fox", logoPath = null, originCountry = "US")
    private val universal = TmdbCompany(id = 33, name = "Universal Pictures", logoPath = null, originCountry = "US")
    private val warnerBros = TmdbCompany(id = 174, name = "Warner Bros. Pictures", logoPath = null, originCountry = "US")
    private val netflixCompany = TmdbCompany(id = 178464, name = "Netflix", logoPath = null, originCountry = "US")
    private val a24 = TmdbCompany(id = 41077, name = "A24", logoPath = null, originCountry = "US")
    private val appleStudios = TmdbCompany(id = 194232, name = "Apple Studios", logoPath = null, originCountry = "US")
    private val amazonMGM = TmdbCompany(id = 210099, name = "Amazon MGM Studios", logoPath = null, originCountry = "US")

    // MAJOR
    private val lionsgate = TmdbCompany(id = 1632, name = "Lionsgate", logoPath = null, originCountry = "US")
    private val natGeo = TmdbCompany(id = 43, name = "National Geographic", logoPath = null, originCountry = "US")

    // NETWORKS
    private val amc = TmdbCompany(id = 174, name = "AMC", logoPath = null, originCountry = "US")
    private val netflixNetwork = TmdbCompany(id = 213, name = "Netflix", logoPath = null, originCountry = "US")
    private val primeVideo = TmdbCompany(id = 1024, name = "Prime Video", logoPath = null, originCountry = "US")

    // OBSCURE
    private val obscureCoProducer = TmdbCompany(id = 999999, name = "Obscure Co-Producer", logoPath = "/logo.png", originCountry = "US")

    @Test
    fun `BASIC - Recognition of whitelisted studios`() {
        assertEquals("Lucasfilm", ProductionCompanyRecognition.findBestCompany(listOf(lucasfilm), null, MediaType.MOVIE))
        assertEquals("A24", ProductionCompanyRecognition.findBestCompany(listOf(a24), null, MediaType.MOVIE))
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(universal), null, MediaType.MOVIE))
        assertEquals("Lionsgate", ProductionCompanyRecognition.findBestCompany(listOf(lionsgate), null, MediaType.MOVIE))
    }

    @Test
    fun `BRANDING - Resolution of display names`() {
        assertEquals("Netflix Original", ProductionCompanyRecognition.findBestCompany(listOf(netflixCompany), null, MediaType.MOVIE))
        assertEquals("Netflix Original", ProductionCompanyRecognition.findBestCompany(null, listOf(netflixNetwork), MediaType.TV))
        assertEquals("Prime Exclusive", ProductionCompanyRecognition.findBestCompany(null, listOf(primeVideo), MediaType.TV))
        assertEquals("Apple TV+", ProductionCompanyRecognition.findBestCompany(listOf(appleStudios), null, MediaType.MOVIE))
        assertEquals("Sony Pictures", ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, MediaType.MOVIE))
        assertEquals("MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm), null, MediaType.MOVIE))
    }

    @Test
    fun `WHITELIST - Obscure companies are ignored`() {
        assertEquals("", ProductionCompanyRecognition.findBestCompany(listOf(obscureCoProducer), null, MediaType.MOVIE))
        // Obscure first, whitelisted second
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(obscureCoProducer, universal), null, MediaType.MOVIE))
    }

    @Test
    fun `TIER - Premier beats Major`() {
        // Lionsgate (MAJOR) vs Universal (PREMIER)
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(lionsgate, universal), null, MediaType.MOVIE))
    }

    @Test
    fun `POSITION - First recognized entity wins if tiers are equal`() {
        // Disney (PREMIER) vs Universal (PREMIER)
        val disney = TmdbCompany(id = 2, name = "Disney", logoPath = null, originCountry = "US")
        assertEquals("Disney", ProductionCompanyRecognition.findBestCompany(listOf(disney, universal), null, MediaType.MOVIE))
        assertEquals("Universal", ProductionCompanyRecognition.findBestCompany(listOf(universal, disney), null, MediaType.MOVIE))
    }

    @Test
    fun `NAMESPACE - Isolation of ID 174`() {
        // Movie Context -> Warner Bros.
        assertEquals("Warner Bros.", ProductionCompanyRecognition.findBestCompany(listOf(warnerBros), null, MediaType.MOVIE))
        // TV Context (Networks) -> AMC
        assertEquals("AMC", ProductionCompanyRecognition.findBestCompany(null, listOf(amc), MediaType.TV))
    }

    @Test
    fun `PRIORITY - Amazon MGM Studios beats MGM within Premier tier`() {
        // Challengers: MGM (21) and Amazon MGM (210099)
        // Even if MGM is first, Amazon MGM should win due to priority=1
        assertEquals("Amazon MGM Studios", ProductionCompanyRecognition.findBestCompany(listOf(mgm, amazonMGM), null, MediaType.MOVIE))
    }

    @Test
    fun `REGRESSION - Mutiny resolves to Lionsgate`() {
        val syncopy = TmdbCompany(id = 9996, name = "Syncopy", logoPath = null, originCountry = "GB")
        assertEquals("Lionsgate", ProductionCompanyRecognition.findBestCompany(listOf(syncopy, lionsgate), null, MediaType.MOVIE))
    }

    @Test
    fun `REGRESSION - Facing El Chapo resolves to National Geographic`() {
        assertEquals("National Geographic", ProductionCompanyRecognition.findBestCompany(listOf(natGeo), null, MediaType.TV))
    }

    @Test
    fun `REGRESSION - Good Omens resolves to Prime Exclusive`() {
        // TV show: Amazon Studios (Company) + Prime Video (Network)
        val amazonStudios = TmdbCompany(id = 20580, name = "Amazon Studios", logoPath = null, originCountry = "US")
        assertEquals("Prime Exclusive", ProductionCompanyRecognition.findBestCompany(listOf(amazonStudios), listOf(primeVideo), MediaType.TV))
    }

    @Test
    fun `REGRESSION - Venom resolves to Sony Pictures`() {
        assertEquals("Sony Pictures", ProductionCompanyRecognition.findBestCompany(listOf(columbia), null, MediaType.MOVIE))
    }

    @Test
    fun `REGRESSION - X-Men resolves to 20th Century Fox`() {
        assertEquals("20th Century Fox", ProductionCompanyRecognition.findBestCompany(listOf(fox20th), null, MediaType.MOVIE))
    }
}
