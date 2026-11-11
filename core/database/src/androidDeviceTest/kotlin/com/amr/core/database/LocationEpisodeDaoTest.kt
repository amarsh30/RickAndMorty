package com.amr.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.EpisodeCharacterEntity
import com.amr.core.database.entity.EpisodeEntity
import com.amr.core.database.entity.LocationEntity
import com.amr.core.database.entity.LocationResidentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Location and episode DAOs against a real in-memory Room database (foreign keys on). */
class LocationEpisodeDaoTest {

    private lateinit var database: CharacterDatabase

    private fun location(id: Int, name: String = "Location $id", inList: Boolean = true) =
        LocationEntity(id, name, "Planet", "unknown", inList)

    private fun residents(locationId: Int, vararg characterIds: Int) =
        characterIds.mapIndexed { index, characterId -> LocationResidentEntity(locationId, characterId, index) }

    private fun episode(id: Int, name: String = "Episode $id", inList: Boolean = true) =
        EpisodeEntity(id, name, "December 2, 2013", "S01E0$id", inList)

    private fun links(episodeId: Int, vararg characterIds: Int) =
        characterIds.mapIndexed { index, characterId -> EpisodeCharacterEntity(episodeId, characterId, index) }

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

    // --- locations ---

    @Test
    fun replaceFirstPage_keepsDetailRowsAndTheirResidents_outsideTheList() = runTest {
        val dao = database.locationDao()
        dao.saveDetail(location(50, inList = false), residents(50, 8, 9))
        dao.replaceFirstPage(listOf(location(1), location(2)), residents(1, 3), nextPage = 2, totalPages = 7)

        dao.replaceFirstPage(listOf(location(1, name = "Earth")), residents(1, 4, 5), nextPage = 2, totalPages = 7)

        // Only the new page is listed; nothing was deleted.
        assertEquals(listOf(location(1, name = "Earth")), dao.observeListLocations().first())
        assertEquals(location(2, inList = false), dao.observeLocation(2).first())
        assertEquals(location(50, inList = false), dao.observeLocation(50).first())
        assertEquals(listOf(8, 9), dao.observeResidentIds(50).first())
        assertEquals(listOf(4, 5), dao.observeResidentIds(1).first())
    }

    @Test
    fun appendPage_addsToListAndAdvancesMetadata() = runTest {
        val dao = database.locationDao()
        dao.replaceFirstPage(listOf(location(1)), emptyList(), nextPage = 2, totalPages = 2)

        dao.appendPage(listOf(location(21)), residents(21, 1), nextPage = null, totalPages = 2)

        assertEquals(listOf(1, 21), dao.observeListLocations().first().map { it.id })
        assertNull(dao.getPagingState()?.nextPage)
        assertEquals(2, dao.getPagingState()?.totalPages)
    }

    @Test
    fun pageRefresh_ofExistingRow_keepsResidentsWithoutCascadeLoss() = runTest {
        val dao = database.locationDao()
        dao.replaceFirstPage(listOf(location(1)), residents(1, 3, 4), nextPage = 2, totalPages = 7)

        // Same location again with the same residents: upsert must not cascade-delete them.
        dao.appendPage(listOf(location(1, name = "Renamed")), residents(1, 3, 4), nextPage = 3, totalPages = 7)

        assertEquals(listOf(3, 4), dao.observeResidentIds(1).first())
        assertEquals("Renamed", dao.observeLocation(1).first()?.name)
    }

    @Test
    fun saveDetail_ofListedRow_updatesFieldsAndKeepsListMembership() = runTest {
        val dao = database.locationDao()
        dao.replaceFirstPage(listOf(location(1)), residents(1, 3), nextPage = 2, totalPages = 7)

        dao.saveDetail(location(1, name = "Earth (C-137)", inList = false), residents(1, 3, 4))

        assertEquals(listOf(location(1, name = "Earth (C-137)")), dao.observeListLocations().first())
        assertEquals(listOf(3, 4), dao.observeResidentIds(1).first())
    }

    @Test
    fun resident_withoutLocation_isRejected() = runTest {
        assertFailsWith<Exception> {
            database.locationDao().insertResidents(residents(404, 1))
        }
    }

    @Test
    fun failedPageWrite_rollsBackListAndMetadata() = runTest {
        val dao = database.locationDao()
        dao.replaceFirstPage(listOf(location(1)), residents(1, 3), nextPage = 2, totalPages = 7)

        // Residents pointing at a location outside the page violate the foreign key.
        assertFailsWith<Exception> {
            dao.replaceFirstPage(listOf(location(2)), residents(404, 1), nextPage = 3, totalPages = 7)
        }

        assertEquals(listOf(location(1)), dao.observeListLocations().first())
        assertEquals(listOf(3), dao.observeResidentIds(1).first())
        assertEquals(2, dao.getPagingState()?.nextPage)
    }

    // --- episodes ---

    @Test
    fun episodes_followTheSameListAndRelationRules() = runTest {
        val dao = database.episodeDao()
        dao.saveDetail(episode(9, inList = false), links(9, 1))
        dao.replaceFirstPage(listOf(episode(1), episode(2)), links(1, 1, 2), nextPage = 2, totalPages = 3)
        dao.appendPage(listOf(episode(21)), links(21, 5), nextPage = 3, totalPages = 3)

        dao.replaceFirstPage(listOf(episode(1)), links(1, 2, 1), nextPage = 2, totalPages = 3)

        assertEquals(listOf(episode(1)), dao.observeListEpisodes().first())
        assertEquals(episode(21, inList = false), dao.observeEpisode(21).first())
        assertEquals(listOf(5), dao.observeCharacterIds(21).first())
        assertEquals(listOf(1), dao.observeCharacterIds(9).first())
        // API order is kept, not id order.
        assertEquals(listOf(2, 1), dao.observeCharacterIds(1).first())
        assertEquals(2, dao.getPagingState()?.nextPage)
    }

    @Test
    fun episodeLink_withoutEpisode_isRejected() = runTest {
        assertFailsWith<Exception> {
            database.episodeDao().insertCharacterLinks(links(404, 1))
        }
    }

    // --- isolation from the character tables ---

    @Test
    fun locationAndEpisodeWrites_leaveCharacterTablesUntouched() = runTest {
        val rick = CharacterEntity(1, "Rick Sanchez", "Alive", "Human", null)
        database.characterDao().replaceFirstPage(listOf(rick), nextPage = 2, totalPages = 42)

        database.locationDao().replaceFirstPage(listOf(location(1)), residents(1, 1), nextPage = 2, totalPages = 7)
        database.episodeDao().replaceFirstPage(listOf(episode(1)), links(1, 1), nextPage = 2, totalPages = 3)

        assertEquals(listOf(rick), database.characterDao().observeCharacters().first())
        assertEquals(2, database.characterDao().getPagingState()?.nextPage)
        assertEquals(42, database.characterDao().getPagingState()?.totalPages)
        assertEquals(0, database.searchCharacterDao().countCharacters())
    }

    @Test
    fun charactersByIds_readsAllCachesWithoutDuplicates() = runTest {
        val rick = CharacterEntity(1, "Rick Sanchez", "Alive", "Human", null)
        database.characterDao().insertCharacters(listOf(rick))
        database.searchCharacterDao().upsertCharacters(
            listOf(
                com.amr.core.database.entity.SearchCharacterEntity(
                    1, "Rick (old)", "Alive", "Human", null, null, null, null, null, null, null, null
                ),
                com.amr.core.database.entity.SearchCharacterEntity(
                    7, "Birdperson", "Alive", "Alien", null, null, null, null, null, null, null, null
                )
            )
        )

        val found = database.searchCharacterDao().observeCharactersByIds(listOf(1, 7, 99)).first()

        assertEquals(setOf(1, 7), found.map { it.id }.toSet())
        assertEquals("Rick Sanchez", found.single { it.id == 1 }.name)
        assertEquals(setOf(1, 7), database.searchCharacterDao().getCachedCharacterIds(listOf(1, 7, 99)).toSet())
    }
}
