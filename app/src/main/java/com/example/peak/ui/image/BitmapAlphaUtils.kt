package com.example.peak.ui.image

import android.graphics.Bitmap

/**
 * Validates if an image contains genuine transparency by scanning pixel alpha values.
 * Uses a single row buffer for O(1) memory overhead.
 * @param alphaThreshold Any pixel with alpha < this value is considered transparent.
 *                     Defaults to 250 to allow for slight anti-aliasing artifacts on solid backgrounds.
 */
internal fun Bitmap.hasActualTransparency(alphaThreshold: Int = 250): Boolean {
    val isHardware = try {
        android.os.Build.VERSION.SDK_INT >= 26 && this.config == Bitmap.Config.HARDWARE
    } catch (e: Exception) {
        false
    } catch (e: NoSuchFieldError) {
        false
    }

    val safeBitmap = if (isHardware) {
        this.copy(Bitmap.Config.ARGB_8888, false) ?: return false
    } else {
        this
    }

    val width = safeBitmap.width
    val height = safeBitmap.height
    if (width <= 0 || height <= 0) return false

    val row = IntArray(width)
    for (y in 0 until height) {
        safeBitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (x in 0 until width) {
            val alpha = (row[x] ushr 24) and 0xFF
            // If we find a pixel that is not fully opaque, the image has transparency.
            if (alpha < alphaThreshold) {
                return true
            }
        }
    }
    return false
}
