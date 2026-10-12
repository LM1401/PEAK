package com.example.peak.domain.model

enum class DiscoveryMode {
    COMPLETELY_RANDOM,
    REFINE_SELECTION
}

data class RandomSelectionRequest(
    val mode: DiscoveryMode = DiscoveryMode.COMPLETELY_RANDOM,
    val includeMovies: Boolean = true,
    val includeTvShows: Boolean = true,
    val moviesRequested: Int = 1,
    val tvRequested: Int = 0,
    val filters: Map<String, String> = emptyMap(),
    val sessionExclusions: Set<String> = emptySet()
) {
    val totalRequested: Int
        get() = moviesRequested + tvRequested
}
