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
    fun `test mostly opaque bitmap with one transparent pixel returns true`() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)
        bitmap.setPixel(5, 5, Color.TRANSPARENT)
        assertTrue(bitmap.hasActualTransparency())
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
