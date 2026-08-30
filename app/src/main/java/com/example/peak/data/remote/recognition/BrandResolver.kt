package com.example.peak.data.remote.recognition

import com.example.peak.domain.model.BrandConfidence

object BrandResolver {

    data class ResolutionResult(
        val definition: BrandDefinition,
        val confidence: BrandConfidence
    )

    fun resolve(
        name: String? = null,
        id: Int? = null,
        type: BrandSignalType = BrandSignalType.COMPANY
    ): ResolutionResult? {
        // 1. Match by ID first (High Confidence)
        if (id != null) {
            BrandCatalog.brands.find { definition ->
                when (type) {
                    BrandSignalType.COMPANY -> definition.whitelistedCompanyIds.contains(id)
                    BrandSignalType.NETWORK -> definition.whitelistedNetworkIds.contains(id)
                    BrandSignalType.PROVIDER -> definition.whitelistedProviderIds.contains(id)
                }
            }?.let { 
                return ResolutionResult(it, BrandConfidence.HIGH) 
            }
        }

        // 2. Match by Name Pattern
        if (name == null) return null
        val normalized = normalize(name)
        if (normalized.isEmpty()) return null

        BrandCatalog.brands.find { definition ->
            definition.namePatterns.any { it.containsMatchIn(normalized) }
        }?.let {
            // Name patterns for Premier/Major brands are Medium-High confidence
            val confidence = if (it.tier == BrandTier.PREMIER || it.tier == BrandTier.MAJOR) {
                BrandConfidence.HIGH
            } else {
                BrandConfidence.MEDIUM
            }
            return ResolutionResult(it, confidence)
        }
        
        return null
    }

    private fun normalize(name: String): String {
        val noise = listOf(
            "pictures", "studios", "films", "entertainment", "productions",
            "group", "ltd", "inc", "corp", "corporation", "uk", "us"
        )
        
        var result = name.lowercase()
        
        // Remove corporate noise
        noise.forEach { word ->
            result = result.replace(Regex("\\b$word\\b"), "")
        }
        
        return result.replace(Regex("[^a-z0-9\\s]"), " ") // Remove punctuation
                     .replace(Regex("\\s+"), " ")         // Collapse spaces
                     .trim()
    }
}
