package com.example.peak.data.addon

import android.util.Log
import com.example.peak.domain.model.AddonBehaviorHints
import com.example.peak.domain.model.AddonCatalog
import com.example.peak.domain.model.AddonManifest
import com.example.peak.domain.model.AddonResource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Dedicated network client for fetching and parsing Stremio add-on manifests off the main thread.
 * Enforces 10-second connect/read timeouts, custom resource deserialization, and safe error handling.
 */
class AddonApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(AddonResource::class.java, AddonResourceDeserializer())
        .create()

    suspend fun fetchManifest(manifestUrl: String): Result<AddonManifest> = withContext(ioDispatcher) {
        val normalizedUrl = AddonStorage.normalizeAddonUrl(manifestUrl)
        if (normalizedUrl.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Manifest URL cannot be blank"))
        }

        if (!normalizedUrl.startsWith("http://", ignoreCase = true) && !normalizedUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Unsupported URL scheme. Only HTTP and HTTPS are allowed."))
        }

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("HTTP error ${response.code}: ${response.message}")
                    )
                }

                val bodyString = response.body?.string()
                if (bodyString.isNull_or_blank()) {
                    return@withContext Result.failure(IllegalStateException("Received empty manifest response"))
                }

                val manifest = gson.fromJson(bodyString, AddonManifest::class.java)
                if (manifest == null || manifest.id.isNullOrBlank() || manifest.name.isNullOrBlank() || manifest.version.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Manifest is missing required fields (id, name, or version)")
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val safeManifest = manifest.copy(
                    resources = (manifest.resources as List<AddonResource>?) ?: emptyList(),
                    types = (manifest.types as List<String>?) ?: emptyList(),
                    catalogs = (manifest.catalogs as List<AddonCatalog>?) ?: emptyList(),
                    idPrefixes = (manifest.idPrefixes as List<String>?) ?: emptyList(),
                    behaviorHints = (manifest.behaviorHints as AddonBehaviorHints?) ?: AddonBehaviorHints()
                )

                return@withContext Result.success(safeManifest)
            }
        } catch (e: Exception) {
            Log.e("ADDON_API_CLIENT", "Failed to fetch manifest from $normalizedUrl", e)
            return@withContext Result.failure(e)
        }
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}

/**
 * Custom deserializer handling both plain string resource items ("catalog", "meta") and
 * structured object resource items ({"name": "stream", "types": ["movie"]}) in Stremio manifests.
 */
class AddonResourceDeserializer : JsonDeserializer<AddonResource> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AddonResource {
        return if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
            AddonResource(name = json.asString)
        } else if (json.isJsonObject) {
            val obj = json.asJsonObject
            val name = obj.get("name")?.asString ?: ""
            val types = obj.getAsJsonArray("types")?.mapNotNull { it.asString } ?: emptyList()
            val idPrefixes = obj.getAsJsonArray("idPrefixes")?.mapNotNull { it.asString } ?: emptyList()
            AddonResource(name = name, types = types, idPrefixes = idPrefixes)
        } else {
            AddonResource(name = "")
        }
    }
}
