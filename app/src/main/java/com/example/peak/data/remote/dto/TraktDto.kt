package com.example.peak.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects (DTOs) for Trakt API integration.
 */
data class TraktSearchResponseDto(
    @SerializedName("type")
    val type: String? = null,

    @SerializedName("score")
    val score: Double? = null,

    @SerializedName("movie")
    val movie: TraktMediaItemDto? = null,

    @SerializedName("show")
    val show: TraktMediaItemDto? = null
)

data class TraktMediaItemDto(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("ids")
    val ids: TraktIdsDto? = null,

    @SerializedName("images")
    val images: TraktImagesDto? = null
)

data class TraktIdsDto(
    @SerializedName("trakt")
    val trakt: Int? = null,

    @SerializedName("tmdb")
    val tmdb: Int? = null
)

data class TraktImagesDto(
    @SerializedName("logo")
    val logo: List<String>? = null
)
