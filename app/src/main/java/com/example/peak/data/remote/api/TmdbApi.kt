package com.example.peak.data.remote.api

import com.example.peak.data.remote.dto.TmdbResponse
import retrofit2.http.GET

/**
 * Retrofit interface for TMDB API.
 * Notice it uses the DTO [TmdbResponse] for the network response.
 */
interface TmdbApi {
    @GET("trending/movie/week")
    suspend fun getTrending(): TmdbResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTv(): TmdbResponse
}
