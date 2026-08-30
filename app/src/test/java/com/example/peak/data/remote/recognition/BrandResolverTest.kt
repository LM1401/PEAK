package com.example.peak.data.remote.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BrandResolverTest {

    @Test
    fun `resolve matches by ID first`() {
        val result = BrandResolver.resolve("Something Else", 2, BrandSignalType.COMPANY) // 2 is Disney
        assertNotNull(result)
        assertEquals("disney", result?.key)
        assertEquals("Disney", result?.displayName)
    }

    @Test
    fun `resolve matches Lionsgate variants`() {
        val variants = listOf("Lionsgate", "Lions Gate", "Lionsgate UK", "Lions Gate Films")
        variants.forEach { name ->
            val result = BrandResolver.resolve(name)
            assertNotNull("Failed to resolve $name", result)
            assertEquals("lionsgate", result?.key)
        }
    }

    @Test
    fun `resolve matches Columbia and Sony correctly`() {
        val columbia = BrandResolver.resolve("Columbia Pictures")
        assertEquals("columbia", columbia?.key)
        assertEquals("Sony Pictures", columbia?.displayName)

        val sony = BrandResolver.resolve("Sony Pictures Entertainment")
        assertEquals("sony_pictures", sony?.key)
    }

    @Test
    fun `resolve matches Warner variants`() {
        val variants = listOf("Warner Bros.", "Warner Brothers", "Warner Bros. Pictures")
        variants.forEach { name ->
            val result = BrandResolver.resolve(name)
            assertEquals("warner_bros", result?.key)
        }
    }

    @Test
    fun `resolve matches Universal and avoids false positives`() {
        assertEquals("universal", BrandResolver.resolve("Universal")?.key)
        assertEquals("universal", BrandResolver.resolve("Universal Pictures")?.key)
        
        // False positive check: "Universal Independent Films" -> "universal independent"
        // Should not match ^universal$
        assertNull(BrandResolver.resolve("Universal Independent Films"))
    }

    @Test
    fun `resolve matches A24`() {
        assertEquals("a24", BrandResolver.resolve("A24")?.key)
        assertEquals("a24", BrandResolver.resolve("A24 Films")?.key)
    }

    @Test
    fun `resolve matches Netflix`() {
        assertEquals("netflix", BrandResolver.resolve("Netflix")?.key)
        assertEquals("netflix", BrandResolver.resolve("Netflix Studios")?.key)
    }

    @Test
    fun `resolve matches Prime Video variants`() {
        assertEquals("amazon", BrandResolver.resolve("Prime Video")?.key)
        assertEquals("amazon", BrandResolver.resolve("Amazon Studios")?.key)
    }

    @Test
    fun `resolve matches Apple TV+`() {
        assertEquals("apple", BrandResolver.resolve("Apple Studios")?.key)
        assertEquals("apple", BrandResolver.resolve("Apple TV+")?.key)
    }

    @Test
    fun `resolve handles complex noise removal`() {
        val name = "Warner Bros. Entertainment UK Ltd."
        assertEquals("warner_bros", BrandResolver.resolve(name)?.key)
    }

    @Test
    fun `resolve supports watch providers`() {
        // Netflix provider ID: 8
        val result = BrandResolver.resolve(id = 8, type = BrandSignalType.PROVIDER)
        assertEquals("netflix", result?.key)
        assertEquals("Netflix Original", result?.displayName)

        // Apple TV+ provider ID: 350
        val appleResult = BrandResolver.resolve(id = 350, type = BrandSignalType.PROVIDER)
        assertEquals("apple", appleResult?.key)
    }

    @Test
    fun `resolve supports release notes via name matching`() {
        val note = "Distributed by Lionsgate"
        val result = BrandResolver.resolve(name = note)
        assertEquals("lionsgate", result?.key)
    }
}
