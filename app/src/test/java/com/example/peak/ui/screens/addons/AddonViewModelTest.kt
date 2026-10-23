package com.example.peak.ui.screens.addons

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.peak.data.addon.AddonApiClient
import com.example.peak.data.addon.AddonStorage
import com.example.peak.data.repository.AddonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class AddonViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakePrefs: AddonViewModelFakeSharedPreferences
    private lateinit var storage: AddonStorage
    private lateinit var repository: AddonRepository

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakePrefs = AddonViewModelFakeSharedPreferences()
        storage = AddonStorage(AddonViewModelDummyContext(), fakePrefs, testDispatcher)
        repository = AddonRepository(storage = storage, ioDispatcher = testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createMockApiClient(statusCode: Int, responseJson: String): AddonApiClient {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body(responseJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()
        return AddonApiClient(client, testDispatcher)
    }

    @Test
    fun viewModel_initialState_emitsInstalledAddons() = runTest {
        val viewModel = AddonViewModel(repository, createMockApiClient(200, "{}"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.installedAddons.isEmpty())
    }

    @Test
    fun installAddon_success_addsAndFetchesManifest() = runTest {
        val manifestJson = """
            {
              "id": "org.stremio.cinemeta",
              "version": "3.0.12",
              "name": "Cinemeta Official",
              "description": "Official metadata add-on"
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, manifestJson)
        val viewModel = AddonViewModel(repository, apiClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installAddon("https://v4-cinemeta.strem.io/manifest.json")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.installedAddons.size)
        assertEquals("Cinemeta Official", state.installedAddons.first().name)
        assertNotNull(state.successMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun installAddon_blankUrl_setsErrorMessage() = runTest {
        val viewModel = AddonViewModel(repository, createMockApiClient(200, "{}"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installAddon("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Add-on URL cannot be blank", viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.installedAddons.isEmpty())
    }

    @Test
    fun installAddon_duplicateUrl_setsErrorMessage() = runTest {
        val manifestJson = """
            {
              "id": "org.stremio.cinemeta",
              "version": "3.0.12",
              "name": "Cinemeta"
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, manifestJson)
        val viewModel = AddonViewModel(repository, apiClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installAddon("https://v4-cinemeta.strem.io/manifest.json")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installAddon("stremio://v4-cinemeta.strem.io/manifest.json")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("This add-on is already installed", viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.installedAddons.size)
    }

    @Test
    fun installAddon_manifestFetchFailure_retainsInstalledAddonWithErrorMessage() = runTest {
        val apiClient = createMockApiClient(500, "Server Error")
        val viewModel = AddonViewModel(repository, apiClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installAddon("https://failing.addon.org/manifest.json")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Installed add-on is retained
        assertEquals(1, state.installedAddons.size)
        assertEquals("https://failing.addon.org/manifest.json", state.installedAddons.first().transportUrl)
        assertNull(state.installedAddons.first().manifest)
        // Clear error message reported to user
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("failed to fetch manifest") == true)
    }

    @Test
    fun deleteAddon_removesAddonAndEmitsSuccessMessage() = runTest {
        val manifestJson = """
            {
              "id": "org.stremio.cinemeta",
              "version": "3.0.12",
              "name": "Cinemeta"
            }
        """.trimIndent()

        val apiClient = createMockApiClient(200, manifestJson)
        val viewModel = AddonViewModel(repository, apiClient)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.installAddon("https://v4-cinemeta.strem.io/manifest.json")
        testDispatcher.scheduler.advanceUntilIdle()

        val addonId = viewModel.uiState.value.installedAddons.first().id
        viewModel.deleteAddon(addonId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.installedAddons.isEmpty())
        assertEquals("Add-on removed successfully", state.successMessage)
    }
}

private class AddonViewModelFakeSharedPreferences : SharedPreferences {
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

private class AddonViewModelDummyContext : ContextWrapper(null)
