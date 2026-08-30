package com.example.peak.data.remote.api

import com.example.peak.data.remote.dto.TmdbMovie
import com.example.peak.data.remote.dto.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for TMDB API.
 */
interface TmdbApi {
    @GET("trending/movie/week")
    suspend fun getTrending(): TmdbResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTv(): TmdbResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: String,
        @Query("append_to_response") append: String = "credits,release_dates,watch/providers"
    ): TmdbMovie

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: String,
        @Query("append_to_response") append: String = "credits,content_ratings,watch/providers"
    ): TmdbMovie

    // MOVIE CATEGORIES
    @GET("movie/popular")
    suspend fun getPopularMovies(): TmdbResponse

    @GET("movie/top_rated")
    suspend fun getTopRatedMovies(): TmdbResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(): TmdbResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("with_genres") genreId: String
    ): TmdbResponse

    // TV CATEGORIES
    @GET("tv/popular")
    suspend fun getPopularSeries(): TmdbResponse

    @GET("tv/top_rated")
    suspend fun getTopRatedSeries(): TmdbResponse

    @GET("tv/on_the_air")
    suspend fun getOnTheAirSeries(): TmdbResponse

    @GET("discover/tv")
    suspend fun discoverSeries(
        @Query("with_genres") genreId: String
    ): TmdbResponse
}
