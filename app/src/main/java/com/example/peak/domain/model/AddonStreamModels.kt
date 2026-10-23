package com.example.peak.domain.model

/**
 * Represents the root JSON response from a Stremio-compatible add-on stream resource request.
 * Specification: https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/api/responses/stream.md
 */
data class AddonStreamResponse(
    val streams: List<AddonStream> = emptyList()
)

/**
 * Individual stream result entry returned by an add-on.
 * May contain direct video URLs, torrent hashes, or external player links.
 */
data class AddonStream(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val externalUrl: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val quality: String? = null,
    val behaviorHints: AddonStreamBehaviorHints? = null
)

/**
 * Optional flags and HTTP request/response hint headers provided by the add-on.
 */
data class AddonStreamBehaviorHints(
    val notVideo: Boolean = false,
    val proxyHeaders: AddonStreamProxyHeaders? = null,
    val bingeGroup: String? = null,
    val filename: String? = null
)

/**
 * Custom request/response headers required for streaming or proxying.
 */
data class AddonStreamProxyHeaders(
    val request: Map<String, String> = emptyMap(),
    val response: Map<String, String> = emptyMap()
)
