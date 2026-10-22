package com.example.peak.data.addon

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddonApiClientTest {

    private fun createMockClient(statusCode: Int = 200, responseJson: String = ""): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body(responseJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()
    }

    @Test
    fun fetchManifest_validJson_plainStringResources_parsesSuccessfully() = runTest {
        val json = """
            {
              "id": "org.stremio.cinemeta",
              "version": "3.0.12",
              "name": "Cinemeta",
              "description": "Official metadata provider",
              "resources": ["catalog", "meta", "subtitles"],
              "types": ["movie", "series"],
              "catalogs": [
                { "type": "movie", "id": "top", "name": "Top Movies" }
              ]
            }
        """.trimIndent()

        val client = AddonApiClient(createMockClient(200, json))
        val result = client.fetchManifest("https://v4-cinemeta.strem.io/manifest.json")

        assertTrue(result.isSuccess)
        val manifest = result.getOrNull()
        assertNotNull(manifest)
        assertEquals("org.stremio.cinemeta", manifest?.id)
        assertEquals("3.0.12", manifest?.version)
        assertEquals("Cinemeta", manifest?.name)
        assertEquals(3, manifest?.resources?.size)
        assertEquals("catalog", manifest?.resources?.get(0)?.name)
        assertEquals("meta", manifest?.resources?.get(1)?.name)
        assertEquals("subtitles", manifest?.resources?.get(2)?.name)
        assertEquals(1, manifest?.catalogs?.size)
        assertEquals("top", manifest?.catalogs?.get(0)?.id)
    }

    @Test
    fun fetchManifest_validJson_structuredObjectResources_parsesSuccessfully() = runTest {
        val json = """
            {
              "id": "org.torrentio.addon",
              "version": "1.0.8",
              "name": "Torrentio",
              "description": "Torrent streams provider",
              "resources": [
                {
                  "name": "stream",
                  "types": ["movie", "series"],
                  "idPrefixes": ["tt"]
                }
              ],
              "types": ["movie", "series"],
              "behaviorHints": {
                "configurable": true,
                "adult": false
              }
            }
        """.trimIndent()

        val client = AddonApiClient(createMockClient(200, json))
        val result = client.fetchManifest("https://torrentio.strem.fun/manifest.json")

        assertTrue(result.isSuccess)
        val manifest = result.getOrNull()
        assertNotNull(manifest)
        assertEquals("org.torrentio.addon", manifest?.id)
        assertEquals(1, manifest?.resources?.size)

        val streamRes = manifest?.resources?.get(0)
        assertEquals("stream", streamRes?.name)
        assertEquals(listOf("movie", "series"), streamRes?.types)
        assertEquals(listOf("tt"), streamRes?.idPrefixes)
        assertTrue(manifest?.behaviorHints?.configurable == true)
    }

    @Test
    fun fetchManifest_mixedStringAndObjectResources_parsesSuccessfully() = runTest {
        val json = """
            {
              "id": "org.mixed.addon",
              "version": "2.0.0",
              "name": "Mixed Addon",
              "resources": [
                "catalog",
                {
                  "name": "stream",
                  "types": ["movie"]
                }
              ]
            }
        """.trimIndent()

        val client = AddonApiClient(createMockClient(200, json))
        val result = client.fetchManifest("https://mixed.addon.org/manifest.json")

        assertTrue(result.isSuccess)
        val manifest = result.getOrNull()
        assertEquals(2, manifest?.resources?.size)
        assertEquals("catalog", manifest?.resources?.get(0)?.name)
        assertEquals("stream", manifest?.resources?.get(1)?.name)
        assertEquals(listOf("movie"), manifest?.resources?.get(1)?.types)
    }

    @Test
    fun fetchManifest_optionalFieldsMissing_parsesSuccessfully() = runTest {
        val json = """
            {
              "id": "org.minimal.addon",
              "version": "1.0.0",
              "name": "Minimal Addon"
            }
        """.trimIndent()

        val client = AddonApiClient(createMockClient(200, json))
        val result = client.fetchManifest("https://minimal.addon.org/manifest.json")

        assertTrue(result.isSuccess)
        val manifest = result.getOrNull()
        assertEquals("org.minimal.addon", manifest?.id)
        assertTrue(manifest?.resources?.isEmpty() == true)
        assertTrue(manifest?.catalogs?.isEmpty() == true)
    }

    @Test
    fun fetchManifest_malformedJson_returnsFailure() = runTest {
        val malformedJson = "{ \"id\": \"test\", broken_json... "
        val client = AddonApiClient(createMockClient(200, malformedJson))

        val result = client.fetchManifest("https://broken.addon.org/manifest.json")
        assertTrue(result.isFailure)
    }

    @Test
    fun fetchManifest_missingRequiredFields_returnsFailure() = runTest {
        val jsonMissingName = """
            {
              "id": "org.incomplete.addon",
              "version": "1.0.0"
            }
        """.trimIndent()

        val client = AddonApiClient(createMockClient(200, jsonMissingName))
        val result = client.fetchManifest("https://incomplete.addon.org/manifest.json")

        assertTrue(result.isFailure)
        assertEquals("Manifest is missing required fields (id, name, or version)", result.exceptionOrNull()?.message)
    }

    @Test
    fun fetchManifest_http404Error_returnsFailure() = runTest {
        val client = AddonApiClient(createMockClient(404, "Not Found"))
        val result = client.fetchManifest("https://missing.addon.org/manifest.json")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("HTTP error 404") == true)
    }

    @Test
    fun fetchManifest_invalidUrlScheme_returnsFailure() = runTest {
        val client = AddonApiClient(createMockClient(200, "{}"))
        val result = client.fetchManifest("ftp://invalid.scheme/manifest.json")

        assertTrue(result.isFailure)
        assertEquals("Unsupported URL scheme. Only HTTP and HTTPS are allowed.", result.exceptionOrNull()?.message)
    }
}
