package com.example.peak.data.remote.recognition

import com.example.peak.domain.model.BrandIdentityType
import com.example.peak.domain.model.BrandPresentationStyle

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
    val priority: Int = 0,
    val presentationName: String? = null,
    val presentationStyle: BrandPresentationStyle = BrandPresentationStyle.PRODUCTION,
    val networkPresentationName: String? = null,
    val networkPresentationStyle: BrandPresentationStyle? = null
) {
    fun getDisplayName(isNetwork: Boolean): String {
        return if (isNetwork) (networkDisplayName ?: displayName) else displayName
    }

    fun getPresentationName(isNetwork: Boolean): String {
        return if (isNetwork) (networkPresentationName ?: presentationName ?: getDisplayName(true))
               else (presentationName ?: displayName)
    }

    fun getPresentationStyle(isNetwork: Boolean): BrandPresentationStyle {
        return if (isNetwork) (networkPresentationStyle ?: presentationStyle)
               else presentationStyle
    }
}
