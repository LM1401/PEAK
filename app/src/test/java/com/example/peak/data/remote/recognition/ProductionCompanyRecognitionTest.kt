package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductionCompanyRecognitionTest {

    // PREMIER
    private val a24 = TmdbCompany(id = 41077, name = "A24", logoPath = "/a24.png", originCountry = "US")
    private val hbo = TmdbCompany(id = 49, name = "HBO", logoPath = "/hbo.png", originCountry = "US")
    private val disney = TmdbCompany(id = 2, name = "Walt Disney Pictures", logoPath = "/disney.png", originCountry = "US")
    
    // MAJOR
    private val blumhouse = TmdbCompany(id = 3172, name = "Blumhouse Productions", logoPath = "/blumhouse.png", originCountry = "US")
    
    // UNKNOWN
    private val teaShop = TmdbCompany(id = 53139, name = "Tea Shop Productions", logoPath = "/teashop.png", originCountry = "GB")
    private val obscureCoProducer = TmdbCompany(id = 999999, name = "Obscure Co-Producer", logoPath = null, originCountry = "US")
    private val unknownWithLogo = TmdbCompany(id = 888888, name = "Unknown With Logo", logoPath = "/logo.png", originCountry = "US")

    @Test
    fun `MOVIE - A24 wins even when not first`() {
        val companies = listOf(obscureCoProducer, a24)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("A24", result)
    }

    @Test
    fun `MOVIE - Blumhouse wins over Tea Shop despite Tea Shop having a logo`() {
        // Obsession scenario
        val companies = listOf(teaShop, blumhouse, obscureCoProducer)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Blumhouse", result)
    }

    @Test
    fun `MOVIE - Recognised company without logo beats unknown with logo`() {
        val blumhouseNoLogo = blumhouse.copy(logoPath = null)
        val companies = listOf(unknownWithLogo, blumhouseNoLogo)
        
        // Blumhouse: 500 (MAJOR) vs Unknown With Logo: 50 (logo)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Blumhouse", result)
    }

    @Test
    fun `MOVIE - Unknown company with logo does NOT pass threshold`() {
        val companies = listOf(unknownWithLogo) 
        // Score: 50 (logo). Threshold is 80.
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("", result)
    }

    @Test
    fun `MOVIE - Multiple recognised companies resolved by tier`() {
        val companies = listOf(blumhouse, disney) // MAJOR (500) vs PREMIER (1000)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Disney", result)
    }

    @Test
    fun `MOVIE - Empty list returns blank`() {
        val result = ProductionCompanyRecognition.findBestCompany(emptyList(), null, MediaType.MOVIE)
        assertEquals("", result)
    }

    @Test
    fun `TV - Recognised network wins over recognised production company`() {
        val networks = listOf(hbo)
        val companies = listOf(blumhouse)
        
        // HBO: 1000 (PREMIER) + 200 (Boost) + 50 (Logo) = 1250
        // Blumhouse: 500 (MAJOR) + 50 (Logo) = 550
        val result = ProductionCompanyRecognition.findBestCompany(companies, networks, MediaType.TV)
        assertEquals("HBO", result)
    }

    @Test
    fun `TV - Unknown network with logo does NOT pass threshold`() {
        val networks = listOf(unknownWithLogo)
        // Score: 50 (Logo). No Boost because it's Unknown. Threshold 80.
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("", result)
    }

    @Test
    fun `TV - Recognised production company wins if network is unknown`() {
        val networks = listOf(unknownWithLogo)
        val companies = listOf(a24)
        
        // Network: 50 (Logo)
        // A24: 1000 (PREMIER) + 50 (Logo) = 1050
        val result = ProductionCompanyRecognition.findBestCompany(companies, networks, MediaType.TV)
        assertEquals("A24", result)
    }

    @Test
    fun `TV - Returns empty if nothing passes threshold`() {
        val networks = listOf(obscureCoProducer)
        val companies = listOf(teaShop)
        val result = ProductionCompanyRecognition.findBestCompany(companies, networks, MediaType.TV)
        assertEquals("", result)
    }
}
