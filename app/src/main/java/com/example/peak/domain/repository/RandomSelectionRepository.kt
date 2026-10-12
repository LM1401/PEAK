package com.example.peak.domain.repository

import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.RandomSelectionRequest

interface RandomSelectionRepository {
    suspend fun fetchRandomSelection(request: RandomSelectionRequest): Result<List<Movie>>
    
    /**
     * For individual replacement
     */
    suspend fun fetchSingleReplacement(
        request: RandomSelectionRequest,
        mediaType: com.example.peak.domain.model.MediaType,
        existingResults: List<Movie>
    ): Result<Movie>
}
