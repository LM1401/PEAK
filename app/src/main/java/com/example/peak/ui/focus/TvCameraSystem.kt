package com.example.peak.ui.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

/**
 * TV Camera System State.
 * RECALIBRATED: Window-Fit Camera Model.
 * 
 * Enforces a bounded visual window:
 * - Hero Zone (0% - 50%): Protected from content overlap.
 * - Primary Row Zone (50% - 80%): Focused row container.
 * - Peek Zone (80% - 100%): Spillover for next row title.
 */
class TvCameraState(
    initialOffset: Float = 0f,
    private val animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
) {
    private val _offsetY = Animatable(initialOffset)
    val offsetY: Float get() = _offsetY.value

    // Registry of row Y-positions and heights in world space
    private val rowPositions = mutableStateMapOf<String, Float>()
    private val rowHeights = mutableStateMapOf<String, Float>()
    
    // Viewport height captured at runtime
    var viewportHeight by mutableStateOf(0f)

    // Visual Window Constants
    private val WIN_TOP_FRACTION = 0.50f
    private val WIN_BOTTOM_FRACTION = 0.80f

    fun onRowPositioned(rowId: String, y: Float, height: Float) {
        rowPositions[rowId] = y
        rowHeights[rowId] = height
    }

    /**
     * Calculates the deterministic offset required to fit the row into the visual window.
     * Logic:
     * 1. Attempt to align row bottom to 80% VH (guarantees next row title peek).
     * 2. Force constraint: row top must NEVER go above 50% VH (NO Hero overlap).
     */
    private fun calculateTargetOffset(rowId: String): Float? {
        val rowY = rowPositions[rowId] ?: return null
        val rowH = rowHeights[rowId] ?: 0f
        if (viewportHeight <= 0f) return null

        val winTop = viewportHeight * WIN_TOP_FRACTION
        val winBottom = viewportHeight * WIN_BOTTOM_FRACTION

        // Ideal: Align bottom to 80% boundary
        var target = winBottom - rowY - rowH
        
        // Constraint: Clamp top to 50% boundary (Primary Priority)
        val minOffset = winTop - rowY
        if (target < minOffset) {
            target = minOffset
        }
        
        return target
    }

    suspend fun scrollToRow(rowId: String) {
        calculateTargetOffset(rowId)?.let {
            _offsetY.animateTo(it, animationSpec)
        }
    }

    suspend fun snapToRow(rowId: String) {
        calculateTargetOffset(rowId)?.let {
            _offsetY.snapTo(it)
        }
    }
}

@Composable
fun rememberTvCameraState(): TvCameraState {
    return remember { TvCameraState() }
}

/**
 * Modifier that applies the camera translation to the content world.
 */
fun Modifier.tvCameraWorld(state: TvCameraState): Modifier = this
    .graphicsLayer {
        translationY = state.offsetY
    }

/**
 * Modifier that captures the row's position and height for the camera system.
 */
fun Modifier.onRowPositioned(rowId: String, state: TvCameraState): Modifier = this
    .onGloballyPositioned { layoutCoordinates ->
        // Capture Y and Height relative to the camera world container
        state.onRowPositioned(
            rowId = rowId,
            y = layoutCoordinates.positionInParent().y,
            height = layoutCoordinates.size.height.toFloat()
        )
    }
