package com.example.peak.data.repository

import android.content.ContextWrapper
import android.content.SharedPreferences
import com.example.peak.data.settings.SettingsStorage
import com.example.peak.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePrefs: SettingsFakeSharedPreferences
    private lateinit var storage: SettingsStorage
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePrefs = SettingsFakeSharedPreferences()
        storage = SettingsStorage(SettingsDummyContext(), fakePrefs)
        repository = SettingsRepository(storage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun storage_defaultValues_areCorrect() {
        assertTrue(storage.getAutoPlayEnabled())
        assertFalse(storage.getDebugModeEnabled())
    }

    @Test
    fun storage_updates_arePersisted() {
        storage.setAutoPlayEnabled(false)
        storage.setDebugModeEnabled(true)

        assertFalse(storage.getAutoPlayEnabled())
        assertTrue(storage.getDebugModeEnabled())

        // Recreate storage instance to verify persistence across instances
        val recreatedStorage = SettingsStorage(SettingsDummyContext(), fakePrefs)
        assertFalse(recreatedStorage.getAutoPlayEnabled())
        assertTrue(recreatedStorage.getDebugModeEnabled())
    }

    @Test
    fun repository_initialState_matchesStorage() {
        assertEquals(SettingsStorage.DEFAULT_AUTO_PLAY, repository.autoPlayEnabled.value)
        assertEquals(SettingsStorage.DEFAULT_DEBUG_MODE, repository.debugModeEnabled.value)
    }

    @Test
    fun repository_updates_persistAndEmitNewValues() = runTest {
        repository.setAutoPlayEnabled(false)
        repository.setDebugModeEnabled(true)

        assertFalse(repository.autoPlayEnabled.value)
        assertTrue(repository.debugModeEnabled.value)

        // Verify values persisted in storage
        assertFalse(storage.getAutoPlayEnabled())
        assertTrue(storage.getDebugModeEnabled())
    }

    @Test
    fun viewModel_toggleInteractions_updateRepositoryAndState() = runTest {
        val viewModel = SettingsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialAutoPlay = viewModel.uiState.value.items.find { it.id == "auto_play" }?.isEnabled
        assertEquals(true, initialAutoPlay)

        viewModel.onToggleItem("auto_play")
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedAutoPlay = viewModel.uiState.value.items.find { it.id == "auto_play" }?.isEnabled
        assertEquals(false, updatedAutoPlay)
        assertFalse(repository.autoPlayEnabled.value)
    }
}

private class SettingsFakeSharedPreferences : SharedPreferences {
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

private class SettingsDummyContext : ContextWrapper(null)
