package com.example.peak.data.remote.recognition

import com.example.peak.domain.model.BrandIdentityType

data class BrandDefinition(
    val key: String,
    val displayName: String,
    val tier: BrandTier,
    val type: BrandIdentityType = BrandIdentityType.PRODUCTION_COMPANY,
    val whitelistedCompanyIds: Set<Int> = emptySet(),
    val whitelistedNetworkIds: Set<Int> = emptySet(),
    val whitelistedProviderIds: Set<Int> = emptySet(),
    val namePatterns: List<Regex> = emptyList(),
    val networkDisplayName: String? = null,
    val localResource: Int? = null,
    val priority: Int = 0
) {
    fun getDisplayName(isNetwork: Boolean): String {
        return if (isNetwork) (networkDisplayName ?: displayName) else displayName
    }
}
