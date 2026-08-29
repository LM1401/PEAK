package com.example.peak.data.remote.recognition

import com.example.peak.data.remote.dto.TmdbCompany
import com.example.peak.domain.model.MediaType

/**
 * Deterministic scoring algorithm to select the most recognizable company.
 */
object ProductionCompanyRecognition {

    private const val LOGO_SCORE_BONUS = 50
    private const val ORDER_SCORE_PENALTY_STEP = 10
    private const val RECOGNISED_NETWORK_BOOST = 200
    private const val MIN_CONFIDENCE_THRESHOLD = 80 // Higher threshold to ensure quality

    /**
     * Examines all candidates and returns the name of the highest-scoring trustworthy company.
     */
    fun findBestCompany(
        productionCompanies: List<TmdbCompany>?,
        networks: List<TmdbCompany>?,
        mediaType: MediaType
    ): String {
        val scoredCandidates = when (mediaType) {
            MediaType.MOVIE -> {
                scoreCompanies(productionCompanies, isTvContext = false, isNetworkSource = false)
            }
            MediaType.TV -> {
                val networkScores = scoreCompanies(networks, isTvContext = true, isNetworkSource = true)
                val companyScores = scoreCompanies(productionCompanies, isTvContext = true, isNetworkSource = false)
                networkScores + companyScores
            }
        }

        return scoredCandidates
            .maxByOrNull { it.score }
            ?.takeIf { it.score >= MIN_CONFIDENCE_THRESHOLD }
            ?.name ?: ""
    }

    private data class ScoredCandidate(val name: String, val score: Int)

    private fun scoreCompanies(
        companies: List<TmdbCompany>?,
        isTvContext: Boolean,
        isNetworkSource: Boolean
    ): List<ScoredCandidate> {
        if (companies.isNullOrEmpty()) return emptyList()

        return companies.mapIndexed { index, company ->
            var score = 0
            val recognised = if (isNetworkSource) {
                ProductionCompanyIndex.getRecognisedNetwork(company.id)
            } else {
                ProductionCompanyIndex.getRecognisedCompany(company.id)
            }
            
            // 1. Base Score from Recognition Tier
            score += recognised?.tier?.baseScore ?: CompanyTier.UNKNOWN.baseScore

            // 2. TV Network Contextual Boost
            // Only recognized networks or streamers from the correct source get the boost in TV context.
            if (isTvContext && recognised != null && 
                (recognised.type == CompanyType.NETWORK || recognised.type == CompanyType.BOTH)) {
                score += RECOGNISED_NETWORK_BOOST
            }

            // 3. Logo Signal (Strictly supporting)
            // A logo adds confidence but cannot push an unknown company over the threshold alone.
            if (!company.logoPath.isNullOrBlank()) {
                score += LOGO_SCORE_BONUS
            }

            // 4. Order Penalty (Tie-breaker)
            score -= (index * ORDER_SCORE_PENALTY_STEP)

            ScoredCandidate(recognised?.name ?: company.name, score)
        }
    }
}
