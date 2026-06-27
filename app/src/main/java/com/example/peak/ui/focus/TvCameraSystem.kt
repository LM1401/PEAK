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
 * REFACTORED: Feasible Layout Solver.
 * 
 * Guarantees focused row visibility by resolving constraints between 
 * the Hero Zone (Top) and the Peek Zone (Bottom).
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

    // Layout Constraints (Fractions)
    private val HERO_ZONE_FRACTION = 0.50f
    private val PEEK_ZONE_FRACTION = 0.20f
    
    // Safety buffer for card expansion (scale 1.12x + focus glow)
    private val EXPANSION_BUFFER_PX = 48f

    fun onRowPositioned(rowId: String, y: Float, height: Float) {
        if (rowPositions[rowId] != y || rowHeights[rowId] != height) {
            rowPositions[rowId] = y
            rowHeights[rowId] = height
        }
    }

    /**
     * FEASIBLE LAYOUT SOLVER
     * 1. Compute Visual Footprint: logical bounds + expansion scaling.
     * 2. Determine Safe Zones: Hero Ceiling (50%) and Peek Floor (80%).
     * 3. Resolve Constraints: Prioritize visibility and Hero isolation.
     */
    private fun calculateDeterministicOffset(rowId: String): Float? {
        val rowY = rowPositions[rowId] ?: return null
        val rowH = rowHeights[rowId] ?: 0f
        if (viewportHeight <= 0f) return null

        // 1. Compute visual footprint (including scale 1.12x expansion)
        val expandedTop = rowY - EXPANSION_BUFFER_PX
        val expandedBottom = rowY + rowH + EXPANSION_BUFFER_PX

        // 2. Define Zones
        val heroCeiling = viewportHeight * HERO_ZONE_FRACTION
        val idealFloor = viewportHeight * (1f - PEEK_ZONE_FRACTION)
        val hardFloor = viewportHeight // Absolute screen edge

        // 3. Define Offsets
        // minOffset: Smallest translation to keep top below Hero.
        val minOffsetForHero = heroCeiling - expandedTop
        
        // idealMaxOffset: Largest translation to keep bottom above Peek Zone.
        val idealMaxOffset = idealFloor - expandedBottom
        
        // hardMaxOffset: Largest translation to keep bottom above screen edge (no clipping).
        val hardMaxOffset = hardFloor - expandedBottom

        // 4. Resolve Range
        return if (minOffsetForHero <= idealMaxOffset) {
            // Case A: Row fits within Focus Zone (between Hero and Peek Zone).
            // Strategy: Align to the bottom of the Focus Zone to maximize cinematic Hero space.
            idealMaxOffset
        } else if (minOffsetForHero <= hardMaxOffset) {
            // Case B: Row fits below Hero but requires the Peek Zone space to be visible.
            // Strategy: Prioritize Hero isolation; use minOffset to anchor top at 50% VH.
            minOffsetForHero
        } else {
            // Case C: Row is too tall to fit between Hero and screen edge.
            // Strategy: Relax Hero constraint to prevent clipping. 
            // We use hardMaxOffset to ensure the bottom is fully visible at the screen edge.
            hardMaxOffset
        }
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
