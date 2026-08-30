package com.example.peak.data.remote.recognition

import android.util.Log
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
        Log.e("PEAK_DIAGNOSTIC", "findBestCompany: companies=${productionCompanies?.size}, networks=${networks?.size}, providers=${providers?.size}, notes=${releaseNotes?.size}")
        val candidates = mutableListOf<BrandCandidate>()

        // 1. Providers (Streaming Originals / Strongest Signal)
        providers?.forEachIndexed { index, provider ->
            BrandResolver.resolve(provider.name, provider.id, BrandSignalType.PROVIDER)?.let { result ->
                candidates.add(BrandCandidate(
                    identity = BrandIdentity(
                        key = result.definition.key,
                        displayName = result.definition.getDisplayName(isNetwork = true),
                        logoUrl = provider.logoPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                        tmdbCompanyId = provider.id,
                        type = result.definition.type,
                        confidence = result.confidence
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
                            logoUrl = network.logoPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                            tmdbCompanyId = network.id,
                            type = BrandIdentityType.NETWORK,
                            confidence = result.confidence
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
                        logoUrl = company.logoPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                        tmdbCompanyId = company.id,
                        type = result.definition.type,
                        confidence = result.confidence
                    ),
                    originalIndex = index,
                    sourcePriority = 0,
                    signalType = BrandSignalType.COMPANY
                ))
            } else if (!company.logoPath.isNullOrBlank()) {
                // Unknown company with a logo -> LOW confidence candidate
                candidates.add(BrandCandidate(
                    identity = BrandIdentity(
                        key = "unknown_${company.id}",
                        displayName = company.name,
                        logoUrl = "https://image.tmdb.org/t/p/w500${company.logoPath}",
                        tmdbCompanyId = company.id,
                        type = BrandIdentityType.PRODUCTION_COMPANY,
                        confidence = BrandConfidence.LOW
                    ),
                    originalIndex = index,
                    sourcePriority = -10, // Low priority for unknown companies
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
                        confidence = BrandConfidence.MEDIUM // Evidence from notes is medium confidence
                    ),
                    originalIndex = index,
                    sourcePriority = -5,
                    signalType = BrandSignalType.COMPANY
                ))
            }
        }

        if (candidates.isEmpty()) return null

        // Group candidates by brand key to separate Identity from Display Asset
        val mergedBrands = candidates.groupBy { it.identity.key }.map { (key, group) ->
            // 1. Determine the Best Metadata (highest source priority wins identity)
            val bestInfo = group.maxWith(
                compareBy<BrandCandidate> { it.identity.confidence.ordinal }
                    .thenByDescending { it.sourcePriority }
            )

            // 2. Determine the Best Visual Asset (Wordmark > Icon)
            // We prefer COMPANY/NETWORK logos over PROVIDER icons for Hero branding.
            val bestLogo = group.filter { !it.identity.logoUrl.isNullOrBlank() }
                .sortedWith(
                    compareByDescending<BrandCandidate> { it.signalType != BrandSignalType.PROVIDER }
                        .thenByDescending { it.sourcePriority }
                ).firstOrNull()

            // Fetch brand priority from catalog
            val brandPriority = BrandCatalog.brands.find { it.key == key }?.priority ?: 0

            MergedCandidate(
                identity = bestInfo.identity.copy(
                    logoUrl = bestLogo?.identity?.logoUrl ?: bestInfo.identity.logoUrl
                ),
                maxSourcePriority = bestInfo.sourcePriority,
                brandPriority = brandPriority,
                minOriginalIndex = group.minOf { it.originalIndex }
            )
        }

        // Resolve Best Brand among all identified brands
        // Filter for displayable brands (HIGH or MEDIUM confidence)
        val displayableMerged = mergedBrands.filter { it.identity.isDisplayable }

        if (displayableMerged.isEmpty()) {
            Log.d("PEAK_DIAGNOSTIC", "No displayable brand found for title.")
            return null
        }

        // Sort by confidence, then Brand Priority (specific beats generic), then Source Priority
        val winner = displayableMerged.sortedWith(
            compareBy<MergedCandidate> { it.identity.confidence.ordinal }
                .thenByDescending { it.brandPriority }
                .thenByDescending { it.maxSourcePriority }
                .thenBy { it.minOriginalIndex }
        ).firstOrNull()

        Log.d("PEAK_DIAGNOSTIC", "Resolved winner: ${winner?.identity?.displayName} with confidence ${winner?.identity?.confidence}")
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

