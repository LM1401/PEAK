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
    val results: List<TmdbMovie>,
    @SerializedName("total_pages") val totalPages: Int? = null
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
    val images: TmdbImages? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("external_ids") val externalIds: TmdbExternalIds? = null,
    @SerializedName("imdb_id") val imdbId: String? = null
)

data class TmdbExternalIds(
    @SerializedName("imdb_id") val imdbId: String? = null
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
fun TmdbMovie.toMovie(defaultMediaType: MediaType = MediaType.MOVIE): Movie? {
    // If mediaType field is present (from search/multi), filter out anything not movie/tv
    val resolvedMediaType = when (this.mediaType) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        "person" -> return null // Explicitly ignore person results
        null -> defaultMediaType
        else -> return null
    }

    val displayName = title ?: name ?: "Unknown Title"
    val releaseYear = when (resolvedMediaType) {
        MediaType.MOVIE -> releaseDate?.take(4)
        MediaType.TV -> firstAirDate?.take(4)
    } ?: ""

    return Movie(
        movieId = id.toString(),
        mediaType = resolvedMediaType,
        name = displayName,
        imageUrl = posterPath?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w500$it" } ?: "",
        backdropUrl = backdropPath?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: "",
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
    val summary = toMovie(mediaType) ?: return Movie(movieId = id.toString(), mediaType = mediaType, name = "Unknown", imageUrl = "", backdropUrl = "", description = "", rating = "", year = "", videoUrl = "", isEnriched = false)
    
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
    val logoAspectRatio = images?.logos?.find { logo ->
        logo.filePath?.let { "https://image.tmdb.org/t/p/w500$it" } == titleLogoUrl
    }?.aspectRatio?.toFloat()
    val resolvedImdbId = externalIds?.imdbId?.takeIf { it.isNotBlank() }
        ?: imdbId?.takeIf { it.isNotBlank() }

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
        logoAspectRatio = logoAspectRatio,
        videoUrl = summary.videoUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        isEnriched = true,
        imdbId = resolvedImdbId
    )
}

/**
 * Deterministically selects the highest-quality, best-proportioned title logo URL from available TMDB logos.
 * Maximizes branded logo coverage across both movies and TV series without requiring text fallbacks.
 */
fun selectBestTitleLogo(logos: List<TmdbLogo>?): String? {
    if (logos.isNullOrEmpty()) return null

    data class ScoredLogo(val logo: TmdbLogo, val score: Int, val pixelArea: Long)

    val scoredLogos = logos.mapNotNull { logo ->
        val filePath = logo.filePath
        if (filePath.isNullOrBlank()) return@mapNotNull null

        // Accept .png, .jpg, .jpeg; exclude unsupported SVG vector assets until Coil SVG decoder is added
        val isRasterImage = filePath.endsWith(".png", ignoreCase = true) ||
                            filePath.endsWith(".jpg", ignoreCase = true) ||
                            filePath.endsWith(".jpeg", ignoreCase = true)
        if (!isRasterImage) return@mapNotNull null

        val lang = logo.iso6391
        val ar = logo.aspectRatio ?: 0.0
        val w = logo.width
        val h = logo.height

        val hasKnownWidth = w != null && w > 0
        val hasKnownHeight = h != null && h > 0

        // Aspect-ratio eligibility
        if (ar > 0.0 && (ar < 0.5 || ar > 8.0)) return@mapNotNull null

        // Disqualify genuinely tiny thumbnails or single-pixel artifacts
        if (hasKnownWidth && w in 1..199 && hasKnownHeight && h in 1..199) return@mapNotNull null
        if (hasKnownWidth && hasKnownHeight && minOf(w, h) in 1..29) return@mapNotNull null

        var score = 0

        // Language suitability
        when {
            lang.equals("en", ignoreCase = true) -> score += 100
            lang.isNullOrBlank() -> score += 70
            else -> score += 30 // Foreign native-language logos remain eligible
        }

        // Aspect-ratio scoring (Mutually exclusive conditions)
        when {
            ar >= 1.8 && ar < 4.5 -> score += 50 // Optimal horizontal
            (ar >= 1.4 && ar < 1.8) || (ar >= 4.5 && ar <= 7.0) -> score += 40 // Standard horizontal / banner
            (ar >= 0.7 && ar < 1.4) || (ar > 7.0 && ar <= 8.0) -> score += 30 // Square / stacked badge / extreme ribbon
            ar >= 0.5 && ar < 0.7 -> score += 20 // Tall portrait
            else -> score += 25 // Missing / unknown aspect ratio (ar <= 0.0)
        }

        // Resolution bonus (ONLY awarded when BOTH width and height are known and > 0)
        if (hasKnownWidth && hasKnownHeight) {
            val maxDim = maxOf(w, h)
            when {
                maxDim >= 1000 -> score += 30
                maxDim >= 500 -> score += 20
                maxDim >= 200 -> score += 10
            }
        }

        val pixelArea = if (hasKnownWidth && hasKnownHeight) w.toLong() * h.toLong() else 0L
        ScoredLogo(logo, score, pixelArea)
    }

    // Minimum suitability threshold
    val eligibleLogos = scoredLogos.filter { it.score >= 30 }
    if (eligibleLogos.isEmpty()) return null

    // Deterministic selection tie-breaker: Score -> Aspect Ratio -> Pixel Area -> File Path String
    val winner = eligibleLogos.maxWithOrNull(
        compareBy<ScoredLogo> { it.score }
            .thenBy { it.logo.aspectRatio ?: 0.0 }
            .thenBy { it.pixelArea }
            .thenBy { it.logo.filePath ?: "" }
    )

    return winner?.let { "https://image.tmdb.org/t/p/w500${it.logo.filePath}" }
}
