package com.example.peak.domain.model

/**
 * Domain representation of an installed add-on and Stremio v3/v4 manifest schemas.
 */
data class Addon(
    val id: String,
    val transportUrl: String,
    val manifestId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val version: String? = null,
    val installedAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true,
    val manifest: AddonManifest? = null
) {
    val displayName: String
        get() = name ?: manifest?.name ?: manifestId ?: transportUrl
}

/**
 * Parsed manifest metadata for a Stremio-compatible add-on.
 */
data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String? = null,
    val resources: List<AddonResource> = emptyList(),
    val types: List<String> = emptyList(),
    val catalogs: List<AddonCatalog> = emptyList(),
    val idPrefixes: List<String> = emptyList(),
    val behaviorHints: AddonBehaviorHints = AddonBehaviorHints()
)

/**
 * Resource declaration inside an add-on manifest (e.g., catalog, meta, stream, subtitles).
 */
data class AddonResource(
    val name: String,
    val types: List<String> = emptyList(),
    val idPrefixes: List<String> = emptyList()
)

/**
 * Catalogue definition inside an add-on manifest.
 */
data class AddonCatalog(
    val type: String,
    val id: String,
    val name: String? = null,
    val extra: List<AddonExtra> = emptyList()
)

/**
 * Optional or required filter parameter for an add-on catalogue.
 */
data class AddonExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList()
)

/**
 * Flags declaring specialized add-on capabilities or restrictions.
 */
data class AddonBehaviorHints(
    val configurable: Boolean = false,
    val configurationRequired: Boolean = false,
    val adult: Boolean = false,
    val p2p: Boolean = false
)
