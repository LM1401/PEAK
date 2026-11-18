package com.example.peak.ui.image

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

/**
 * Result data class for candidate transparent border cropping calculations.
 */
data class CropResult(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val cropLeft: Int,
    val cropTop: Int,
    val cropRight: Int,
    val cropBottom: Int,
    val cropWidth: Int,
    val cropHeight: Int,
    val areaSavedPct: Float,
    val shouldTrim: Boolean
)

/**
 * Stage 10E Production-Grade Isolated Prototype: Custom Coil 2.7.0 Bitmap Transformation.
 * Automatically detects and trims 4-sided transparent canvas borders on background thread (Dispatchers.IO).
 * Uses row-buffered pixel scanning (2KB memory overhead per 500px image) to eliminate GC heap allocation churn.
 * Preserves 100% exact bounds for detached artwork, floating elements, and multi-group title text.
 * Integrates cleanly with Coil's MemoryCache and DiskCache via precise cacheKey identity.
 */
class AutoTrimTransparentBordersTransformation(
    alphaThreshold: Int = 16,
    paddingPx: Int = 4,
    minAreaSavedPct: Float = 5.0f
) : Transformation {

    val alphaThreshold: Int = alphaThreshold.coerceIn(0, 255)
    val paddingPx: Int = maxOf(0, paddingPx)
    val minAreaSavedPct: Float = minAreaSavedPct.coerceIn(0.0f, 100.0f)

    override val cacheKey: String =
        "auto_trim_v3_th${this.alphaThreshold}_pad${this.paddingPx}_minArea${this.minAreaSavedPct}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        if (width <= 0 || height <= 0) return input

        // Defensive handling for Hardware Bitmaps
        val safeInput = if (input.config == Bitmap.Config.HARDWARE) {
            input.copy(Bitmap.Config.ARGB_8888, false) ?: return input
        } else {
            input
        }

        val cropResult = calculateCropBitmap(
            bitmap = safeInput,
            alphaThreshold = alphaThreshold,
            paddingPx = paddingPx,
            minAreaSavedPct = minAreaSavedPct
        )

        if (!cropResult.shouldTrim) {
            return input
        }

        val cropped = Bitmap.createBitmap(
            safeInput,
            cropResult.cropLeft,
            cropResult.cropTop,
            cropResult.cropWidth,
            cropResult.cropHeight
        )
        return cropped
    }
}

/**
 * Low-memory row-buffered candidate crop calculation directly on Android Bitmap instances.
 * Allocates only 1 row buffer (width * 4 bytes = 2KB for a 500px wide logo), eliminating MB-scale array allocations.
 */
internal fun calculateCropBitmap(
    bitmap: Bitmap,
    alphaThreshold: Int,
    paddingPx: Int,
    minAreaSavedPct: Float
): CropResult {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) {
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    val safeThreshold = alphaThreshold.coerceIn(0, 255)
    val safePadding = maxOf(0, paddingPx)
    val safeMinArea = minAreaSavedPct.coerceIn(0.0f, 100.0f)

    val row = IntArray(width)

    // 1. Top boundary (minY)
    var minY = height
    for (y in 0 until height) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        var hasVisible = false
        for (x in 0 until width) {
            val alpha = (row[x] ushr 24) and 0xFF
            if (alpha > safeThreshold) {
                hasVisible = true
                break
            }
        }
        if (hasVisible) {
            minY = y
            break
        }
    }

    if (minY == height) { // Fully transparent
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    // 2. Bottom boundary (maxY)
    var maxY = -1
    for (y in height - 1 downTo minY) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        var hasVisible = false
        for (x in 0 until width) {
            val alpha = (row[x] ushr 24) and 0xFF
            if (alpha > safeThreshold) {
                hasVisible = true
                break
            }
        }
        if (hasVisible) {
            maxY = y
            break
        }
    }

    if (maxY < minY) {
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    // 3. Left boundary (minX) & Right boundary (maxX) within y range [minY..maxY]
    var minX = width
    var maxX = -1
    for (y in minY..maxY) {
        bitmap.getPixels(row, 0, width, 0, y, width, 1)
        for (x in 0 until width) {
            val alpha = (row[x] ushr 24) and 0xFF
            if (alpha > safeThreshold) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
            }
        }
    }

    if (minX > maxX) {
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    // Safety-padded crop bounds
    val cropLeft = maxOf(0, minX - safePadding)
    val cropTop = maxOf(0, minY - safePadding)
    val cropRight = minOf(width, maxX + 1 + safePadding)
    val cropBottom = minOf(height, maxY + 1 + safePadding)

    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop

    if (cropWidth <= 0 || cropHeight <= 0) {
        return CropResult(minX, maxX, minY, maxY, 0, 0, width, height, width, height, 0f, false)
    }

    val isFullCanvas = (cropLeft == 0 && cropTop == 0 && cropWidth == width && cropHeight == height)
    val originalArea = (width * height).toFloat()
    val croppedArea = (cropWidth * cropHeight).toFloat()
    val areaSavedPct = ((originalArea - croppedArea) / originalArea) * 100f

    val shouldTrim = !isFullCanvas && (areaSavedPct >= safeMinArea)

    return CropResult(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
        cropLeft = cropLeft,
        cropTop = cropTop,
        cropRight = cropRight,
        cropBottom = cropBottom,
        cropWidth = cropWidth,
        cropHeight = cropHeight,
        areaSavedPct = areaSavedPct,
        shouldTrim = shouldTrim
    )
}

/**
 * Pure Kotlin candidate crop calculation over IntArray for unit testing detached components without Android Bitmap stubs.
 */
internal fun calculateCrop(
    pixels: IntArray,
    width: Int,
    height: Int,
    alphaThreshold: Int,
    paddingPx: Int,
    minAreaSavedPct: Float
): CropResult {
    if (width <= 0 || height <= 0 || pixels.size < width * height) {
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    val safeThreshold = alphaThreshold.coerceIn(0, 255)
    val safePadding = maxOf(0, paddingPx)
    val safeMinArea = minAreaSavedPct.coerceIn(0.0f, 100.0f)

    // 1. Top boundary (minY)
    var minY = height
    for (y in 0 until height) {
        val rowOffset = y * width
        var hasVisiblePixel = false
        for (x in 0 until width) {
            val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
            if (alpha > safeThreshold) {
                hasVisiblePixel = true
                break
            }
        }
        if (hasVisiblePixel) {
            minY = y
            break
        }
    }

    if (minY == height) { // Fully transparent
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    // 2. Bottom boundary (maxY)
    var maxY = -1
    for (y in height - 1 downTo minY) {
        val rowOffset = y * width
        var hasVisiblePixel = false
        for (x in 0 until width) {
            val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
            if (alpha > safeThreshold) {
                hasVisiblePixel = true
                break
            }
        }
        if (hasVisiblePixel) {
            maxY = y
            break
        }
    }

    if (maxY < minY) {
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    // 3. Left boundary (minX) & Right boundary (maxX) within y range [minY..maxY]
    var minX = width
    var maxX = -1
    for (y in minY..maxY) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
            if (alpha > safeThreshold) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
            }
        }
    }

    if (minX > maxX) {
        return CropResult(0, -1, 0, -1, 0, 0, width, height, width, height, 0f, false)
    }

    // Safety-padded crop bounds
    val cropLeft = maxOf(0, minX - safePadding)
    val cropTop = maxOf(0, minY - safePadding)
    val cropRight = minOf(width, maxX + 1 + safePadding)
    val cropBottom = minOf(height, maxY + 1 + safePadding)

    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop

    if (cropWidth <= 0 || cropHeight <= 0) {
        return CropResult(minX, maxX, minY, maxY, 0, 0, width, height, width, height, 0f, false)
    }

    val isFullCanvas = (cropLeft == 0 && cropTop == 0 && cropWidth == width && cropHeight == height)
    val originalArea = (width * height).toFloat()
    val croppedArea = (cropWidth * cropHeight).toFloat()
    val areaSavedPct = ((originalArea - croppedArea) / originalArea) * 100f

    val shouldTrim = !isFullCanvas && (areaSavedPct >= safeMinArea)

    return CropResult(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
        cropLeft = cropLeft,
        cropTop = cropTop,
        cropRight = cropRight,
        cropBottom = cropBottom,
        cropWidth = cropWidth,
        cropHeight = cropHeight,
        areaSavedPct = areaSavedPct,
        shouldTrim = shouldTrim
    )
}
