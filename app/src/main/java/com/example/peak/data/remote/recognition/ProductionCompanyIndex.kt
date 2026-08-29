package com.example.peak.data.remote.recognition

/**
 * Categorization of production entities to help the recognition system
 * determine context-appropriate branding.
 */
enum class CompanyType {
    STUDIO,   // Primarily film production
    NETWORK,  // Primarily television network / broadcaster
    BOTH      // Major entities active in both (e.g. Disney, Netflix)
}

/**
 * Tiered scoring system for production companies.
 * PREMIER: Top-tier global brands (e.g., A24, Disney, HBO).
 * MAJOR: Large, well-known production companies (e.g., Blumhouse).
 * RECOGNISED: Established studios with moderate brand recognition.
 */
enum class CompanyTier(val baseScore: Int) {
    PREMIER(1000),
    MAJOR(500),
    RECOGNISED(100),
    UNKNOWN(0)
}

data class RecognisedCompany(
    val id: Int,
    val name: String,
    val tier: CompanyTier,
    val type: CompanyType
)

/**
 * Reusable knowledge about recognised production companies and networks.
 * IDs are verified TMDB internal identifiers.
 */
object ProductionCompanyIndex {
    private val index = mapOf(
        // PREMIER STUDIOS & NETWORKS
        2 to RecognisedCompany(2, "Disney", CompanyTier.PREMIER, CompanyType.BOTH),
        174 to RecognisedCompany(174, "Warner Bros.", CompanyTier.PREMIER, CompanyType.STUDIO),
        33 to RecognisedCompany(33, "Universal", CompanyTier.PREMIER, CompanyType.STUDIO),
        4 to RecognisedCompany(4, "Paramount", CompanyTier.PREMIER, CompanyType.STUDIO),
        5 to RecognisedCompany(5, "Columbia Pictures", CompanyTier.PREMIER, CompanyType.STUDIO),
        41077 to RecognisedCompany(41077, "A24", CompanyTier.PREMIER, CompanyType.STUDIO),
        420 to RecognisedCompany(420, "Marvel Studios", CompanyTier.PREMIER, CompanyType.STUDIO),
        3 to RecognisedCompany(3, "Pixar", CompanyTier.PREMIER, CompanyType.STUDIO),
        213 to RecognisedCompany(213, "Netflix", CompanyTier.PREMIER, CompanyType.BOTH),
        49 to RecognisedCompany(49, "HBO", CompanyTier.PREMIER, CompanyType.NETWORK),
        2552 to RecognisedCompany(2552, "Apple TV+", CompanyTier.PREMIER, CompanyType.BOTH),
        20580 to RecognisedCompany(20580, "Amazon Studios", CompanyTier.PREMIER, CompanyType.BOTH),
        
        // MAJOR PRODUCTION COMPANIES
        3172 to RecognisedCompany(3172, "Blumhouse", CompanyTier.MAJOR, CompanyType.STUDIO),
        1632 to RecognisedCompany(1632, "Lionsgate", CompanyTier.MAJOR, CompanyType.STUDIO),
        923 to RecognisedCompany(923, "Legendary", CompanyTier.MAJOR, CompanyType.STUDIO),
        521 to RecognisedCompany(521, "DreamWorks", CompanyTier.MAJOR, CompanyType.STUDIO),
        13240 to RecognisedCompany(13240, "Searchlight Pictures", CompanyTier.MAJOR, CompanyType.STUDIO),
        
        // RECOGNISED STUDIOS
        7 to RecognisedCompany(7, "DreamWorks Animation", CompanyTier.RECOGNISED, CompanyType.STUDIO),
        34 to RecognisedCompany(34, "Sony Pictures", CompanyTier.RECOGNISED, CompanyType.STUDIO)
    )

    fun getRecognisedCompany(id: Int): RecognisedCompany? = index[id]
}
