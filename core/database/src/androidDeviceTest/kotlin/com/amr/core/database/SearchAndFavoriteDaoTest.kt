package com.amr.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Search and favorite DAOs against a real in-memory Room database. */
class SearchAndFavoriteDaoTest {

    private lateinit var database: CharacterDatabase

    private val rick = CharacterEntity(
        1, "Rick Sanchez", "Alive", "Human", "rick.png",
        type = "", gender = "Male", originName = "Earth (C-137)", originId = 1,
        locationName = "Citadel of Ricks", locationId = 3, episodeIds = "1,2"
    )
    private val morty = CharacterEntity(2, "Morty Smith", "Alive", "Human", "morty.png", gender = "Male")
    private val summer = CharacterEntity(3, "Summer Smith", "Alive", "Human", "summer.png", gender = "Female")

    private fun CharacterEntity.asSearch(updatedAt: Long = 0) = SearchCharacterEntity(
        id, name, status, species, imageUrl, type, gender,
        originName, originId, locationName, locationId, episodeIds, updatedAt
    )

    private fun searchRow(id: Int, updatedAt: Long) =
        CharacterEntity(id, "Character $id", "Alive", "Human", null).asSearch(updatedAt)

    private fun CharacterEntity.asFavorite() = FavoriteCharacterEntity(
        id, name, status, species, imageUrl, type, gender,
        originName, originId, locationName, locationId, episodeIds
    )

    @BeforeTest
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<CharacterDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun search(
        name: String? = null,
        status: String? = null,
        species: String? = null,
        gender: String? = null
    ) = database.searchCharacterDao().searchCharacters(name, status, species, gender).first()

    @Test
    fun search_matchesBothCachesWithoutDuplicates_listCacheWins() = runTest {
        database.characterDao().insertCharacters(listOf(rick, morty))
        // Same id as morty with an older name, plus a character only known from search.
        database.searchCharacterDao().upsertCharacters(
            listOf(morty.copy(name = "Morty Old").asSearch(), summer.asSearch())
        )

        val results = search(name = "smith")

        assertEquals(listOf(morty, summer), results)
    }

    @Test
    fun search_filtersByStatusAndGenderIgnoringCase() = runTest {
        database.characterDao().insertCharacters(listOf(rick, morty, summer))

        assertEquals(listOf(summer), search(gender = "female"))
        assertEquals(listOf(rick, morty, summer), search(status = "alive", species = "hum"))
        assertEquals(emptyList(), search(status = "Dead"))
    }

    @Test
    fun searchCache_doesNotEnterMainList() = runTest {
        database.characterDao().insertCharacters(listOf(rick))
        database.searchCharacterDao().upsertCharacters(listOf(summer.asSearch()))

        assertEquals(listOf(rick), database.characterDao().observeCharacters().first())
    }

    @Test
    fun updateCharacter_onlyUpdatesExistingListRows() = runTest {
        database.characterDao().insertCharacters(listOf(rick))

        assertEquals(1, database.characterDao().updateCharacter(rick.copy(status = "Dead")))
        assertEquals(0, database.characterDao().updateCharacter(summer))
        assertEquals(listOf(rick.copy(status = "Dead")), database.characterDao().observeCharacters().first())
    }

    // --- bounded search cache (v5) ---

    @Test
    fun upsertAndEvict_above1000_removesOldestUpdatedAt() = runTest {
        val dao = database.searchCharacterDao()
        // updatedAt == id, so id 1 is the oldest.
        dao.upsertAndEvict((1..1_001).map { searchRow(it, updatedAt = it.toLong()) }, maxEntries = 1_000)

        assertEquals(1_000, dao.countCharacters())
        assertNull(dao.observeCharacter(1).first())
        assertEquals(2L, dao.observeCharacter(2).first()?.updatedAt)
        assertEquals(1_001L, dao.observeCharacter(1_001).first()?.updatedAt)
    }

    @Test
    fun upsertAndEvict_atLimit_removesNothing() = runTest {
        val dao = database.searchCharacterDao()
        dao.upsertAndEvict((1..1_000).map { searchRow(it, updatedAt = 5) }, maxEntries = 1_000)

        assertEquals(1_000, dao.countCharacters())
    }

    @Test
    fun evictOldest_tieOnUpdatedAt_evictsLowerIdFirst() = runTest {
        val dao = database.searchCharacterDao()
        dao.upsertCharacters(listOf(searchRow(30, 7), searchRow(10, 7), searchRow(20, 7), searchRow(5, 9)))

        dao.evictOldest(maxEntries = 2)

        // Newest (id 5, t=9) stays; among t=7 the highest id stays.
        assertNotNull(dao.observeCharacter(5).first())
        assertNotNull(dao.observeCharacter(30).first())
        assertNull(dao.observeCharacter(10).first())
        assertNull(dao.observeCharacter(20).first())
    }

    @Test
    fun refetch_movesUpdatedAt_soRowIsKeptOverOlderOnes() = runTest {
        val dao = database.searchCharacterDao()
        dao.upsertCharacters(listOf(searchRow(1, 1), searchRow(2, 2)))

        // Character 1 is fetched again later: it becomes the newest row.
        dao.upsertAndEvict(listOf(searchRow(1, 3)), maxEntries = 1)

        assertEquals(3L, dao.observeCharacter(1).first()?.updatedAt)
        assertNull(dao.observeCharacter(2).first())
    }

    @Test
    fun eviction_leavesListFavoritesAndPagingUntouched() = runTest {
        database.characterDao().replaceFirstPage(listOf(rick, morty), nextPage = 2, totalPages = 42)
        database.favoriteCharacterDao().upsertFavorite(summer.asFavorite())
        // Same ids as list and favorite rows, all older than the new batch.
        database.searchCharacterDao().upsertCharacters(
            listOf(rick.asSearch(1), morty.asSearch(1), summer.asSearch(1))
        )

        database.searchCharacterDao().upsertAndEvict(listOf(searchRow(99, 100)), maxEntries = 1)

        assertEquals(1, database.searchCharacterDao().countCharacters())
        assertEquals(listOf(rick, morty), database.characterDao().observeCharacters().first())
        assertEquals(2, database.characterDao().getPagingState()?.nextPage)
        assertEquals(42, database.characterDao().getPagingState()?.totalPages)
        assertEquals(listOf(summer.asFavorite()), database.favoriteCharacterDao().observeFavorites().first())
    }

    @Test
    fun staleSearchRow_isStillReturnedBySearch() = runTest {
        // updatedAt 0 = cached before v5, far older than the 7-day TTL.
        database.searchCharacterDao().upsertCharacters(listOf(summer.asSearch(updatedAt = 0)))

        assertEquals(listOf(summer), search(name = "summer"))
    }

    @Test
    fun favorite_survivesListReplace() = runTest {
        val dao = database.favoriteCharacterDao()
        database.characterDao().insertCharacters(listOf(rick, morty))
        dao.upsertFavorite(rick.asFavorite())

        database.characterDao().replaceFirstPage(listOf(summer), nextPage = 2, totalPages = 42)

        assertEquals(listOf(rick.asFavorite()), dao.observeFavorites().first())
        assertTrue(dao.observeIsFavorite(1).first())
    }

    @Test
    fun favorite_toggleAndUpdateIfExists() = runTest {
        val dao = database.favoriteCharacterDao()
        assertFalse(dao.observeIsFavorite(1).first())

        dao.upsertFavorite(rick.asFavorite())
        with(rick.copy(status = "Dead")) {
            dao.updateFavoriteIfExists(
                id, name, status, species, imageUrl, type, gender,
                originName, originId, locationName, locationId, episodeIds
            )
        }
        // Not a favorite: the update must not insert a row.
        with(morty) {
            dao.updateFavoriteIfExists(
                id, name, status, species, imageUrl, type, gender,
                originName, originId, locationName, locationId, episodeIds
            )
        }

        assertEquals("Dead", dao.observeFavorite(1).first()?.status)
        assertNull(dao.observeFavorite(2).first())

        dao.deleteFavorite(1)
        assertFalse(dao.observeIsFavorite(1).first())
    }
}
