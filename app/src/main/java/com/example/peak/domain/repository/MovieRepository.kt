package com.example.peak.domain.repository

import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie

/**
 * Interface defining the operations for fetching movie and TV data.
 * This lives in the domain layer and doesn't know about Retrofit.
 */
interface MovieRepository {
    suspend fun getTrendingMovies(): Result<List<Movie>>
    suspend fun getTrendingSeries(): Result<List<Movie>>
    suspend fun getMediaById(id: String, type: MediaType): Result<Movie>
}
