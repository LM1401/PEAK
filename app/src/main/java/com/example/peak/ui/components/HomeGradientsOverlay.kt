package com.example.peak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Optimized cinematic gradients for Netflix TV 2024 style.
 * Focuses on smooth transitions between the hero header and movie rows.
 */
@Composable
fun HomeGradientsOverlay(
    modifier: Modifier = Modifier
) {
    val navyDark = Color(0xFF040B16)
    val navyMedium = Color(0xFF061124)
    val navyLight = Color(0xFF0A1C36)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Left Sidebar & Hero Content Readability Fade
        // Blends the sidebar into the hero while keeping text readable.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to navyDark.copy(alpha = 0.95f),
                        0.2f to navyDark.copy(alpha = 0.85f),
                        0.4f to navyDark.copy(alpha = 0.50f),
                        0.7f to Color.Transparent
                    )
                )
        )

        // 2. Vertical Blend (Hero to Rows)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to navyDark.copy(alpha = 0.30f),
                        0.4f to Color.Transparent,
                        0.6f to navyMedium.copy(alpha = 0.40f),
                        1.0f to navyDark.copy(alpha = 0.90f)
                    )
                )
        )

        // 3. Ambient Blue Vignette (Bottom-Right lighting)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            navyLight.copy(alpha = 0.15f)
                        ),
                        center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        radius = 1500f
                    )
                )
        )
    }
}
