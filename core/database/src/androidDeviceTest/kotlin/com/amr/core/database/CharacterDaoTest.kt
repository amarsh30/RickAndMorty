package com.amr.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.amr.core.database.dao.CharacterDao
import com.amr.core.database.entity.CharacterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests [CharacterDao] against a real in-memory Room database with the bundled
 * SQLite driver used in production, so upsert, ordering and transactions are
 * SQLite behaviour, not a fake.
 */
class CharacterDaoTest {

    private lateinit var database: CharacterDatabase
    private lateinit var dao: CharacterDao

    private val rick = CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png")
    private val morty = CharacterEntity(2, "Morty Smith", "Alive", "Human", "morty.png")
    private val summer = CharacterEntity(3, "Summer Smith", "Alive", "Human", "summer.png")
    private val beth = CharacterEntity(4, "Beth Smith", "Alive", "Human", "beth.png")

    @BeforeTest
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder<CharacterDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addCallback(FailOnNextPageTrigger)
            .build()
        dao = database.characterDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagingState_whenNothingStored_isNull() = runTest {
        assertNull(dao.getPagingState())
        assertNull(dao.observePagingState().first())
    }

    @Test
    fun pagingState_withNullNextPage_isAnExistingRow() = runTest {
        dao.replaceFirstPage(listOf(rick), nextPage = null, totalPages = 1)

        val state = assertNotNull(dao.getPagingState())
        assertNull(state.nextPage)
        assertEquals(1, state.totalPages)
        assertEquals(state, dao.observePagingState().first())
    }

    @Test
    fun replaceFirstPage_replacesAllCharactersAndMetadata() = runTest {
        dao.replaceFirstPage(listOf(rick, morty), nextPage = 2, totalPages = 42)
        dao.appendPage(listOf(summer), nextPage = 3, totalPages = 42)

        dao.replaceFirstPage(listOf(beth), nextPage = 2, totalPages = 43)

        assertEquals(listOf(beth), dao.observeCharacters().first())
        val state = assertNotNull(dao.getPagingState())
        assertEquals(2, state.nextPage)
        assertEquals(43, state.totalPages)
    }

    @Test
    fun appendPage_keepsExistingCharactersAndAdvancesMetadata() = runTest {
        dao.replaceFirstPage(listOf(rick, morty), nextPage = 2, totalPages = 42)

        dao.appendPage(listOf(summer, beth), nextPage = 3, totalPages = 42)

        assertEquals(listOf(rick, morty, summer, beth), dao.observeCharacters().first())
        assertEquals(3, dao.getPagingState()?.nextPage)
    }

    @Test
    fun appendPage_withExistingId_updatesRowWithoutDuplicate() = runTest {
        dao.replaceFirstPage(listOf(rick, morty), nextPage = 2, totalPages = 42)
        val updatedMorty = morty.copy(status = "Dead")

        dao.appendPage(listOf(updatedMorty, summer), nextPage = 3, totalPages = 42)

        assertEquals(listOf(rick, updatedMorty, summer), dao.observeCharacters().first())
    }

    @Test
    fun observeCharacters_isOrderedByIdRegardlessOfInsertOrder() = runTest {
        dao.replaceFirstPage(listOf(beth, rick), nextPage = 2, totalPages = 42)

        dao.appendPage(listOf(summer, morty), nextPage = 3, totalPages = 42)

        assertEquals(listOf(rick, morty, summer, beth), dao.observeCharacters().first())
    }

    @Test
    fun replaceFirstPage_whenMetadataWriteFails_rollsBackDeleteAndInsert() = runTest {
        dao.replaceFirstPage(listOf(rick, morty), nextPage = 2, totalPages = 42)

        // Characters are deleted and inserted first; the metadata write then fails.
        assertFails {
            dao.replaceFirstPage(listOf(beth), nextPage = FAILING_NEXT_PAGE, totalPages = 42)
        }

        assertEquals(listOf(rick, morty), dao.observeCharacters().first())
        assertEquals(2, dao.getPagingState()?.nextPage)
    }

    @Test
    fun appendPage_whenMetadataWriteFails_rollsBackInsert() = runTest {
        dao.replaceFirstPage(listOf(rick, morty), nextPage = 2, totalPages = 42)

        assertFails {
            dao.appendPage(listOf(summer), nextPage = FAILING_NEXT_PAGE, totalPages = 42)
        }

        assertEquals(listOf(rick, morty), dao.observeCharacters().first())
        assertEquals(2, dao.getPagingState()?.nextPage)
    }

    private companion object {
        const val FAILING_NEXT_PAGE = 999
    }

    /**
     * Makes any write of nextPage = [FAILING_NEXT_PAGE] to the metadata table fail,
     * which is the last statement of both transactions.
     */
    private object FailOnNextPageTrigger : RoomDatabase.Callback() {
        override fun onOpen(connection: SQLiteConnection) {
            listOf("INSERT", "UPDATE").forEach { operation ->
                connection.execSQL(
                    """
                    CREATE TEMP TRIGGER IF NOT EXISTS fail_paging_state_$operation
                    BEFORE $operation ON character_paging_state
                    WHEN NEW.nextPage = $FAILING_NEXT_PAGE
                    BEGIN SELECT RAISE(ABORT, 'forced failure'); END
                    """.trimIndent()
                )
            }
        }
    }
}
