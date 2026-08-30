package com.example.peak.data.remote.recognition

import android.util.Log
import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.data.remote.dto.TmdbProvider
import com.example.peak.domain.model.MediaType

/**
 * Deterministic Branding Resolver.
 * Maps TMDB entities to PEAK-curated display brands.
 */
object ProductionCompanyRecognition {

    /**
     * Resolves metadata into a recognisable entertainment brand.
     *
     * Priority:
     * 1. Brand Tier (PREMIER > MAJOR > RECOGNISED)
     *   - Within Tier, context determines the winner:
     * 2. Source Priority (Provider > Network > Company > Note)
     * 3. Identity Priority (Explicit Brand Priority)
     * 4. Position: Original TMDB array order.
     */
    fun findBestCompany(
        productionCompanies: List<TmdbCompany>?,
        networks: List<TmdbCompany>?,
        providers: List<TmdbProvider>?,
        releaseNotes: List<String>?,
        mediaType: MediaType
    ): String {
        Log.e("PEAK_DIAGNOSTIC", "findBestCompany STAGE 2: companies=${productionCompanies?.size}, networks=${networks?.size}, providers=${providers?.size}, notes=${releaseNotes?.size}")
        val candidates = mutableListOf<BrandCandidate>()

        // 1. Providers (Streaming Originals / Strongest Signal)
        providers?.forEachIndexed { index, provider ->
            BrandResolver.resolve(provider.name, provider.id, BrandSignalType.PROVIDER)?.let { brand ->
                candidates.add(BrandCandidate(brand, index, isNetwork = true, sourcePriority = 10))
            }
        }

        // 2. Networks (TV Context)
        if (mediaType == MediaType.TV) {
            networks?.forEachIndexed { index, network ->
                BrandResolver.resolve(network.name, network.id, BrandSignalType.NETWORK)?.let { brand ->
                    candidates.add(BrandCandidate(brand, index, isNetwork = true, sourcePriority = 5))
                }
            }
        }

        // 3. Production Companies
        productionCompanies?.forEachIndexed { index, company ->
            BrandResolver.resolve(company.name, company.id, BrandSignalType.COMPANY)?.let { brand ->
                candidates.add(BrandCandidate(brand, index, isNetwork = false, sourcePriority = 0))
            }
        }

        // 4. Release Notes (Supplemental Evidence)
        releaseNotes?.forEachIndexed { index, note ->
            BrandResolver.resolve(name = note, type = BrandSignalType.COMPANY)?.let { brand ->
                // Release notes are used if no stronger signal is found
                candidates.add(BrandCandidate(brand, index, isNetwork = false, sourcePriority = -5))
            }
        }

        if (candidates.isEmpty()) return ""

        // Resolve Best Brand
        val winner = candidates.sortedWith(
            compareByDescending<BrandCandidate> { it.brand.tier.priority }
                .thenByDescending { it.sourcePriority }
                .thenByDescending { it.brand.priority }
                .thenBy { it.originalIndex }
        ).firstOrNull()

        return winner?.let { it.brand.getDisplayName(it.isNetwork) } ?: ""
    }

    private data class BrandCandidate(
        val brand: BrandDefinition,
        val originalIndex: Int,
        val isNetwork: Boolean,
        val sourcePriority: Int = 0
    )
}
