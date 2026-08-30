package com.example.peak.data.remote.recognition

object BrandResolver {
    fun resolve(
        name: String? = null,
        id: Int? = null,
        type: BrandSignalType = BrandSignalType.COMPANY
    ): BrandDefinition? {
        // 1. Match by ID first (High Confidence)
        if (id != null) {
            BrandCatalog.brands.find { definition ->
                when (type) {
                    BrandSignalType.COMPANY -> definition.whitelistedCompanyIds.contains(id)
                    BrandSignalType.NETWORK -> definition.whitelistedNetworkIds.contains(id)
                    BrandSignalType.PROVIDER -> definition.whitelistedProviderIds.contains(id)
                }
            }?.let { return it }
        }

        // 2. Match by Name Pattern
        if (name == null) return null
        val normalized = normalize(name)
        if (normalized.isEmpty()) return null

        return BrandCatalog.brands.find { definition ->
            definition.namePatterns.any { it.containsMatchIn(normalized) }
        }
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
