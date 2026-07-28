package com.example.peak.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * TV Camera System State.
 * REFACTORED: Index-based slot alignment.
 * 
 * Enforces deterministic row snapping by calculating offsets from discrete indices.
 * Uses Slot density-awareness to support 720p, 1080p, and 4K resolution consistency.
 */
class TvCameraState(
    private val density: Density,
    private val animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
) {
    // SLOT_HEIGHT: 287dp (Row Content) + 24dp (Spacing)
    private val slotHeightPx = with(density) { 311.dp.toPx() }

    private val _offsetY = Animatable(0f)
    val offsetY: Float get() = _offsetY.value

    /**
     * SNAPS the viewport to exactly one row index.
     * Guaranteed to stop at deterministic pixel boundaries.
     */
    suspend fun scrollToRow(index: Int) {
        val targetOffset = -(index * slotHeightPx)
        _offsetY.animateTo(targetOffset, animationSpec)
    }

    suspend fun snapToRow(index: Int) {
        val targetOffset = -(index * slotHeightPx)
        _offsetY.snapTo(targetOffset)
    }
}

@Composable
fun rememberTvCameraState(): TvCameraState {
    val density = LocalDensity.current
    return remember(density) { TvCameraState(density) }
}

/**
 * Applies cinematic slot-aligned camera translation.
 */
fun Modifier.tvCameraWorld(state: TvCameraState): Modifier = this
    .graphicsLayer {
        translationY = state.offsetY
    }

/**
 * Captures row bounds - Kept as identity for compatibility, but logic is now index-driven.
 */
fun Modifier.onRowPositioned(rowId: String, state: TvCameraState): Modifier = this
