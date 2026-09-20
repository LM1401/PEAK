package com.example.peak.domain.model

/**
 * Defines the presentation style for a recognized entertainment brand.
 */
enum class BrandPresentationStyle {
    /**
     * Presentation format: "{Name} Presents" (e.g., "Warner Bros. Pictures Presents", "A24 Presents")
     */
    PRESENTS,

    /**
     * Presentation format: "{Article} {Name} Original" (e.g., "A Netflix Original", "An Apple Original")
     */
    ORIGINAL,

    /**
     * Presentation format: "{Article} {Name} Production" (e.g., "A Blumhouse Production", "A Disney Production")
     */
    PRODUCTION,

    /**
     * Presentation format: "{Article} {Name} Exclusive" (e.g., "A Prime Exclusive")
     */
    EXCLUSIVE
}
