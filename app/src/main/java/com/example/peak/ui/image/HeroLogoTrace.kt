package com.example.peak.ui.image

import android.os.SystemClock
import android.util.Log
import com.example.peak.domain.model.MediaType

/**
 * Temporary, privacy-safe diagnostics for the home hero title-logo pipeline.
 *
 * Events include only stable media identity and fixed diagnostic values. Never pass URLs,
 * titles, exception messages, request objects, or headers to this logger.
 */
internal object HeroLogoTrace {
    private const val TAG = "PEAK_HERO_LOGO"
    private val safeMediaId = Regex("^[0-9]{1,12}$")
    private val safeCategory = setOf(
        "trending",
        "popular_movies",
        "popular_series",
        "top_rated_movies"
    )

    internal enum class Event(val wireName: String) {
        CATEGORY_RESULT_RECEIVED("category_result_received"),
        CATEGORY_REQUEST_FAILED("category_request_failed"),
        CATEGORY_REQUEST_CANCELLED("category_request_cancelled"),
        INITIAL_HERO_DETAIL_STARTED("initial_hero_detail_started"),
        INITIAL_HERO_DETAIL_SUCCEEDED("initial_hero_detail_succeeded"),
        INITIAL_HERO_DETAIL_FAILED("initial_hero_detail_failed"),
        INITIAL_HERO_DETAIL_CANCELLED("initial_hero_detail_cancelled"),
        DETAIL_CACHE_HIT("detail_cache_hit"),
        DETAIL_NETWORK_STARTED("detail_network_started"),
        DETAIL_RESPONSE_RECEIVED("detail_response_received"),
        DETAIL_REQUEST_FAILED("detail_request_failed"),
        DETAIL_REQUEST_CANCELLED("detail_request_cancelled"),
        LOGO_URL_AVAILABLE("logo_url_available"),
        LOGO_WARM_MEMORY_CACHE_HIT("logo_warm_memory_cache_hit"),
        LOGO_WARM_STARTED("logo_warm_started"),
        LOGO_WARM_SUCCEEDED("logo_warm_succeeded"),
        LOGO_WARM_FAILED("logo_warm_failed"),
        LOGO_WARM_CANCELLED("logo_warm_cancelled"),
        HERO_IMAGE_STARTED("hero_image_started"),
        HERO_IMAGE_SUCCEEDED("hero_image_succeeded"),
        HERO_IMAGE_FAILED("hero_image_failed"),
        HERO_IMAGE_CANCELLED("hero_image_cancelled"),
        LOGO_FIRST_DRAWN("logo_first_drawn")
    }

    internal enum class Outcome(val wireName: String) {
        CONTENT("content"),
        EMPTY("empty"),
        FAILED("failed"),
        CANCELLED("cancelled"),
        AVAILABLE("available"),
        MISSING("missing")
    }

    internal enum class ImageSource(val wireName: String) {
        MEMORY_CACHE("MEMORY_CACHE"),
        DISK("DISK"),
        NETWORK("NETWORK"),
        OTHER("OTHER");

        companion object {
            fun fromCoilName(name: String): ImageSource =
                entries.firstOrNull { it.wireName == name } ?: OTHER
        }
    }

    fun record(
        event: Event,
        mediaType: MediaType? = null,
        mediaId: String? = null,
        category: String? = null,
        outcome: Outcome? = null,
        logoAvailable: Boolean? = null,
        imageSource: ImageSource? = null
    ) {
        Log.d(
            TAG,
            format(
                event = event,
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(),
                mediaType = mediaType,
                mediaId = mediaId,
                category = category,
                outcome = outcome,
                logoAvailable = logoAvailable,
                imageSource = imageSource
            )
        )
    }

    internal fun format(
        event: Event,
        elapsedRealtimeNanos: Long,
        mediaType: MediaType? = null,
        mediaId: String? = null,
        category: String? = null,
        outcome: Outcome? = null,
        logoAvailable: Boolean? = null,
        imageSource: ImageSource? = null
    ): String = buildString {
        append("event=").append(event.wireName)
        append(" elapsedRealtimeNanos=").append(elapsedRealtimeNanos)
        mediaType?.let { append(" mediaType=").append(it.name) }
        mediaId?.let {
            append(" mediaId=")
            append(if (safeMediaId.matches(it)) it else "redacted")
        }
        category?.let {
            append(" category=")
            append(if (it in safeCategory) it else "other")
        }
        outcome?.let { append(" outcome=").append(it.wireName) }
        logoAvailable?.let { append(" logoAvailable=").append(it) }
        imageSource?.let { append(" dataSource=").append(it.wireName) }
    }
}
