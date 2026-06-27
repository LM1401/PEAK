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
 * REFACTORED: Bottom-Safe Anchor Model.
 * 
 * Guarantees focused row visibility by anchoring to a safe bottom margin, 
 * ensuring no clipping of expanded cards while maintaining Hero isolation.
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
    
    var viewportHeight by mutableStateOf(0f)

    // Layout Constraints
    private val HERO_BOUNDARY_FRACTION = 0.50f
    private val SAFE_BOTTOM_PADDING_FRACTION = 0.15f // 15% safe zone for peek + overscan
    
    // Safety buffer for card expansion (scale 1.12x + focus glow)
    private val EXPANSION_BUFFER_PX = 48f

    fun onRowPositioned(rowId: String, y: Float, height: Float) {
        rowPositions[rowId] = y
        rowHeights[rowId] = height
    }

    /**
     * BOTTOM-SAFE ANCHOR FORMULA:
     * 1. targetOffset: Positions the expanded bottom above the safe bottom margin.
     * 2. minOffset: Ensures the expanded top never crosses the 50% Hero boundary.
     */
    private fun calculateDeterministicOffset(rowId: String): Float? {
        val rowY = rowPositions[rowId] ?: return null
        val rowH = rowHeights[rowId] ?: 0f
        if (viewportHeight <= 0f) return null

        // Calculate the safe visual floor (above peek zone and screen edge)
        val safeBottomY = viewportHeight * (1f - SAFE_BOTTOM_PADDING_FRACTION)
        
        // Target: Align the bottom of the expanded card to the safe floor
        val expandedBottomWorld = rowY + rowH + EXPANSION_BUFFER_PX
        val targetOffset = safeBottomY - expandedBottomWorld

        // Constraint: Hero Safety Floor (Top must not cross 50% VH)
        val expandedTopWorld = rowY - EXPANSION_BUFFER_PX
        val minOffsetForHeroSafety = (viewportHeight * HERO_BOUNDARY_FRACTION) - expandedTopWorld

        // Final deterministic result: Prioritize Hero safety, but anchor to bottom safe zone.
        return kotlin.math.max(targetOffset, minOffsetForHeroSafety)
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
