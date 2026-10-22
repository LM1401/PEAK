package com.example.peak.data.repository

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.peak.data.addon.AddonStorage
import com.example.peak.domain.model.Addon
import com.example.peak.domain.model.AddonManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddonRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefs: AddonFakeSharedPreferences
    private lateinit var storage: AddonStorage
    private lateinit var repository: AddonRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = AddonFakeSharedPreferences()
        storage = AddonStorage(AddonDummyContext(), fakePrefs)
        repository = AddonRepository(storage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun urlNormalization_convertsStremioSchemeAndAppendsManifestJson() {
        val converted1 = AddonStorage.normalizeAddonUrl("stremio://v4-cinemeta.strem.io/manifest.json")
        assertEquals("https://v4-cinemeta.strem.io/manifest.json", converted1)

        val converted2 = AddonStorage.normalizeAddonUrl("https://v4-cinemeta.strem.io")
        assertEquals("https://v4-cinemeta.strem.io/manifest.json", converted2)

        val converted3 = AddonStorage.normalizeAddonUrl("stremio://torrentio.strem.fun/providers=yts/manifest.json")
        assertEquals("https://torrentio.strem.fun/providers=yts/manifest.json", converted3)
    }

    @Test
    fun storage_emptyInitial_returnsEmptyList() {
        assertTrue(storage.getAllAddons().isEmpty())
    }

    @Test
    fun storage_addAndRemove_persistsAndRestores() = runTest {
        val testAddon = Addon(
            id = "addon_1",
            transportUrl = "https://v4-cinemeta.strem.io/manifest.json",
            name = "Cinemeta"
        )

        storage.addAddon(testAddon)
        assertEquals(1, storage.getAllAddons().size)
        assertEquals("Cinemeta", storage.getAllAddons().first().name)

        // Recreate storage instance to verify persistence
        val recreatedStorage = AddonStorage(AddonDummyContext(), fakePrefs)
        assertEquals(1, recreatedStorage.getAllAddons().size)

        // Remove add-on
        storage.removeAddon("addon_1")
        assertTrue(storage.getAllAddons().isEmpty())
    }

    @Test
    fun storage_duplicateAddonUrl_preventsDuplicateEntry() = runTest {
        val addon1 = Addon(id = "1", transportUrl = "stremio://torrentio.strem.fun/manifest.json")
        val addon2 = Addon(id = "2", transportUrl = "https://torrentio.strem.fun/manifest.json")

        storage.addAddon(addon1)
        storage.addAddon(addon2)

        assertEquals(1, storage.getAllAddons().size)
    }

    @Test
    fun storage_differentConfiguredUrls_areStoredAsDistinctEntries() = runTest {
        val config1 = Addon(id = "1", transportUrl = "https://torrentio.strem.fun/providers=yts/manifest.json")
        val config2 = Addon(id = "2", transportUrl = "https://torrentio.strem.fun/providers=yts,eztv/manifest.json")

        storage.addAddon(config1)
        storage.addAddon(config2)

        assertEquals(2, storage.getAllAddons().size)
    }

    @Test
    fun repository_addAddon_updatesStateFlowAndStorage() = runTest {
        val result = repository.addAddon("stremio://v4-cinemeta.strem.io/manifest.json", name = "Cinemeta")
        assertTrue(result.isSuccess)

        val added = result.getOrNull()
        assertNotNull(added)
        assertEquals("https://v4-cinemeta.strem.io/manifest.json", added?.transportUrl)

        assertEquals(1, repository.addons.value.size)
        assertTrue(repository.isAddonInstalled("https://v4-cinemeta.strem.io/manifest.json"))
    }

    @Test
    fun repository_duplicateAddition_returnsFailure() = runTest {
        repository.addAddon("https://v4-cinemeta.strem.io/manifest.json")
        val duplicateResult = repository.addAddon("stremio://v4-cinemeta.strem.io/manifest.json")

        assertTrue(duplicateResult.isFailure)
        assertEquals(1, repository.addons.value.size)
    }

    @Test
    fun repository_updateManifestAndToggle_updatesAddonState() = runTest {
        val addResult = repository.addAddon("https://v4-cinemeta.strem.io/manifest.json")
        val addonId = addResult.getOrThrow().id

        val manifest = AddonManifest(
            id = "org.stremio.cinemeta",
            version = "3.0.0",
            name = "Official Cinemeta",
            description = "Official metadata provider"
        )

        val updateResult = repository.updateAddonManifest(addonId, manifest)
        assertTrue(updateResult.isSuccess)

        val updated = repository.addons.value.first()
        assertEquals("org.stremio.cinemeta", updated.manifestId)
        assertEquals("Official Cinemeta", updated.name)
        assertEquals("3.0.0", updated.version)

        // Toggle add-on
        repository.toggleAddon(addonId, false)
        assertFalse(repository.addons.value.first().isEnabled)
    }

    @Test
    fun repository_fetchAndInstallManifest_success_updatesStorageAndState() = runTest {
        val addResult = repository.addAddon("https://v4-cinemeta.strem.io/manifest.json")
        val addonId = addResult.getOrThrow().id

        val json = """
            {
              "id": "org.stremio.cinemeta",
              "version": "3.0.12",
              "name": "Cinemeta Official",
              "description": "Official metadata add-on"
            }
        """.trimIndent()

        val mockClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(json.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val apiClient = com.example.peak.data.addon.AddonApiClient(mockClient)
        val result = repository.fetchAndInstallManifest(addonId, apiClient)

        assertTrue(result.isSuccess)
        val updated = repository.addons.value.first()
        assertEquals("org.stremio.cinemeta", updated.manifestId)
        assertEquals("Cinemeta Official", updated.name)
        assertEquals("3.0.12", updated.version)
    }

    @Test
    fun repository_fetchAndInstallManifest_failure_leavesAddonIntact() = runTest {
        val addResult = repository.addAddon("https://failing.addon.org/manifest.json", name = "Original Name")
        val addonId = addResult.getOrThrow().id

        val mockClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(500)
                    .message("Server Error")
                    .body("Internal Server Error".toResponseBody("text/plain".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val apiClient = com.example.peak.data.addon.AddonApiClient(mockClient)
        val result = repository.fetchAndInstallManifest(addonId, apiClient)

        assertTrue(result.isFailure)
        val intactAddon = repository.addons.value.first()
        assertEquals("Original Name", intactAddon.name)
        assertEquals("https://failing.addon.org/manifest.json", intactAddon.transportUrl)
    }
}

private class AddonFakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) map[key] = value ?: ""
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) map[key] = values ?: mutableSetOf<String>()
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) map.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            map.clear()
            return this
        }
        override fun commit(): Boolean = true
        override fun apply() {}
    }
}

private class AddonDummyContext : ContextWrapper(null)
