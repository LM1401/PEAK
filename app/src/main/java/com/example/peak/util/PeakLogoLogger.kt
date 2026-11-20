package com.example.peak.util

import android.util.Log
import com.example.peak.BuildConfig

/**
 * Debug-only diagnostic logger for PEAK logo provenance and discovery tracing.
 * Guarantees zero output in release builds by checking [BuildConfig.DEBUG].
 */
object PeakLogoLogger {
    const val TAG = "PEAK_LOGO_DEBUG"

    inline fun d(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}
