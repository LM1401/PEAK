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
    private val mgm = TmdbCompany(id = 21, name = "Metro-Goldwyn-Mayer", logoPath = "/mgm.png", originCountry = "US")
    private val lucasfilm = TmdbCompany(id = 1, name = "Lucasfilm Ltd.", logoPath = "/lucas.png", originCountry = "US")
    private val fox20th = TmdbCompany(id = 25, name = "20th Century Fox", logoPath = "/fox.png", originCountry = "US")
    private val studios20th = TmdbCompany(id = 12792, name = "20th Century Studios", logoPath = "/20th.png", originCountry = "US")
    
    // MAJOR
    private val blumhouse = TmdbCompany(id = 3172, name = "Blumhouse Productions", logoPath = "/blumhouse.png", originCountry = "US")
    private val newLine = TmdbCompany(id = 12, name = "New Line Cinema", logoPath = "/newline.png", originCountry = "US")
    
    // RECOGNISED
    private val miramax = TmdbCompany(id = 14, name = "Miramax", logoPath = null, originCountry = "US")

    // TV NETWORKS
    private val fx = TmdbCompany(id = 30, name = "FX", logoPath = "/fx.png", originCountry = "US")
    private val amc = TmdbCompany(id = 3394, name = "AMC", logoPath = "/amc.png", originCountry = "US")
    private val hulu = TmdbCompany(id = 453, name = "Hulu", logoPath = "/hulu.png", originCountry = "US")
    private val amazonNetwork = TmdbCompany(id = 1024, name = "Amazon", logoPath = "/amazon.png", originCountry = "US")
    private val amazonStudios = TmdbCompany(id = 20580, name = "Amazon Studios", logoPath = "/amazon_studios.png", originCountry = "US")

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
    fun `MOVIE - MGM is recognised`() {
        val companies = listOf(mgm)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("MGM", result)
    }

    @Test
    fun `MOVIE - Lucasfilm is recognised`() {
        val companies = listOf(lucasfilm)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Lucasfilm", result)
    }

    @Test
    fun `MOVIE - 20th Century Studios and Fox are recognised`() {
        assertEquals("20th Century Studios", ProductionCompanyRecognition.findBestCompany(listOf(studios20th), null, MediaType.MOVIE))
        assertEquals("20th Century Fox", ProductionCompanyRecognition.findBestCompany(listOf(fox20th), null, MediaType.MOVIE))
    }

    @Test
    fun `MOVIE - New Line Cinema is recognised`() {
        val companies = listOf(newLine)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("New Line Cinema", result)
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
    fun `MOVIE - RECOGNISED company at later array position without logo still passes`() {
        // Miramax: 120 (RECOGNISED)
        // Index 3: -30
        // Score: 90. Threshold 80.
        val companies = listOf(obscureCoProducer, obscureCoProducer, obscureCoProducer, miramax)
        val result = ProductionCompanyRecognition.findBestCompany(companies, null, MediaType.MOVIE)
        assertEquals("Miramax", result)
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
    fun `TV - FX is recognised in TV context`() {
        val networks = listOf(fx)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("FX", result)
    }

    @Test
    fun `TV - AMC is recognised in TV context`() {
        val networks = listOf(amc)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("AMC", result)
    }

    @Test
    fun `TV - Hulu is recognised in TV context`() {
        val networks = listOf(hulu)
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("Hulu", result)
    }

    @Test
    fun `TV - Amazon network is recognised independently from Amazon Studios`() {
        // Amazon network (1024)
        val result1 = ProductionCompanyRecognition.findBestCompany(null, listOf(amazonNetwork), MediaType.TV)
        assertEquals("Amazon", result1)
        
        // Amazon Studios (20580) as production company
        val result2 = ProductionCompanyRecognition.findBestCompany(listOf(amazonStudios), null, MediaType.TV)
        assertEquals("Amazon Studios", result2)
    }

    @Test
    fun `TV - Namespace collision is impossible`() {
        // ID 49 is HBO in networkIndex, but doesn't exist in companyIndex.
        
        // As network -> HBO
        val result1 = ProductionCompanyRecognition.findBestCompany(null, listOf(hbo), MediaType.TV)
        assertEquals("HBO", result1)
        
        // As production company -> Unknown (score 0, or 50 with logo)
        val result2 = ProductionCompanyRecognition.findBestCompany(listOf(hbo), null, MediaType.TV)
        assertEquals("", result2) // HBO is not in companyIndex
    }

    @Test
    fun `TV - Recognised network wins over recognised production company`() {
        val networks = listOf(hbo)
        val companies = listOf(blumhouse)
        
        // HBO (Network): 1000 (PREMIER) + 200 (Boost) + 50 (Logo) = 1250
        // Blumhouse (Company): 500 (MAJOR) + 50 (Logo) = 550
        val result = ProductionCompanyRecognition.findBestCompany(companies, networks, MediaType.TV)
        assertEquals("HBO", result)
    }

    @Test
    fun `TV - Unknown network with logo does NOT pass threshold`() {
        val networks = listOf(unknownWithLogo)
        // Score: 50 (Logo). No Boost because it's Unknown in networkIndex. Threshold 80.
        val result = ProductionCompanyRecognition.findBestCompany(null, networks, MediaType.TV)
        assertEquals("", result)
    }

    @Test
    fun `MOVIE - Empty inputs return blank`() {
        val result = ProductionCompanyRecognition.findBestCompany(emptyList(), null, MediaType.MOVIE)
        assertEquals("", result)
    }
}
