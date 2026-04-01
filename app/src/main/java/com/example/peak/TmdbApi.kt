package com.example.peak

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

data class TmdbResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?
)

interface TmdbApi {
    @GET("trending/movie/week")
    suspend fun getTrending(): TmdbResponse
}
