package com.example.peak.ui.screens.movies

import androidx.compose.ui.unit.dp

/**
 * Movies Screen Specific Geometry and Layout Constants.
 * Dedicated constants to prevent modifying shared HomeConstants.kt.
 */
object MoviesConstants {
    val CONTENT_START_PADDING = 140.dp
    val CONTENT_END_PADDING = 48.dp

    // Collection Cards Geometry (2:1 Ratio)
    val COLLECTION_CARD_WIDTH = 220.dp
    val COLLECTION_CARD_HEIGHT = 110.dp
    val COLLECTION_CARD_SPACING = 16.dp

    // Landscape Movie Cards Geometry (16:9 Ratio)
    val LANDSCAPE_CARD_WIDTH = 210.dp
    val LANDSCAPE_CARD_HEIGHT = 118.dp
    val LANDSCAPE_CARD_SPACING = 16.dp
    val LANDSCAPE_CARD_CORNER_RADIUS = 10.dp

    // Top 10 Composite Cards Geometry
    val TOP10_TOTAL_WIDTH = 245.dp
    val TOP10_ARTWORK_WIDTH = 190.dp
    val TOP10_ARTWORK_HEIGHT = 107.dp
    val TOP10_CARD_SPACING = 16.dp

    // Slot Height for Movies TvCameraSystem
    val MOVIES_ROW_SLOT_HEIGHT = 180.dp
}
