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
 * REFACTORED: Measurement-based correction system.
 * 
 * Synchronizes the layout world with the viewport by applying a translation 
 * derived from the focused row's layout position.
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

    private val rowPositions = mutableStateMapOf<String, Float>()
    private val rowHeights = mutableStateMapOf<String, Float>()
    
    var viewportHeight by mutableFloatStateOf(0f)

    /**
     * Checks if a specific row has been measured by the layout system.
     */
    fun hasPosition(rowId: String): Boolean = rowPositions.containsKey(rowId)

    fun onRowPositioned(rowId: String, y: Float, height: Float) {
        if (rowPositions[rowId] != y || rowHeights[rowId] != height) {
            rowPositions[rowId] = y
            rowHeights[rowId] = height
        }
    }

    /**
     * MEASUREMENT-BASED CORRECTION
     * Derives camera offset directly from layout position.
     * 
     * Formula: cameraOffsetY = -focusedRowY + anchorY
     * Where anchorY is the natural layout position of the top-most row.
     */
    private fun calculateDeterministicOffset(rowId: String): Float? {
        val rowY = rowPositions[rowId] ?: return null
        if (viewportHeight <= 0f) return null

        // DYNAMIC ANCHOR: The natural layout position of the top-most row.
        // This ensures that layout changes (like Spacers) directly affect the visual anchor.
        val anchorY = rowPositions.values.minOrNull() ?: 0f

        // Calculate target offset to align focused row with the dynamic layout anchor.
        return anchorY - rowY
    }

    suspend fun scrollToRow(rowId: String) {
        calculateDeterministicOffset(rowId)?.let {
            _offsetY.animateTo(it, animationSpec)
        }
    }

    suspend fun snapToRow(rowId: String) {
        calculateDeterministicOffset(rowId)?.let {
            _offsetY.snapTo(it)
        }
    }
}

@Composable
fun rememberTvCameraState(): TvCameraState {
    return remember { TvCameraState() }
}

/**
 * Applies cinematic camera translation.
 */
fun Modifier.tvCameraWorld(state: TvCameraState): Modifier = this
    .graphicsLayer {
        translationY = state.offsetY
    }

/**
 * Captures row bounds for the deterministic camera system.
 */
fun Modifier.onRowPositioned(rowId: String, state: TvCameraState): Modifier = this
    .onGloballyPositioned { layoutCoordinates ->
        state.onRowPositioned(
            rowId = rowId,
            y = layoutCoordinates.positionInParent().y,
            height = layoutCoordinates.size.height.toFloat()
        )
    }
