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
        assertEquals("disney", result?.definition?.key)
        assertEquals("Disney", result?.definition?.displayName)
    }

    @Test
    fun `resolve matches Lionsgate variants`() {
        val variants = listOf("Lionsgate", "Lions Gate", "Lionsgate UK", "Lions Gate Films")
        variants.forEach { name ->
            val result = BrandResolver.resolve(name)
            assertNotNull("Failed to resolve $name", result)
            assertEquals("lionsgate", result?.definition?.key)
        }
    }

    @Test
    fun `resolve matches Columbia and Sony correctly`() {
        val columbia = BrandResolver.resolve("Columbia Pictures")
        assertEquals("columbia", columbia?.definition?.key)
        assertEquals("Sony Pictures", columbia?.definition?.displayName)

        val sony = BrandResolver.resolve("Sony Pictures Entertainment")
        assertEquals("sony_pictures", sony?.definition?.key)
    }

    @Test
    fun `resolve matches Warner variants`() {
        val variants = listOf("Warner Bros.", "Warner Brothers", "Warner Bros. Pictures")
        variants.forEach { name ->
            val result = BrandResolver.resolve(name)
            assertEquals("warner_bros", result?.definition?.key)
        }
    }

    @Test
    fun `resolve matches Universal and avoids false positives`() {
        assertEquals("universal", BrandResolver.resolve("Universal")?.definition?.key)
        assertEquals("universal", BrandResolver.resolve("Universal Pictures")?.definition?.key)
        
        // False positive check: "Universal Independent Films" -> "universal independent"
        // Should not match ^universal$
        assertNull(BrandResolver.resolve("Universal Independent Films"))
    }

    @Test
    fun `resolve matches A24`() {
        assertEquals("a24", BrandResolver.resolve("A24")?.definition?.key)
        assertEquals("a24", BrandResolver.resolve("A24 Films")?.definition?.key)
    }

    @Test
    fun `resolve matches Netflix`() {
        assertEquals("netflix", BrandResolver.resolve("Netflix")?.definition?.key)
        assertEquals("netflix", BrandResolver.resolve("Netflix Studios")?.definition?.key)
    }

    @Test
    fun `resolve matches Prime Video variants`() {
        assertEquals("amazon", BrandResolver.resolve("Prime Video")?.definition?.key)
        assertEquals("amazon", BrandResolver.resolve("Amazon Studios")?.definition?.key)
    }

    @Test
    fun `resolve matches Apple TV+`() {
        assertEquals("apple", BrandResolver.resolve("Apple Studios")?.definition?.key)
        assertEquals("apple", BrandResolver.resolve("Apple TV+")?.definition?.key)
    }

    @Test
    fun `resolve handles complex noise removal`() {
        val name = "Warner Bros. Entertainment UK Ltd."
        assertEquals("warner_bros", BrandResolver.resolve(name)?.definition?.key)
    }

    @Test
    fun `resolve supports watch providers`() {
        // Netflix provider ID: 8
        val result = BrandResolver.resolve(id = 8, type = BrandSignalType.PROVIDER)
        assertEquals("netflix", result?.definition?.key)
        assertEquals("Netflix Original", result?.definition?.displayName)

        // Apple TV+ provider ID: 350
        val appleResult = BrandResolver.resolve(id = 350, type = BrandSignalType.PROVIDER)
        assertEquals("apple", appleResult?.definition?.key)
    }

    @Test
    fun `resolve supports release notes via name matching`() {
        val note = "Distributed by Lionsgate"
        val result = BrandResolver.resolve(name = note)
        assertEquals("lionsgate", result?.definition?.key)
    }

    @Test
    fun `resolve Paramount vs Paramount Plus specificity and IDs`() {
        // ID 4 -> paramount
        val pCompany = BrandResolver.resolve(id = 4, type = BrandSignalType.COMPANY)
        assertEquals("paramount", pCompany?.definition?.key)

        // ID 433 as NETWORK -> paramount_plus
        val pNetwork = BrandResolver.resolve(id = 433, type = BrandSignalType.NETWORK)
        assertEquals("paramount_plus", pNetwork?.definition?.key)

        // ID 531 as PROVIDER -> paramount_plus
        val pProvider = BrandResolver.resolve(id = 531, type = BrandSignalType.PROVIDER)
        assertEquals("paramount_plus", pProvider?.definition?.key)

        // Name matching
        assertEquals("paramount_plus", BrandResolver.resolve("Paramount+")?.definition?.key)
        assertEquals("paramount_plus", BrandResolver.resolve("Paramount Plus")?.definition?.key)
        assertEquals("paramount", BrandResolver.resolve("Paramount Pictures")?.definition?.key)
        assertEquals("paramount", BrandResolver.resolve("Paramount")?.definition?.key)
    }

    @Test
    fun `resolve MGM vs MGM Plus specificity and IDs`() {
        // ID 21 -> mgm
        val mgmCompany = BrandResolver.resolve(id = 21, type = BrandSignalType.COMPANY)
        assertEquals("mgm", mgmCompany?.definition?.key)

        // ID 233 as NETWORK -> mgm_plus
        val mgmNetwork = BrandResolver.resolve(id = 233, type = BrandSignalType.NETWORK)
        assertEquals("mgm_plus", mgmNetwork?.definition?.key)

        // ID 34 as PROVIDER -> mgm_plus
        val mgmProvider = BrandResolver.resolve(id = 34, type = BrandSignalType.PROVIDER)
        assertEquals("mgm_plus", mgmProvider?.definition?.key)

        // Name matching
        assertEquals("mgm_plus", BrandResolver.resolve("MGM+")?.definition?.key)
        assertEquals("mgm_plus", BrandResolver.resolve("MGM Plus")?.definition?.key)
        assertEquals("mgm", BrandResolver.resolve("Metro-Goldwyn-Mayer")?.definition?.key)
        assertEquals("mgm", BrandResolver.resolve("MGM")?.definition?.key)
    }

    @Test
    fun `resolve Apple brand IDs and names`() {
        assertEquals("apple", BrandResolver.resolve(id = 350, type = BrandSignalType.PROVIDER)?.definition?.key)
        assertEquals("apple", BrandResolver.resolve(id = 2552, type = BrandSignalType.NETWORK)?.definition?.key)
        assertEquals("apple", BrandResolver.resolve(id = 194232, type = BrandSignalType.COMPANY)?.definition?.key)
        assertEquals("apple", BrandResolver.resolve("Apple TV+")?.definition?.key)
    }
}
