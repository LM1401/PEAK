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
    Box(modifier = modifier.fillMaxSize()) {
        // 1. Left Readability Fade (Hero text)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0.0f to Color.Black.copy(alpha = 0.85f),
                        0.4f to Color.Black.copy(alpha = 0.4f),
                        0.7f to Color.Transparent
                    )
                )
        )

        // 2. Full-Screen Depth Blend (Middle transition)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.6f),
                        0.3f to Color.Black.copy(alpha = 0.3f),
                        0.5f to Color.Transparent,
                        0.8f to Color.Black.copy(alpha = 0.6f),
                        1.0f to Color.Black
                    )
                )
        )

        // 3. Subtle Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        ),
                        radius = 1200f
                    )
                )
        )
    }
}
