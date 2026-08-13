package com.example.peak.domain.repository

import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie

enum class MovieListType {
    TRENDING,
    POPULAR,
    NOW_PLAYING,
    TOP_RATED,
    GENRE
}

enum class TvListType {
    TRENDING,
    POPULAR,
    TOP_RATED,
    ON_THE_AIR,
    GENRE
}

/**
 * Interface defining the operations for fetching movie and TV data.
 */
interface MovieRepository {
    suspend fun getTrendingMovies(): Result<List<Movie>>
    suspend fun getTrendingSeries(): Result<List<Movie>>
    
    suspend fun getMovies(type: MovieListType, genreId: Int? = null): Result<List<Movie>>
    suspend fun getSeries(type: TvListType, genreId: Int? = null): Result<List<Movie>>
    
    suspend fun getMediaById(id: String, type: MediaType): Result<Movie>
}
