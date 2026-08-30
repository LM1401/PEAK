package com.example.peak.domain.model

/**
 * Represents the brand identity for a movie or TV show.
 */
data class BrandIdentity(
    val key: String,
    val displayName: String,
    val logoUrl: String? = null,
    val tmdbCompanyId: Int? = null,
    val type: BrandIdentityType = BrandIdentityType.UNKNOWN,
    val confidence: BrandConfidence = BrandConfidence.NO_CONFIDENCE
) {
    /**
     * Whether this brand is considered suitable for primary display in the UI.
     */
    val isDisplayable: Boolean
        get() = confidence == BrandConfidence.HIGH || confidence == BrandConfidence.MEDIUM
}

enum class BrandIdentityType {
    PRODUCTION_COMPANY,
    STUDIO,
    DISTRIBUTOR,
    NETWORK,
    STREAMER,
    UNKNOWN
}

enum class BrandConfidence {
    /**
     * Exact recognized TMDB ID or very strong recognized brand match.
     */
    HIGH,
    
    /**
     * Recognized name, recognized network, or strong distributor evidence.
     */
    MEDIUM,
    
    /**
     * Candidate only (e.g., unknown company with a logo). Not automatically displayable.
     */
    LOW,
    
    /**
     * Insufficient evidence.
     */
    NO_CONFIDENCE
}
