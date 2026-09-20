package com.example.peak.domain.model

/**
 * Represents the brand identity for a movie or TV show.
 */
data class BrandIdentity(
    val key: String,
    val displayName: String,
    val tmdbCompanyId: Int? = null,
    val type: BrandIdentityType = BrandIdentityType.UNKNOWN,
    val confidence: BrandConfidence = BrandConfidence.NO_CONFIDENCE,
    val presentationName: String? = null,
    val presentationStyle: BrandPresentationStyle = BrandPresentationStyle.PRODUCTION
) {
    /**
     * Whether this brand is considered suitable for primary display in the UI.
     */
    val isDisplayable: Boolean
        get() = confidence == BrandConfidence.HIGH || confidence == BrandConfidence.MEDIUM

    /**
     * Formats the brand identity into presentation text (e.g. "A Netflix Original", "Warner Bros. Pictures Presents").
     */
    fun formatPresentationText(): String = formatBrandingText(
        displayName = presentationName ?: displayName,
        style = presentationStyle
    )
}

/**
 * Formats a brand or production company display name into presentation text based on presentation style.
 * e.g., "Netflix", ORIGINAL -> "A Netflix Original"
 *       "Blumhouse", PRODUCTION -> "A Blumhouse Production"
 *       "Warner Bros. Pictures", PRESENTS -> "Warner Bros. Pictures Presents"
 */
fun formatBrandingText(
    displayName: String,
    style: BrandPresentationStyle = BrandPresentationStyle.PRODUCTION
): String {
    val cleanName = displayName.trim()
    if (cleanName.isEmpty()) return ""

    // If cleanName is already full presentation text starting with "A " or "An "
    if (cleanName.startsWith("A ", ignoreCase = true) || cleanName.startsWith("An ", ignoreCase = true)) {
        return cleanName
    }

    // Defensive check: if cleanName already ends with " Presents", return as-is
    if (cleanName.endsWith(" Presents", ignoreCase = true)) {
        return cleanName
    }

    val hasOriginal = cleanName.endsWith(" Original", ignoreCase = true) || cleanName.contains(" Original", ignoreCase = true)
    val hasExclusive = cleanName.endsWith(" Exclusive", ignoreCase = true) || cleanName.contains(" Exclusive", ignoreCase = true)

    return when {
        hasOriginal -> {
            val article = getIndefiniteArticle(cleanName)
            "$article $cleanName"
        }
        hasExclusive -> {
            val article = getIndefiniteArticle(cleanName)
            "$article $cleanName"
        }
        style == BrandPresentationStyle.PRESENTS -> {
            "$cleanName Presents"
        }
        style == BrandPresentationStyle.ORIGINAL -> {
            val base = if (cleanName.endsWith("Original", ignoreCase = true)) cleanName else "$cleanName Original"
            val article = getIndefiniteArticle(base)
            "$article $base"
        }
        style == BrandPresentationStyle.EXCLUSIVE -> {
            val base = if (cleanName.endsWith("Exclusive", ignoreCase = true)) cleanName else "$cleanName Exclusive"
            val article = getIndefiniteArticle(base)
            "$article $base"
        }
        else -> { // BrandPresentationStyle.PRODUCTION
            val base = if (cleanName.endsWith("Production", ignoreCase = true) || 
                           cleanName.endsWith("Productions", ignoreCase = true)
            ) {
                cleanName
            } else {
                "$cleanName Production"
            }
            val article = getIndefiniteArticle(base)
            "$article $base"
        }
    }
}

/**
 * Legacy compatibility overload mapping BrandIdentityType to default presentation style.
 */
fun formatBrandingText(displayName: String, type: BrandIdentityType): String {
    val style = when (type) {
        BrandIdentityType.STREAMER -> BrandPresentationStyle.ORIGINAL
        else -> BrandPresentationStyle.PRODUCTION
    }
    return formatBrandingText(displayName, style)
}

private fun getIndefiniteArticle(text: String): String {
    val clean = text.trim()
    if (clean.isEmpty()) return "A"

    val upper = clean.uppercase()
    if (upper.startsWith("UNIVERSAL")) return "A"
    if (upper.startsWith("HBO") || upper.startsWith("AMC") || upper.startsWith("ITV") || 
        upper.startsWith("A24") || upper.startsWith("FX") || upper.startsWith("MGM")
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
