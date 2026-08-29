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
 * Tiered priority system for production companies.
 * PREMIER: Top-tier global brands (e.g., A24, Disney, HBO).
 * MAJOR: Large, well-known production companies (e.g., Blumhouse).
 * RECOGNISED: Established studios with moderate brand recognition.
 */
enum class CompanyTier(val priority: Int) {
    PREMIER(3),
    MAJOR(2),
    RECOGNISED(1),
    UNKNOWN(0)
}

data class RecognisedCompany(
    val id: Int,
    val name: String,
    val tier: CompanyTier,
    val type: CompanyType,
    val brandingName: String? = null,
    val priority: Int = 0 // Tie-breaker within Tier
)

/**
 * Curated PEAK display-brand whitelist.
 * IDs are verified TMDB internal identifiers.
 */
object ProductionCompanyIndex {
    private val companyIndex = mapOf(
        // PREMIER STUDIOS
        1 to RecognisedCompany(1, "Lucasfilm", CompanyTier.PREMIER, CompanyType.STUDIO),
        2 to RecognisedCompany(2, "Disney", CompanyTier.PREMIER, CompanyType.BOTH),
        3 to RecognisedCompany(3, "Pixar", CompanyTier.PREMIER, CompanyType.STUDIO),
        4 to RecognisedCompany(4, "Paramount", CompanyTier.PREMIER, CompanyType.STUDIO),
        5 to RecognisedCompany(5, "Columbia Pictures", CompanyTier.PREMIER, CompanyType.STUDIO, brandingName = "Sony Pictures"),
        21 to RecognisedCompany(21, "MGM", CompanyTier.PREMIER, CompanyType.STUDIO, brandingName = "MGM Studios"),
        25 to RecognisedCompany(25, "20th Century Fox", CompanyTier.PREMIER, CompanyType.STUDIO),
        33 to RecognisedCompany(33, "Universal", CompanyTier.PREMIER, CompanyType.STUDIO),
        174 to RecognisedCompany(174, "Warner Bros.", CompanyTier.PREMIER, CompanyType.STUDIO),
        420 to RecognisedCompany(420, "Marvel Studios", CompanyTier.PREMIER, CompanyType.STUDIO),
        429 to RecognisedCompany(429, "DC", CompanyTier.PREMIER, CompanyType.STUDIO),
        178464 to RecognisedCompany(178464, "Netflix", CompanyTier.PREMIER, CompanyType.BOTH, brandingName = "Netflix Original"),
        184898 to RecognisedCompany(184898, "DC Studios", CompanyTier.PREMIER, CompanyType.STUDIO),
        194232 to RecognisedCompany(194232, "Apple Studios", CompanyTier.PREMIER, CompanyType.STUDIO, brandingName = "Apple TV+"),
        210099 to RecognisedCompany(210099, "Amazon MGM Studios", CompanyTier.PREMIER, CompanyType.STUDIO, priority = 1),
        41077 to RecognisedCompany(41077, "A24", CompanyTier.PREMIER, CompanyType.STUDIO),
        90733 to RecognisedCompany(90733, "NEON", CompanyTier.PREMIER, CompanyType.STUDIO),
        127928 to RecognisedCompany(127928, "20th Century Studios", CompanyTier.PREMIER, CompanyType.STUDIO),
        20580 to RecognisedCompany(20580, "Amazon Studios", CompanyTier.PREMIER, CompanyType.BOTH),
        
        // MAJOR PRODUCTION COMPANIES
        12 to RecognisedCompany(12, "New Line Cinema", CompanyTier.MAJOR, CompanyType.STUDIO),
        521 to RecognisedCompany(521, "DreamWorks", CompanyTier.MAJOR, CompanyType.STUDIO),
        923 to RecognisedCompany(923, "Legendary", CompanyTier.MAJOR, CompanyType.STUDIO),
        1632 to RecognisedCompany(1632, "Lionsgate", CompanyTier.MAJOR, CompanyType.STUDIO),
        3172 to RecognisedCompany(3172, "Blumhouse", CompanyTier.MAJOR, CompanyType.STUDIO),
        10146 to RecognisedCompany(10146, "Focus Features", CompanyTier.MAJOR, CompanyType.STUDIO),
        127929 to RecognisedCompany(127929, "Searchlight Pictures", CompanyTier.MAJOR, CompanyType.STUDIO),
        43 to RecognisedCompany(43, "National Geographic", CompanyTier.MAJOR, CompanyType.BOTH),
        250 to RecognisedCompany(250, "Univision", CompanyTier.MAJOR, CompanyType.NETWORK),
        
        // RECOGNISED STUDIOS
        14 to RecognisedCompany(14, "Miramax", CompanyTier.RECOGNISED, CompanyType.STUDIO),
        34 to RecognisedCompany(34, "Sony Pictures", CompanyTier.RECOGNISED, CompanyType.STUDIO)
    )

    private val networkIndex = mapOf(
        2 to RecognisedCompany(2, "ABC", CompanyTier.PREMIER, CompanyType.NETWORK),
        49 to RecognisedCompany(49, "HBO", CompanyTier.PREMIER, CompanyType.NETWORK),
        67 to RecognisedCompany(67, "Showtime", CompanyTier.PREMIER, CompanyType.NETWORK),
        88 to RecognisedCompany(88, "FX", CompanyTier.PREMIER, CompanyType.NETWORK),
        174 to RecognisedCompany(174, "AMC", CompanyTier.PREMIER, CompanyType.NETWORK),
        213 to RecognisedCompany(213, "Netflix", CompanyTier.PREMIER, CompanyType.BOTH, brandingName = "Netflix Original"),
        453 to RecognisedCompany(453, "Hulu", CompanyTier.PREMIER, CompanyType.BOTH),
        1024 to RecognisedCompany(1024, "Prime Video", CompanyTier.PREMIER, CompanyType.BOTH, brandingName = "Prime Exclusive"),
        2552 to RecognisedCompany(2552, "Apple TV", CompanyTier.PREMIER, CompanyType.BOTH, brandingName = "Apple TV+"),
        2739 to RecognisedCompany(2739, "Disney+", CompanyTier.PREMIER, CompanyType.BOTH)
    )

    fun getRecognisedCompany(id: Int): RecognisedCompany? = companyIndex[id]
    
    fun getRecognisedNetwork(id: Int): RecognisedCompany? = networkIndex[id]
}
