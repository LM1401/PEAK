package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionCompanyRecognitionTest {

    // PREMIER
    private val lucasfilm = TmdbCompany(id = 1, name = "Lucasfilm Ltd.", logoPath = "/lucas.png", originCountry = "US")
    private val disney = TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = "/disney.png", originCountry = "US")
    private val mgm = TmdbCompany(id = 21, name = "Metro-Goldwyn-Mayer", logoPath = "/mgm.png", originCountry = "US")
    private val fox20th = TmdbCompany(id = 25, name = "20th Century Fox", logoPath = "/fox.png", originCountry = "US")
    private val universal = TmdbCompany(id = 33, name = "Universal Pictures", logoPath = "/universal.png", originCountry = "US")
    private val warnerBros = TmdbCompany(id = 174, name = "Warner Bros. Pictures", logoPath = "/wb.png", originCountry = "US")
    private val marvel = TmdbCompany(id = 420, name = "Marvel Studios", logoPath = "/marvel.png", originCountry = "US")
    private val dc = TmdbCompany(id = 429, name = "DC", logoPath = "/dc.png", originCountry = "US")
    private val a24 = TmdbCompany(id = 41077, name = "A24", logoPath = "/a24.png", originCountry = "US")
    private val neon = TmdbCompany(id = 90733, name = "NEON", logoPath = "/neon.png", originCountry = "US")
    private val studios20th = TmdbCompany(id = 127928, name = "20th Century Studios", logoPath = "/20th.png", originCountry = "US")
    private val amazonMGM = TmdbCompany(id = 210099, name = "Amazon MGM Studios", logoPath = "/amzmGM.png", originCountry = "US")

    // MAJOR
    private val newLine = TmdbCompany(id = 12, name = "New Line Cinema", logoPath = "/newline.png", originCountry = "US")
    private val blumhouse = TmdbCompany(id = 3172, name = "Blumhouse Productions", logoPath = "/blumhouse.png", originCountry = "US")
    private val searchlight = TmdbCompany(id = 127929, name = "Searchlight Pictures", logoPath = "/searchlight.png", originCountry = "US")

    // RECOGNISED
    private val miramax = TmdbCompany(id = 14, name = "Miramax", logoPath = null, originCountry = "US")

    // NETWORKS
    private val fx = TmdbCompany(id = 88, name = "FX", logoPath = "/fx.png", originCountry = "US")
    private val amc = TmdbCompany(id = 174, name = "AMC", logoPath = "/amc.png", originCountry = "US")
    private val hbo = TmdbCompany(id = 49, name = "HBO", logoPath = "/hbo.png", originCountry = "US")
    private val hulu = TmdbCompany(id = 453, name = "Hulu", logoPath = "/hulu.png", originCountry = "US")
    private val primeVideo = TmdbCompany(id = 1024, name = "Prime Video", logoPath = "/prime.png", originCountry = "US")

    // UNKNOWN
    private val teaShop = TmdbCompany(id = 53139, name = "Tea Shop Productions", logoPath = "/teashop.png", originCountry = "GB")
    private val obscureCoProducer = TmdbCompany(id = 999999, name = "Obscure Co-Producer", logoPath = null, originCountry = "US")
    private val financingCo = TmdbCompany(id = 888888, name = "Financing Company", logoPath = null, originCountry = "US")
    private val unknownWithLogo = TmdbCompany(id = 777777, name = "Unknown With Logo", logoPath = "/logo.png", originCountry = "US")

    @Test
    fun `MOVIE - Star Wars resolves to Lucasfilm`() {
        val companies = listOf(lucasfilm)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Lucasfilm", result)
    }

    @Test
    fun `MOVIE - Challengers resolves to Amazon MGM Studios`() {
        val companies = listOf(obscureCoProducer, amazonMGM, financingCo)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Amazon MGM Studios", result)
    }

    @Test
    fun `MOVIE - Planet of the Apes resolves to 20th Century Studios`() {
        val companies = listOf(studios20th, obscureCoProducer)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("20th Century Studios", result)
    }

    @Test
    fun `MOVIE - Oppenheimer resolves to Universal`() {
        // Oppenheimer has Syncopy (9996) at index 0, Universal at index 1
        val syncopy = TmdbCompany(id = 9996, name = "Syncopy", logoPath = "/syncopy.png", originCountry = "GB")
        val companies = listOf(syncopy, universal)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Universal", result)
    }

    @Test
    fun `MOVIE - DC title resolves to DC`() {
        val companies = listOf(dc, warnerBros)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("DC", result) // DC has weight 100, WB has weight 100. WB is index 1.
    }

    @Test
    fun `MOVIE - Searchlight Pictures is recognised`() {
        val companies = listOf(searchlight)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Searchlight Pictures", result)
    }

    @Test
    fun `MOVIE - NEON is recognised`() {
        val companies = listOf(neon)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("NEON", result)
    }

    @Test
    fun `MOVIE - New Line Cinema is recognised`() {
        val companies = listOf(newLine)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("New Line Cinema", result)
    }

    @Test
    fun `MOVIE - A24 is recognised`() {
        val companies = listOf(a24)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("A24", result)
    }

    @Test
    fun `TV - Shogun resolves to FX`() {
        val networks = listOf(fx, hulu)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("FX", result)
    }

    @Test
    fun `TV - Better Call Saul resolves to AMC`() {
        val networks = listOf(amc)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("AMC", result)
    }

    @Test
    fun `TV - HBO is recognised`() {
        val networks = listOf(hbo)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("HBO", result)
    }

    @Test
    fun `TV - Prime Video is recognised`() {
        val networks = listOf(primeVideo)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("Prime Video", result)
    }

    @Test
    fun `QUALITY - Unknown company with logo is rejected`() {
        val companies = listOf(unknownWithLogo)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("", result)
    }

    @Test
    fun `QUALITY - Recognised company at later array position wins`() {
        val companies = listOf(obscureCoProducer, obscureCoProducer, blumhouse)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Blumhouse", result)
    }

    @Test
    fun `QUALITY - RECOGNISED tier company at later position still passes`() {
        // Miramax: 120 (RECOGNISED)
        // Index 3: -30
        // Score: 90. Threshold 80.
        val companies = listOf(obscureCoProducer, obscureCoProducer, obscureCoProducer, miramax)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Miramax", result)
    }

    @Test
    fun `QUALITY - Namespace isolation is enforced`() {
        // 174 is Warner Bros in companyIndex, but AMC in networkIndex.

        // Movie context (companyIndex)
        val movieResult = ProductionCompanyRecognition.findBestCompany(listOf(warnerBros), null, MediaType.MOVIE)
        assertEquals("Warner Bros.", movieResult)

        // TV context (networkIndex)
        val tvResult = ProductionCompanyRecognition.findBestCompany(null, listOf(amc), MediaType.TV)
        assertEquals("AMC", tvResult)

        // Network ID in Company list should NOT be recognised as Network
        // amc (174) in companies list -> resolves to Warner Bros.
        val wrongListResult = ProductionCompanyRecognition.findBestCompany(listOf(amc), null, MediaType.TV)
        assertEquals("Warner Bros.", wrongListResult)
    }

    @Test
    fun `QUALITY - ID collision remains isolated`() {
        // ID 4 is Paramount Pictures (Company) vs BBC One (Network - not in index)
        val bbcOne = TmdbCompany(id = 4, name = "BBC One", logoPath = "/bbc.png", originCountry = "GB")
        val paramount = TmdbCompany(id = 4, name = "Paramount Pictures", logoPath = "/para.png", originCountry = "US")

        // As Network -> Unknown (rejected)
        val networkResult = ProductionCompanyRecognition.findBestCompany(null, listOf(bbcOne), MediaType.TV)
        assertEquals("", networkResult)

        // As Company -> Paramount
        val companyResult = ProductionCompanyRecognition.findBestCompany(listOf(paramount), null, MediaType.TV)
        assertEquals("Paramount", companyResult)
    }

    @Test
    fun `MOVIE - Empty inputs return blank`() {
        val result = ProductionCompanyRecognition.findBestCompany(emptyList(), null, MediaType.MOVIE)
        assertEquals("", result)
    }
}
