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
}

private class DummyContext : android.content.ContextWrapper(null)
