package com.example.peak.ui.screens.random

import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.RandomSelectionRequest

enum class RandomStatus {
    IDLE,
    LOADING,
    REVEALING,
    RESULTS,
    ERROR
}

data class RandomUiState(
    val status: RandomStatus = RandomStatus.IDLE,
    val request: RandomSelectionRequest = RandomSelectionRequest(),
    val results: List<Movie> = emptyList(),
    val partialResultWarning: String? = null,
    val errorMessage: String? = null
)
