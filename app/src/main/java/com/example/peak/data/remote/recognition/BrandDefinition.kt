package com.example.peak.data.remote.recognition

data class BrandDefinition(
    val key: String,
    val displayName: String,
    val tier: BrandTier,
    val whitelistedCompanyIds: Set<Int> = emptySet(),
    val whitelistedNetworkIds: Set<Int> = emptySet(),
    val whitelistedProviderIds: Set<Int> = emptySet(),
    val namePatterns: List<Regex> = emptyList(),
    val networkDisplayName: String? = null,
    val priority: Int = 0
) {
    fun getDisplayName(isNetwork: Boolean): String {
        return if (isNetwork) (networkDisplayName ?: displayName) else displayName
    }
}
