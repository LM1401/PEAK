package com.example.peak.data.remote.api

import com.example.peak.data.remote.dto.TraktSearchResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Headers

/**
 * Retrofit interface for Trakt API.
 */
interface TraktApi {

    @Headers(
        "trakt-api-version: 2",
        "Content-Type: application/json"
    )
    @GET("search/tmdb/{id}")
    suspend fun searchByTmdbId(
        @Path("id") tmdbId: String,
        @Query("type") type: String,
        @Query("extended") extended: String = "full,images"
    ): Response<List<TraktSearchResponseDto>>
}
