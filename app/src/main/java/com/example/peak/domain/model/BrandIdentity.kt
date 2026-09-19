package com.example.peak.domain.model

/**
 * Represents the brand identity for a movie or TV show.
 */
data class BrandIdentity(
    val key: String,
    val displayName: String,
    val tmdbCompanyId: Int? = null,
    val type: BrandIdentityType = BrandIdentityType.UNKNOWN,
    val confidence: BrandConfidence = BrandConfidence.NO_CONFIDENCE
) {
    /**
     * Whether this brand is considered suitable for primary display in the UI.
     */
    val isDisplayable: Boolean
        get() = confidence == BrandConfidence.HIGH || confidence == BrandConfidence.MEDIUM

    /**
     * Formats the brand identity into presentation text (e.g. "A Netflix Original", "A Blumhouse Production").
     */
    fun formatPresentationText(): String = formatBrandingText(displayName, type)
}

/**
 * Formats a brand or production company display name into presentation text.
 * e.g., "Netflix Original" -> "A Netflix Original"
 *       "Blumhouse"        -> "A Blumhouse Production"
 *       "Disney"           -> "A Disney Production"
 */
fun formatBrandingText(displayName: String, type: BrandIdentityType = BrandIdentityType.PRODUCTION_COMPANY): String {
    val cleanName = displayName.trim()
    if (cleanName.isEmpty()) return ""

    if (cleanName.startsWith("A ", ignoreCase = true) || cleanName.startsWith("An ", ignoreCase = true)) {
        return cleanName
    }

    val isOriginalOrExclusive = cleanName.contains("Original", ignoreCase = true) || 
                                cleanName.contains("Exclusive", ignoreCase = true)

    val baseText = when {
        isOriginalOrExclusive -> cleanName
        type == BrandIdentityType.STREAMER -> "$cleanName Original"
        type == BrandIdentityType.NETWORK -> "$cleanName Production"
        type == BrandIdentityType.STUDIO || type == BrandIdentityType.PRODUCTION_COMPANY -> {
            if (cleanName.endsWith("Production", ignoreCase = true) || 
                cleanName.endsWith("Productions", ignoreCase = true)
            ) {
                cleanName
            } else {
                "$cleanName Production"
            }
        }
        else -> {
            if (cleanName.endsWith("Production", ignoreCase = true) || 
                cleanName.endsWith("Productions", ignoreCase = true)
            ) {
                cleanName
            } else {
                "$cleanName Production"
            }
        }
    }

    val article = getIndefiniteArticle(baseText)
    return "$article $baseText"
}

private fun getIndefiniteArticle(text: String): String {
    val clean = text.trim()
    if (clean.isEmpty()) return "A"

    val upper = clean.uppercase()
    if (upper.startsWith("UNIVERSAL")) return "A"
    if (upper.startsWith("HBO") || upper.startsWith("AMC") || upper.startsWith("ITV") || 
        upper.startsWith("A24") || upper.startsWith("FX")
    ) {
        return "An"
    }

    val firstChar = clean.first().uppercaseChar()
    return if (firstChar in listOf('A', 'E', 'I', 'O')) "An" else "A"
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
