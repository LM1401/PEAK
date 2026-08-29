package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.domain.model.MediaType

/**
 * Deterministic recognition algorithm to select the most recognizable display brand.
 *
 * Logic:
 * 1. Filter: Only whitelisted entities are considered.
 * 2. Tier Priority: PREMIER > MAJOR > RECOGNISED.
 * 3. Source Priority: In TV context, Networks beat Production Companies.
 * 4. Internal Priority: Curated tie-breaker within Tier.
 * 5. Position Priority: First recognized entity in TMDB array wins.
 */
object ProductionCompanyRecognition {

    /**
     * Examines candidates and returns the curated branding name or company name.
     */
    fun findBestCompany(
        productionCompanies: List<TmdbCompany>?,
        networks: List<TmdbCompany>?,
        mediaType: MediaType
    ): String {
        val candidates = mutableListOf<RecognisedCandidate>()

        // 1. Collect Whitelisted Candidates (Maintaining Namespace Isolation)
        productionCompanies?.forEachIndexed { index, company ->
            ProductionCompanyIndex.getRecognisedCompany(company.id)?.let { recognised ->
                candidates.add(RecognisedCandidate(recognised, index, isNetworkSource = false))
            }
        }

        // Only consider networks in TV context to prevent namespace leaks into Movies
        if (mediaType == MediaType.TV) {
            networks?.forEachIndexed { index, company ->
                ProductionCompanyIndex.getRecognisedNetwork(company.id)?.let { recognised ->
                    candidates.add(RecognisedCandidate(recognised, index, isNetworkSource = true))
                }
            }
        }

        if (candidates.isEmpty()) return ""

        // 2. Deterministic Ranking
        return candidates.sortedWith(
            compareByDescending<RecognisedCandidate> { it.info.tier.priority }
                .thenByDescending { if (it.isNetworkSource) 1 else 0 }
                .thenByDescending { it.info.priority }
                .thenBy { it.originalIndex }
        ).firstOrNull()?.let {
            it.info.brandingName ?: it.info.name
        } ?: ""
    }

    private data class RecognisedCandidate(
        val info: RecognisedCompany,
        val originalIndex: Int,
        val isNetworkSource: Boolean
    )
}
