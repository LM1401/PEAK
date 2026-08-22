package com.example.peak.ui.screens.home

import androidx.compose.ui.unit.dp

/**
 * Authoritative Geometry Contract for PEAK Home Screen V2.
 * 
 * Invariants:
 * 1. ROW_SLOT_HEIGHT = Structural height of MovieRow + inter-row spacing.
 * 2. VIEWPORT_HEIGHT = One Full Row + Peek.
 * 3. HERO_BOTTOM_ANCHOR = Starting Y position of the content viewport.
 */
object HomeConstants {
    val HOME_ROW_SLOT_HEIGHT = 320.dp
    val HOME_ROW_SPACING = 24.dp
    val HOME_VIEWPORT_PEEK = 80.dp
    val HOME_VIEWPORT_HEIGHT = 400.dp // Slot (320) + Peek (80)
    val HOME_HERO_BOTTOM_ANCHOR = 200.dp
}
