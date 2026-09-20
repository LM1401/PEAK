package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.data.remote.dto.TmdbProvider
import com.example.peak.domain.model.BrandConfidence
import com.example.peak.domain.model.BrandIdentity
import com.example.peak.domain.model.BrandIdentityType
import com.example.peak.domain.model.MediaType

/**
 * Deterministic Branding Resolver.
 * Maps TMDB entities to PEAK-curated display brands.
 */
object ProductionCompanyRecognition {

    /**
     * Resolves metadata into a recognisable entertainment brand identity.
     */
    fun findBestCompany(
        productionCompanies: List<TmdbCompany>?,
        networks: List<TmdbCompany>?,
        providers: List<TmdbProvider>?,
        releaseNotes: List<String>?,
        mediaType: MediaType
    ): BrandIdentity? {
        val candidates = mutableListOf<BrandCandidate>()

        // 1. Providers (Streaming Originals / Strongest Signal)
        providers?.forEachIndexed { index, provider ->
            BrandResolver.resolve(provider.name, provider.id, BrandSignalType.PROVIDER)?.let { result ->
                candidates.add(BrandCandidate(
                    identity = BrandIdentity(
                        key = result.definition.key,
                        displayName = result.definition.getDisplayName(isNetwork = true),
                        tmdbCompanyId = provider.id,
                        type = result.definition.type,
                        confidence = result.confidence,
                        presentationName = result.definition.getPresentationName(isNetwork = true),
                        presentationStyle = result.definition.getPresentationStyle(isNetwork = true)
                    ),
                    originalIndex = index,
                    sourcePriority = 10,
                    signalType = BrandSignalType.PROVIDER
                ))
            }
        }

        // 2. Networks (TV Context)
        if (mediaType == MediaType.TV) {
            networks?.forEachIndexed { index, network ->
                BrandResolver.resolve(network.name, network.id, BrandSignalType.NETWORK)?.let { result ->
                    candidates.add(BrandCandidate(
                        identity = BrandIdentity(
                            key = result.definition.key,
                            displayName = result.definition.getDisplayName(isNetwork = true),
                            tmdbCompanyId = network.id,
                            type = BrandIdentityType.NETWORK,
                            confidence = result.confidence,
                            presentationName = result.definition.getPresentationName(isNetwork = true),
                            presentationStyle = result.definition.getPresentationStyle(isNetwork = true)
                        ),
                        originalIndex = index,
                        sourcePriority = 5,
                        signalType = BrandSignalType.NETWORK
                    ))
                }
            }
        }

        // 3. Production Companies
        productionCompanies?.forEachIndexed { index, company ->
            val result = BrandResolver.resolve(company.name, company.id, BrandSignalType.COMPANY)
            if (result != null) {
                candidates.add(BrandCandidate(
                    identity = BrandIdentity(
                        key = result.definition.key,
                        displayName = result.definition.getDisplayName(isNetwork = false),
                        tmdbCompanyId = company.id,
                        type = result.definition.type,
                        confidence = result.confidence,
                        presentationName = result.definition.getPresentationName(isNetwork = false),
                        presentationStyle = result.definition.getPresentationStyle(isNetwork = false)
                    ),
                    originalIndex = index,
                    sourcePriority = 0,
                    signalType = BrandSignalType.COMPANY
                ))
            }
        }

        // 4. Release Notes (Distributor Evidence)
        releaseNotes?.forEachIndexed { index, note ->
            BrandResolver.resolve(name = note, type = BrandSignalType.COMPANY)?.let { result ->
                candidates.add(BrandCandidate(
                    identity = BrandIdentity(
                        key = result.definition.key,
                        displayName = result.definition.getDisplayName(isNetwork = false),
                        type = BrandIdentityType.DISTRIBUTOR,
                        confidence = BrandConfidence.MEDIUM, // Evidence from notes is medium confidence
                        presentationName = result.definition.getPresentationName(isNetwork = false),
                        presentationStyle = result.definition.getPresentationStyle(isNetwork = false)
                    ),
                    originalIndex = index,
                    sourcePriority = -5,
                    signalType = BrandSignalType.COMPANY
                ))
            }
        }

        if (candidates.isEmpty()) return null

        // Group candidates by brand key
        val mergedBrands = candidates.groupBy { it.identity.key }.map { (key, group) ->
            val bestInfo = group.minWith(
                compareBy<BrandCandidate> { it.identity.confidence.ordinal }
                    .thenByDescending { it.sourcePriority }
            )

            val definition = BrandCatalog.brands.find { it.key == key }
            val brandPriority = definition?.priority ?: 0

            MergedCandidate(
                identity = bestInfo.identity,
                maxSourcePriority = bestInfo.sourcePriority,
                brandPriority = brandPriority,
                minOriginalIndex = group.minOf { it.originalIndex }
            )
        }

        // Resolve Best Brand among all identified brands
        // Filter for displayable brands (HIGH or MEDIUM confidence)
        val displayableMerged = mergedBrands.filter { it.identity.isDisplayable }

        if (displayableMerged.isEmpty()) {
            return null
        }

        // Sort by confidence, then Brand Priority (specific beats generic), then Source Priority
        val winner = displayableMerged.sortedWith(
            compareBy<MergedCandidate> { it.identity.confidence.ordinal }
                .thenByDescending { it.brandPriority }
                .thenByDescending { it.maxSourcePriority }
                .thenBy { it.minOriginalIndex }
        ).firstOrNull()

        return winner?.identity
    }

    private data class BrandCandidate(
        val identity: BrandIdentity,
        val originalIndex: Int,
        val sourcePriority: Int = 0,
        val signalType: BrandSignalType
    )

    private data class MergedCandidate(
        val identity: BrandIdentity,
        val maxSourcePriority: Int,
        val brandPriority: Int,
        val minOriginalIndex: Int
    )
}
