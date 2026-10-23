package com.example.peak.ui.screens.stream_selection

import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.AddonStreamProviderResult
import com.example.peak.domain.model.Movie

/**
 * State model representing the Stream Selection UI screen.
 */
data class StreamSelectionUiState(
    val movie: Movie? = null,
    val imdbId: String? = null,
    val providerResults: List<AddonStreamProviderResult> = emptyList(),
    val selectedStream: AddonStream? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val noAddonsConfigured: Boolean = false,
    val isTvWithoutEpisode: Boolean = false
) {
    /**
     * True if at least one stream was found across all providers.
     */
    val hasStreams: Boolean
        get() = providerResults.any { it.streams.isNotEmpty() }

    /**
     * Total number of streams across all providers.
     */
    val totalStreamCount: Int
        get() = providerResults.sumOf { it.streams.size }

    /**
     * True if all providers returned errors and no streams were found.
     */
    val isAllProvidersFailed: Boolean
        get() = providerResults.isNotEmpty() && providerResults.all { it.error != null && it.streams.isEmpty() }
}
