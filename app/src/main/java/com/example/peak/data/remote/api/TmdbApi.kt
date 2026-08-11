package com.example.peak.data.remote.api

import com.example.peak.data.remote.dto.TmdbMovie
import com.example.peak.data.remote.dto.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for TMDB API.
 * Notice it uses the DTO [TmdbResponse] for the network response.
 */
interface TmdbApi {
    @GET("trending/movie/week")
    suspend fun getTrending(): TmdbResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTv(): TmdbResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(@Path("movie_id") movieId: String): TmdbMovie

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(@Path("tv_id") tvId: String): TmdbMovie
}
