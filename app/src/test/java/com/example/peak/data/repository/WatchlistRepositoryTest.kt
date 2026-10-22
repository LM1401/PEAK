package com.example.peak.data.repository

import com.example.peak.data.watchlist.WatchlistStorage
import com.example.peak.domain.model.MediaType
import com.example.peak.domain.model.Movie
import com.example.peak.domain.model.Watchlist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pure test double for WatchlistStorage without Android runtime dependencies.
 */
private class PureFakeWatchlistStorage(
    private var lists: List<Watchlist>
) {
    fun getAllWatchlists(): List<Watchlist> = lists
    fun saveAllWatchlists(watchlists: List<Watchlist>) {
        lists = watchlists
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistRepositoryTest {

    private lateinit var repository: WatchlistRepository

    private val testMovie1 = Movie(
        movieId = "m1",
        mediaType = MediaType.MOVIE,
        name = "Test Movie 1",
        imageUrl = "",
        backdropUrl = ""
    )

    private val testMovie2 = Movie(
        movieId = "m2",
        mediaType = MediaType.MOVIE,
        name = "Test Movie 2",
        imageUrl = "",
        backdropUrl = ""
    )

    @Before
    fun setUp() {
        val defaultList = Watchlist(
            id = WatchlistStorage.DEFAULT_WATCHLIST_ID,
            name = "My Watchlist",
            isDefault = true,
            items = listOf(testMovie1)
        )
        val pureFake = PureFakeWatchlistStorage(listOf(defaultList))

        val storageSubclass = object : WatchlistStorage(DummyContext()) {
            override fun getAllWatchlists(): List<Watchlist> = pureFake.getAllWatchlists()
            override suspend fun saveAllWatchlists(watchlists: List<Watchlist>) = pureFake.saveAllWatchlists(watchlists)
        }

        repository = WatchlistRepository(storageSubclass)
    }

    @Test
    fun defaultWatchlist_cannotBeDeleted() = runTest {
        val result = repository.deleteWatchlist(WatchlistStorage.DEFAULT_WATCHLIST_ID)
        assertTrue(result.isFailure)
        assertEquals("The default 'My Watchlist' cannot be deleted.", result.exceptionOrNull()?.message)
    }

    @Test
    fun defaultWatchlist_cannotBeRenamed() = runTest {
        val result = repository.renameWatchlist(WatchlistStorage.DEFAULT_WATCHLIST_ID, "New Name")
        assertTrue(result.isFailure)
        assertEquals("Default Watchlist cannot be renamed", result.exceptionOrNull()?.message)
    }

    @Test
    fun createCustomList_success() = runTest {
        val result = repository.createWatchlist("Horror Night")
        assertTrue(result.isSuccess)

        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Horror Night", created?.name)
        assertFalse(created?.isDefault == true)

        val currentLists = repository.watchlists.value
        assertEquals(2, currentLists.size)
    }

    @Test
    fun createCustomList_rejectsBlankAndDuplicateNames() = runTest {
        val blankResult = repository.createWatchlist("   ")
        assertTrue(blankResult.isFailure)

        repository.createWatchlist("Sci-Fi")
        val duplicateResult = repository.createWatchlist("sci-fi")
        assertTrue(duplicateResult.isFailure)
        assertEquals("A list with this name already exists", duplicateResult.exceptionOrNull()?.message)
    }

    @Test
    fun addAndRemoveTitles_isolatesByList() = runTest {
        val createResult = repository.createWatchlist("Partner List")
        val partnerListId = createResult.getOrThrow().id

        // Add testMovie1 to partner list
        repository.addTitleToWatchlist(partnerListId, testMovie1)

        // Verify testMovie1 is in both My Watchlist and Partner List
        assertTrue(repository.isTitleInWatchlist(WatchlistStorage.DEFAULT_WATCHLIST_ID, "m1", MediaType.MOVIE))
        assertTrue(repository.isTitleInWatchlist(partnerListId, "m1", MediaType.MOVIE))

        // Remove testMovie1 from Partner List only
        repository.removeTitleFromWatchlist(partnerListId, "m1", MediaType.MOVIE)

        // Verify testMovie1 is removed from Partner List but still present in My Watchlist
        assertFalse(repository.isTitleInWatchlist(partnerListId, "m1", MediaType.MOVIE))
        assertTrue(repository.isTitleInWatchlist(WatchlistStorage.DEFAULT_WATCHLIST_ID, "m1", MediaType.MOVIE))
    }

    @Test
    fun deleteCustomList_doesNotAffectOtherLists() = runTest {
        val custom = repository.createWatchlist("Temporary List").getOrThrow()
        repository.addTitleToWatchlist(custom.id, testMovie2)

        val deleteResult = repository.deleteWatchlist(custom.id)
        assertTrue(deleteResult.isSuccess)

        // Default list still exists and retains its items
        assertTrue(repository.isTitleInWatchlist(WatchlistStorage.DEFAULT_WATCHLIST_ID, "m1", MediaType.MOVIE))
        assertEquals(1, repository.watchlists.value.size)
    }

    @Test
    fun randomSelectionAcrossAllLists_deduplicatesTitles() = runTest {
        val custom = repository.createWatchlist("Secondary List").getOrThrow()
        // Add testMovie1 to custom list as well
        repository.addTitleToWatchlist(custom.id, testMovie1)

        // Pick random title across all lists
        val picked = repository.getRandomTitleAcrossAllLists()
        assertNotNull(picked)
        assertEquals("m1", picked?.movieId)
    }

    @Test
    fun randomSelectionFromSpecificList() = runTest {
        val custom = repository.createWatchlist("Custom List").getOrThrow()
        repository.addTitleToWatchlist(custom.id, testMovie2)

        val pickedFromCustom = repository.getRandomTitleFromList(custom.id)
        assertEquals("m2", pickedFromCustom?.movieId)

        val pickedFromDefault = repository.getRandomTitleFromList(WatchlistStorage.DEFAULT_WATCHLIST_ID)
        assertEquals("m1", pickedFromDefault?.movieId)
    }

    @Test
    fun migration_freshInstallation_producesOneEmptyDefaultList() {
        val storage = WatchlistStorage(DummyContext(), FakeSharedPreferences())
        val lists = storage.getAllWatchlists()
        assertEquals(1, lists.size)
        assertEquals(WatchlistStorage.DEFAULT_WATCHLIST_ID, lists.first().id)
        assertTrue(lists.first().isDefault)
        assertTrue(lists.first().items.isEmpty())
    }

    @Test
    fun migration_legacySeededMovies_areRemoved() {
        val gson = com.google.gson.Gson()
        val seededMovie = Movie(
            movieId = "1022789",
            mediaType = MediaType.MOVIE,
            name = "Dune: Part Two",
            imageUrl = "https://image.tmdb.org/t/p/w500/1pdfLPoL3VFiCfybdB2AFi2B9A.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8A2B2C2vA4sA2148S6b0390.jpg",
            description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
            year = "2024",
            duration = "2h 46m",
            genres = "Sci-Fi, Adventure",
            rating = "8.5",
            cast = "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem",
            director = "Denis Villeneuve"
        )
        val legacyLists = listOf(
            Watchlist(
                id = WatchlistStorage.DEFAULT_WATCHLIST_ID,
                name = "My Watchlist",
                isDefault = true,
                items = listOf(seededMovie)
            )
        )
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("user_watchlists", gson.toJson(legacyLists)).apply()

        val storage = WatchlistStorage(DummyContext(), fakePrefs)
        val lists = storage.getAllWatchlists()

        assertEquals(1, lists.size)
        assertTrue(lists.first().items.isEmpty())
    }

    @Test
    fun migration_genuineMoviesInDefaultList_arePreserved() {
        val gson = com.google.gson.Gson()
        val seededMovie = Movie(
            movieId = "1022789",
            mediaType = MediaType.MOVIE,
            name = "Dune: Part Two",
            imageUrl = "https://image.tmdb.org/t/p/w500/1pdfLPoL3VFiCfybdB2AFi2B9A.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8A2B2C2vA4sA2148S6b0390.jpg",
            description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
            year = "2024",
            duration = "2h 46m",
            genres = "Sci-Fi, Adventure",
            rating = "8.5",
            cast = "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem",
            director = "Denis Villeneuve"
        )
        val genuineMovie = Movie(
            movieId = "user_movie_100",
            mediaType = MediaType.MOVIE,
            name = "User Added Movie",
            imageUrl = "https://image.tmdb.org/t/p/w500/user.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/user_back.jpg",
            description = "User custom description"
        )
        val legacyLists = listOf(
            Watchlist(
                id = WatchlistStorage.DEFAULT_WATCHLIST_ID,
                name = "My Watchlist",
                isDefault = true,
                items = listOf(seededMovie, genuineMovie)
            )
        )
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("user_watchlists", gson.toJson(legacyLists)).apply()

        val storage = WatchlistStorage(DummyContext(), fakePrefs)
        val lists = storage.getAllWatchlists()

        assertEquals(1, lists.first().items.size)
        assertEquals("user_movie_100", lists.first().items.first().movieId)
    }

    @Test
    fun migration_genuineMoviesInCustomLists_arePreserved() {
        val gson = com.google.gson.Gson()
        val genuineMovie = Movie(
            movieId = "user_movie_200",
            mediaType = MediaType.MOVIE,
            name = "Custom List Movie",
            imageUrl = "https://image.tmdb.org/t/p/w500/custom.jpg",
            backdropUrl = ""
        )
        val customList = Watchlist(
            id = "user_custom_list",
            name = "My Action Favorites",
            isDefault = false,
            items = listOf(genuineMovie)
        )
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("user_watchlists", gson.toJson(listOf(customList))).apply()

        val storage = WatchlistStorage(DummyContext(), fakePrefs)
        val lists = storage.getAllWatchlists()

        val foundCustom = lists.find { it.id == "user_custom_list" }
        assertNotNull(foundCustom)
        assertEquals(1, foundCustom?.items?.size)
        assertEquals("user_movie_200", foundCustom?.items?.first()?.movieId)
    }

    @Test
    fun migration_genuineMovie_withFabricatedUrlSubstring_isNotDeleted() {
        val gson = com.google.gson.Gson()
        val genuineMovieWithUrlSubstring = Movie(
            movieId = "user_movie_300",
            mediaType = MediaType.MOVIE,
            name = "User Movie With Specific Host",
            imageUrl = "https://mycdn.com/123049120.jpg",
            backdropUrl = "https://mycdn.com/back.jpg",
            description = "Genuine user overview text"
        )
        val defaultList = Watchlist(
            id = WatchlistStorage.DEFAULT_WATCHLIST_ID,
            name = "My Watchlist",
            isDefault = true,
            items = listOf(genuineMovieWithUrlSubstring)
        )
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("user_watchlists", gson.toJson(listOf(defaultList))).apply()

        val storage = WatchlistStorage(DummyContext(), fakePrefs)
        val lists = storage.getAllWatchlists()

        assertEquals(1, lists.first().items.size)
        assertEquals("user_movie_300", lists.first().items.first().movieId)
    }

    @Test
    fun migration_legacySampleLists_areRemovedOnlyWhenEmptyAfterMigration() {
        val gson = com.google.gson.Gson()
        val seededPartnerMovie = Movie(
            movieId = "2164",
            mediaType = MediaType.MOVIE,
            name = "The Notebook",
            imageUrl = "https://image.tmdb.org/t/p/w500/r123049120.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/r9123049120.jpg",
            description = "An elderly man reads to a woman with Alzheimer's the story of two young lovers separated by social differences.",
            year = "2004",
            duration = "2h 03m",
            genres = "Romance, Drama",
            rating = "7.8",
            cast = "Ryan Gosling, Rachel McAdams",
            director = "Nick Cassavetes"
        )
        val seededHorrorMovie = Movie(
            movieId = "348",
            mediaType = MediaType.MOVIE,
            name = "Alien",
            imageUrl = "https://image.tmdb.org/t/p/w500/z123049120.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/z9123049120.jpg",
            description = "The crew of a commercial spacecraft encounter a deadly lifeform after investigating a mysterious transmission.",
            year = "1979",
            duration = "1h 57m",
            genres = "Horror, Sci-Fi",
            rating = "8.1",
            cast = "Sigourney Weaver, Tom Skerritt, John Hurt",
            director = "Ridley Scott"
        )
        val genuineMovieInPartnerList = Movie(
            movieId = "user_movie_400",
            mediaType = MediaType.MOVIE,
            name = "User Romantic Comedy",
            imageUrl = "https://tmdb.org/user.jpg",
            backdropUrl = ""
        )
        val partnerListWithUserMovie = Watchlist(
            id = "partner_list",
            name = "Me & My Partner",
            isDefault = false,
            items = listOf(seededPartnerMovie, genuineMovieInPartnerList)
        )
        val horrorListOnlySeeded = Watchlist(
            id = "horror_night_list",
            name = "Horror Night",
            isDefault = false,
            items = listOf(seededHorrorMovie)
        )
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("user_watchlists", gson.toJson(listOf(partnerListWithUserMovie, horrorListOnlySeeded))).apply()

        val storage = WatchlistStorage(DummyContext(), fakePrefs)
        val lists = storage.getAllWatchlists()

        val partnerResult = lists.find { it.id == "partner_list" }
        val horrorResult = lists.find { it.id == "horror_night_list" }

        assertNotNull(partnerResult)
        assertEquals(1, partnerResult?.items?.size)
        assertEquals("user_movie_400", partnerResult?.items?.first()?.movieId)
        assertTrue(horrorResult == null)
    }

    @Test
    fun migration_genuineMovie_withSameIdAndTitleAsSeed_butModifiedMetadata_isPreserved() {
        val gson = com.google.gson.Gson()
        val genuineUserDune = Movie(
            movieId = "1022789",
            mediaType = MediaType.MOVIE,
            name = "Dune: Part Two",
            imageUrl = "https://image.tmdb.org/t/p/w500/1pdfLPoL3VFiCfybdB2AFi2B9A.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8A2B2C2vA4sA2148S6b0390.jpg",
            description = "User custom review: Masterpiece sci-fi epic by Denis Villeneuve.",
            year = "2024",
            duration = "2h 46m",
            genres = "Sci-Fi",
            rating = "9.0",
            cast = "Timothée Chalamet, Zendaya",
            director = "Denis Villeneuve"
        )
        val defaultList = Watchlist(
            id = WatchlistStorage.DEFAULT_WATCHLIST_ID,
            name = "My Watchlist",
            isDefault = true,
            items = listOf(genuineUserDune)
        )
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("user_watchlists", gson.toJson(listOf(defaultList))).apply()

        val storage = WatchlistStorage(DummyContext(), fakePrefs)
        val lists = storage.getAllWatchlists()

        assertEquals(1, lists.first().items.size)
        assertEquals("1022789", lists.first().items.first().movieId)
        assertEquals("User custom review: Masterpiece sci-fi epic by Denis Villeneuve.", lists.first().items.first().description)
    }
}

private class FakeSharedPreferences : android.content.SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = (map[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = EditorImpl()

    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class EditorImpl : android.content.SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): android.content.SharedPreferences.Editor {
            if (key != null) map[key] = value ?: ""
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): android.content.SharedPreferences.Editor {
            if (key != null) map[key] = values ?: mutableSetOf<String>()
            return this
        }
        override fun putInt(key: String?, value: Int): android.content.SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): android.content.SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): android.content.SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): android.content.SharedPreferences.Editor {
            if (key != null) map[key] = value
            return this
        }
        override fun remove(key: String?): android.content.SharedPreferences.Editor {
            if (key != null) map.remove(key)
            return this
        }
        override fun clear(): android.content.SharedPreferences.Editor {
            map.clear()
            return this
        }
        override fun commit(): Boolean = true
        override fun apply() {}
    }
}

private class DummyContext : android.content.ContextWrapper(null)
