package com.example.peak.data.repository

import com.example.peak.data.remote.api.TmdbApi
import com.example.peak.data.remote.dto.toMovie
import com.example.peak.domain.model.Movie
import com.example.peak.domain.repository.MovieRepository

/**
 * Implementation of [MovieRepository] that fetches data from TMDB API.
 * This is where we handle the conversion from API responses to domain models.
 */
class MovieRepositoryImpl(
    private val api: TmdbApi
) : MovieRepository {
    
    override suspend fun getTrendingMovies(): Result<List<Movie>> {
        return try {
            val response = api.getTrending()
            // Map the API results to our Domain models using our extension function
            Result.success(response.results.map { it.toMovie() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrendingSeries(): Result<List<Movie>> {
        return try {
            val response = api.getTrendingTv()
            Result.success(response.results.map { it.toMovie() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMovieById(movieId: String): Result<Movie> {
        return try {
            val response = api.getMovieDetails(movieId)
            Result.success(response.toMovie())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
