package com.example.peak.ui.image

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapAlphaUtilsTest {

    @Test
    fun `test fully opaque bitmap returns false`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        assertFalse(bitmap.hasActualTransparency())
    }

    @Test
    fun `test transparent bitmap returns true`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        assertTrue(bitmap.hasActualTransparency())
    }

    @Test
    fun `test mostly opaque bitmap with one transparent pixel returns false now`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        bitmap.setPixel(5, 5, Color.TRANSPARENT)
        assertFalse(bitmap.hasActualTransparency()) // 1 pixel is < 2% of 10000
    }

    @Test
    fun `test image with exactly required ratio returns true`() {
        val width = 100
        val height = 100
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        // 2% of 10,000 is 200. We'll set 200 pixels to transparent.
        for (i in 0 until 200) {
            bitmap.setPixel(i % width, i / width, Color.TRANSPARENT)
        }
        assertTrue(bitmap.hasActualTransparency(requiredTransparentRatio = 0.02f))
    }

    @Test
    fun `test bitmap just below required transparency ratio returns false`() {
        val width = 101
        val height = 101
        // Total pixels: 10,201. ceil(10201 * 0.02) = 205 required.
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        for (i in 0 until 204) {
            bitmap.setPixel(i % width, i / width, Color.TRANSPARENT)
        }
        assertFalse(bitmap.hasActualTransparency(requiredTransparentRatio = 0.02f))
    }

    @Test
    fun `test bitmap meeting required transparency ratio returns true`() {
        val width = 101
        val height = 101
        // Total pixels: 10,201. ceil(10201 * 0.02) = 205 required.
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        for (i in 0 until 205) {
            bitmap.setPixel(i % width, i / width, Color.TRANSPARENT)
        }
        assertTrue(bitmap.hasActualTransparency(requiredTransparentRatio = 0.02f))
    }

    @Test
    fun `test image with subtle anti-aliased edge returns false if above threshold`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val slightlyTransparentWhite = Color.argb(252, 255, 255, 255)
        bitmap.eraseColor(slightlyTransparentWhite)

        // At threshold 250, alpha 252 should not be considered transparent
        assertFalse(bitmap.hasActualTransparency(alphaThreshold = 250))
    }
}
