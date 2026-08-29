package com.example.peak.data.remote.dto

import com.example.peak.data.remote.recognition.ProductionCompanyRecognition
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) representing the response from TMDB API.
 */
data class TmdbResponse(
    val results: List<TmdbMovie>
)

data class TmdbMovie(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    val overview: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    val runtime: Int?,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int>?,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int?,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int?,
    val genres: List<TmdbGenre>?,
    val credits: TmdbCredits?,
    @SerializedName("production_companies") val productionCompanies: List<TmdbCompany>?,
    val networks: List<TmdbCompany>?,
    @SerializedName("release_dates") val releaseDates: TmdbReleaseDates?,
    @SerializedName("content_ratings") val contentRatings: TmdbContentRatings?
)

data class TmdbGenre(
    val id: Int,
    val name: String
)

data class TmdbCompany(
    val id: Int,
    val name: String,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("origin_country") val originCountry: String?
)

data class TmdbCredits(
    val cast: List<TmdbCast>?,
    val crew: List<TmdbCrew>?
)

data class TmdbCast(
    val name: String
)

data class TmdbCrew(
    val name: String,
    val job: String
)

data class TmdbReleaseDates(
    val results: List<TmdbReleaseDateResult>?
)

data class TmdbReleaseDateResult(
    @SerializedName("iso_3166_1") val iso3166: String,
    @SerializedName("release_dates") val releaseDates: List<TmdbCertification>?
)

data class TmdbCertification(
    val certification: String
)

data class TmdbContentRatings(
    val results: List<TmdbContentRatingResult>?
)

data class TmdbContentRatingResult(
    @SerializedName("iso_3166_1") val iso3166: String,
    val rating: String
)

/**
 * Extension function to map Remote DTO to Domain Model.
 * This is used for summary data (trending lists).
 */
fun TmdbMovie.toMovie(mediaType: MediaType = MediaType.MOVIE): Movie {
    val displayName = title ?: name ?: "Unknown Title"
    val releaseYear = when (mediaType) {
        MediaType.MOVIE -> releaseDate?.take(4)
        MediaType.TV -> firstAirDate?.take(4)
    } ?: ""

    return Movie(
        movieId = id.toString(),
        mediaType = mediaType,
        name = displayName,
        imageUrl = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            ?: "https://via.placeholder.com/1280x720?text=$displayName",
        backdropUrl = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" }
            ?: "https://via.placeholder.com/1280x720?text=$displayName",
        description = overview ?: "Experience the latest trending story. Now streaming on PEAK.",
        rating = voteAverage?.let { String.format("%.1f", it) } ?: "",
        year = releaseYear,
        isEnriched = false
    )
}

/**
 * Maps full Detail DTO to enriched Domain Model.
 */
fun TmdbMovie.toEnrichedMovie(mediaType: MediaType): Movie {
    val summary = toMovie(mediaType)
    
    val genreString = genres?.joinToString(", ") { it.name } ?: ""
    val castString = credits?.cast?.take(3)?.joinToString(", ") { it.name } ?: ""
    
    val directorString = when (mediaType) {
        MediaType.MOVIE -> credits?.crew?.find { it.job == "Director" }?.name ?: ""
        MediaType.TV -> credits?.crew?.find { it.job == "Executive Producer" || it.job == "Producer" }?.name ?: ""
    }

    val durationString = when (mediaType) {
        MediaType.MOVIE -> {
            val r = runtime ?: 0
            if (r > 0) "${r / 60}h ${r % 60}m" else ""
        }
        MediaType.TV -> {
            val seasons = numberOfSeasons ?: 0
            val episodes = numberOfEpisodes ?: 0
            if (seasons > 0) "$seasons Seasons" else if (episodes > 0) "$episodes Episodes" else ""
        }
    }

    val certification = when (mediaType) {
        MediaType.MOVIE -> {
            releaseDates?.results?.find { it.iso3166 == "US" }?.releaseDates?.firstOrNull { it.certification.isNotBlank() }?.certification ?: ""
        }
        MediaType.TV -> {
            contentRatings?.results?.find { it.iso3166 == "US" }?.rating ?: ""
        }
    }

    val company = ProductionCompanyRecognition.findBestCompany(productionCompanies, networks, mediaType)

    return summary.copy(
        genres = genreString,
        cast = castString,
        director = directorString,
        duration = durationString,
        ageRating = certification,
        productionCompany = company,
        isEnriched = true
    )
}
