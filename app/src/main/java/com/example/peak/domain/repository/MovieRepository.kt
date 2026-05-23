package com.example.peak.domain.repository

import com.example.peak.domain.model.Movie

/**
 * Interface defining the operations for fetching movie data.
 * This lives in the domain layer and doesn't know about Retrofit.
 */
interface MovieRepository {
    suspend fun getTrendingMovies(): Result<List<Movie>>
    suspend fun getTrendingSeries(): Result<List<Movie>>
}
