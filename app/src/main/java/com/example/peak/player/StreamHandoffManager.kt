package com.example.peak.player

import com.example.peak.domain.model.AddonStream
import com.example.peak.domain.model.MediaType

/**
 * Thread-safe application-scoped singleton for transferring the selected AddonStream
 * from StreamSelectionScreen to PlayerScreen across navigation transitions.
 * Binds handoff state explicitly to target media identity (mediaId + mediaType).
 */
object StreamHandoffManager {

    data class HandoffEntry(
        val mediaId: String,
        val mediaType: MediaType,
        val stream: AddonStream
    )

    @Volatile
    private var activeEntry: HandoffEntry? = null

    @Synchronized
    fun setStream(mediaId: String, mediaType: MediaType, stream: AddonStream) {
        val cleanMediaId = mediaId.trim()
        if (cleanMediaId.isNotBlank()) {
            activeEntry = HandoffEntry(cleanMediaId, mediaType, stream)
        }
    }

    @Synchronized
    fun getAndClearStream(mediaId: String, mediaType: MediaType): AddonStream? {
        val entry = activeEntry ?: return null
        return if (entry.mediaId.equals(mediaId.trim(), ignoreCase = true) && entry.mediaType == mediaType) {
            activeEntry = null
            entry.stream
        } else {
            // Do not clear or return if media identity does not match
            null
        }
    }

    @Synchronized
    fun peekStream(mediaId: String, mediaType: MediaType): AddonStream? {
        val entry = activeEntry ?: return null
        return if (entry.mediaId.equals(mediaId.trim(), ignoreCase = true) && entry.mediaType == mediaType) {
            entry.stream
        } else {
            null
        }
    }

    @Synchronized
    fun clear() {
        activeEntry = null
    }
}
