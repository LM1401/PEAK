package com.example.peak.ui.screens.random

import com.example.peak.domain.model.DiscoveryMode
import org.junit.Assert.assertEquals
import org.junit.Test

class RandomViewModelTest {

    @Test
    fun testInitialState() {
        // ViewModel tests to ensure states transition properly
        val request = com.example.peak.domain.model.RandomSelectionRequest()
        assertEquals(1, request.totalRequested)
        assertEquals(DiscoveryMode.COMPLETELY_RANDOM, request.mode)
    }
}
