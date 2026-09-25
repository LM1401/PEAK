package com.example.peak.data.remote.dto

import android.util.Log
import com.example.peak.data.remote.recognition.ProductionCompanyRecognition
import com.example.peak.domain.model.CastMember
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
    @SerializedName("content_ratings") val contentRatings: TmdbContentRatings?,
    @SerializedName("watch/providers") val watchProviders: TmdbWatchProviders?,
    val images: TmdbImages? = null
)

data class TmdbImages(
    val logos: List<TmdbLogo>?
)

data class TmdbLogo(
    @SerializedName("file_path") val filePath: String?,
    @SerializedName("iso_639_1") val iso6391: String?,
    @SerializedName("aspect_ratio") val aspectRatio: Double?,
    val width: Int? = null,
    val height: Int? = null
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
    val name: String,
    @SerializedName("profile_path") val profilePath: String? = null
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
    val certification: String,
    val note: String? = null
)

data class TmdbContentRatings(
    val results: List<TmdbContentRatingResult>?
)

data class TmdbContentRatingResult(
    @SerializedName("iso_3166_1") val iso3166: String,
    val rating: String
)

data class TmdbWatchProviders(
    val results: Map<String, TmdbCountryProviders>?
)

data class TmdbCountryProviders(
    val flatrate: List<TmdbProvider>?,
    val rent: List<TmdbProvider>?,
    val buy: List<TmdbProvider>?
)

data class TmdbProvider(
    @SerializedName("provider_id") val id: Int,
    @SerializedName("provider_name") val name: String,
    @SerializedName("logo_path") val logoPath: String?
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
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        isEnriched = false
    )
}

/**
 * Maps full Detail DTO to enriched Domain Model.
 */
fun TmdbMovie.toEnrichedMovie(mediaType: MediaType): Movie {
    Log.e("PEAK_DIAGNOSTIC", "Enriching movie ID: $id ($title/$name)")
    Log.e("PEAK_DIAGNOSTIC", "Raw Production Companies: ${productionCompanies?.joinToString { "${it.name}(${it.id})" }}")
    val summary = toMovie(mediaType)
    
    val genreString = genres?.joinToString(", ") { it.name } ?: ""
    val castString = credits?.cast?.take(3)?.joinToString(", ") { it.name } ?: ""
    val castList = credits?.cast?.take(6)?.map { castItem ->
        CastMember(
            name = castItem.name,
            profileUrl = castItem.profilePath?.let { "https://image.tmdb.org/t/p/w185$it" }
        )
    } ?: emptyList()
    
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

    val providers = watchProviders?.results?.get("US")?.flatrate
    val releaseNotes = releaseDates?.results?.find { it.iso3166 == "US" }?.releaseDates?.mapNotNull { it.note }

    val brandIdentity = ProductionCompanyRecognition.findBestCompany(
        productionCompanies = productionCompanies,
        networks = networks,
        providers = providers,
        releaseNotes = releaseNotes,
        mediaType = mediaType
    )
    Log.e("PEAK_DIAGNOSTIC", "Resolved Brand for $id: '${brandIdentity?.displayName ?: "None"}'")

    val titleLogoUrl = selectBestTitleLogo(images?.logos)

    return summary.copy(
        genres = genreString,
        cast = castString,
        castMembers = castList,
        director = directorString,
        duration = durationString,
        ageRating = certification,
        productionCompany = brandIdentity?.displayName ?: "",
        brand = brandIdentity,
        titleLogoUrl = titleLogoUrl,
        videoUrl = summary.videoUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        isEnriched = true
    )
}

/**
 * Deterministically selects the highest-quality, best-proportioned title logo URL from available TMDB logos.
 * Returns null if no available logo meets the suitability criteria, triggering the PEAK Cinematic Text Fallback.
 */
fun selectBestTitleLogo(logos: List<TmdbLogo>?): String? {
    if (logos.isNullOrEmpty()) return null

    data class ScoredLogo(val logo: TmdbLogo, val score: Int)

    val scoredLogos = logos.mapNotNull { logo ->
        val filePath = logo.filePath
        if (filePath.isNullOrBlank()) return@mapNotNull null

        val lang = logo.iso6391
        val ar = logo.aspectRatio ?: 0.0
        val width = logo.width ?: 0
        val height = logo.height ?: 0

        // Disqualify extreme aspect ratios (square/tall < 1.4 or ultra-wide banner > 5.5)
        if (ar > 0.0 && (ar < 1.4 || ar > 5.5)) return@mapNotNull null

        // Disqualify tiny logos if resolution data is present
        if (width in 1..199 || height in 1..29) return@mapNotNull null

        var score = 0

        // 1. Language suitability
        when {
            lang.equals("en", ignoreCase = true) -> score += 100
            lang.isNullOrBlank() -> score += 50
            else -> score -= 150 // Heavily penalize non-English logos
        }

        // 2. Aspect ratio suitability (Horizontal cinematic title logos)
        when {
            ar in 2.2..4.2 -> score += 80
            ar in 1.8..2.2 -> score += 50
            ar in 4.2..5.0 -> score += 40
            ar > 0.0 -> score += 10
            else -> score += 20 // Unknown aspect ratio
        }

        // 3. Resolution bonus
        when {
            width >= 500 -> score += 30
            width >= 300 -> score += 15
        }

        ScoredLogo(logo, score)
    }

    val winner = scoredLogos.maxByOrNull { it.score }
    // Minimum score threshold for a suitable logo is 80 (e.g. English logo with acceptable aspect ratio)
    return if (winner != null && winner.score >= 80) {
        "https://image.tmdb.org/t/p/w500${winner.logo.filePath}"
    } else {
        null
    }
}
