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
    RECOGNISED(120),
    UNKNOWN(0)
}

data class RecognisedCompany(
    val id: Int,
    val name: String,
    val tier: CompanyTier,
    val type: CompanyType,
    val weight: Int = 0,
    val aliases: List<String> = emptyList()
)

/**
 * Reusable knowledge about recognised production companies and networks.
 * IDs are verified TMDB internal identifiers.
 */
object ProductionCompanyIndex {
    private val companyIndex = mapOf(
        // PREMIER STUDIOS
        1 to RecognisedCompany(1, "Lucasfilm", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        2 to RecognisedCompany(2, "Disney", CompanyTier.PREMIER, CompanyType.BOTH, weight = 100),
        3 to RecognisedCompany(3, "Pixar", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        4 to RecognisedCompany(4, "Paramount", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        5 to RecognisedCompany(5, "Columbia Pictures", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        21 to RecognisedCompany(21, "MGM", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 90),
        25 to RecognisedCompany(25, "20th Century Fox", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 90),
        33 to RecognisedCompany(33, "Universal", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        174 to RecognisedCompany(174, "Warner Bros.", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100, aliases = listOf("Warner Bros. Pictures")),
        213 to RecognisedCompany(213, "Netflix", CompanyTier.PREMIER, CompanyType.BOTH, weight = 100), // Legacy/Network ID usage
        420 to RecognisedCompany(420, "Marvel Studios", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        429 to RecognisedCompany(429, "DC", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        2552 to RecognisedCompany(2552, "Apple TV+", CompanyTier.PREMIER, CompanyType.BOTH, weight = 100), // Legacy/Network ID usage
        20580 to RecognisedCompany(20580, "Amazon Studios", CompanyTier.PREMIER, CompanyType.BOTH, weight = 90),
        41077 to RecognisedCompany(41077, "A24", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        9993 to RecognisedCompany(9993, "DC Films", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 90, aliases = listOf("DC Entertainment")),
        90733 to RecognisedCompany(90733, "NEON", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        127928 to RecognisedCompany(127928, "20th Century Studios", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        178464 to RecognisedCompany(178464, "Netflix", CompanyTier.PREMIER, CompanyType.BOTH, weight = 100),
        184898 to RecognisedCompany(184898, "DC Studios", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        194232 to RecognisedCompany(194232, "Apple Studios", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        210099 to RecognisedCompany(210099, "Amazon MGM Studios", CompanyTier.PREMIER, CompanyType.STUDIO, weight = 100),
        
        // MAJOR PRODUCTION COMPANIES
        12 to RecognisedCompany(12, "New Line Cinema", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50),
        521 to RecognisedCompany(521, "DreamWorks", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50, aliases = listOf("DreamWorks Animation")),
        923 to RecognisedCompany(923, "Legendary", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50, aliases = listOf("Legendary Pictures")),
        1632 to RecognisedCompany(1632, "Lionsgate", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50),
        3172 to RecognisedCompany(3172, "Blumhouse", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50, aliases = listOf("Blumhouse Productions")),
        10146 to RecognisedCompany(10146, "Focus Features", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50),
        127929 to RecognisedCompany(127929, "Searchlight Pictures", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 50),
        13240 to RecognisedCompany(13240, "Bron Studios", CompanyTier.MAJOR, CompanyType.STUDIO, weight = 40),
        
        // RECOGNISED STUDIOS
        7 to RecognisedCompany(7, "DreamWorks Pictures", CompanyTier.RECOGNISED, CompanyType.STUDIO),
        14 to RecognisedCompany(14, "Miramax", CompanyTier.RECOGNISED, CompanyType.STUDIO),
        34 to RecognisedCompany(34, "Sony Pictures", CompanyTier.RECOGNISED, CompanyType.STUDIO)
    )

    private val networkIndex = mapOf(
        // PREMIER NETWORKS
        2 to RecognisedCompany(2, "ABC", CompanyTier.PREMIER, CompanyType.NETWORK),
        49 to RecognisedCompany(49, "HBO", CompanyTier.PREMIER, CompanyType.NETWORK),
        67 to RecognisedCompany(67, "Showtime", CompanyTier.PREMIER, CompanyType.NETWORK),
        88 to RecognisedCompany(88, "FX", CompanyTier.PREMIER, CompanyType.NETWORK),
        174 to RecognisedCompany(174, "AMC", CompanyTier.PREMIER, CompanyType.NETWORK),
        213 to RecognisedCompany(213, "Netflix", CompanyTier.PREMIER, CompanyType.BOTH),
        453 to RecognisedCompany(453, "Hulu", CompanyTier.PREMIER, CompanyType.BOTH),
        1024 to RecognisedCompany(1024, "Prime Video", CompanyTier.PREMIER, CompanyType.BOTH, aliases = listOf("Amazon")),
        2552 to RecognisedCompany(2552, "Apple TV", CompanyTier.PREMIER, CompanyType.BOTH),
        2739 to RecognisedCompany(2739, "Disney+", CompanyTier.PREMIER, CompanyType.BOTH)
    )

    fun getRecognisedCompany(id: Int): RecognisedCompany? = companyIndex[id]
    
    fun getRecognisedNetwork(id: Int): RecognisedCompany? = networkIndex[id]
}
