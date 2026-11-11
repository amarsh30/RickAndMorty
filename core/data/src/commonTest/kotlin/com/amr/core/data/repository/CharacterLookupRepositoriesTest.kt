package com.amr.core.data.repository

import com.amr.core.data.fake.FakeCharacterDao
import com.amr.core.data.fake.FakeCharacterRemoteDataSource
import com.amr.core.data.fake.FakeFavoriteCharacterDao
import com.amr.core.data.fake.FakeSearchCharacterDao
import com.amr.core.data.fake.testCharacter
import com.amr.core.data.fake.testCharacterDto
import com.amr.core.data.fake.testSingleFlight
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSourceImpl
import com.amr.core.data.mapper.toEntity
import com.amr.core.data.mapper.toFavoriteEntity
import com.amr.core.data.mapper.toSearchEntity
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.data.remote.dto.PageInfoDto
import com.amr.core.database.entity.CharacterPagingStateEntity
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.CharacterFilter
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CharacterLookupRepositoriesTest {

    private lateinit var remote: FakeCharacterRemoteDataSource
    private lateinit var characterDao: FakeCharacterDao
    private lateinit var searchDao: FakeSearchCharacterDao
    private lateinit var favoriteDao: FakeFavoriteCharacterDao
    private lateinit var local: CharacterLookupLocalDataSourceImpl
    private lateinit var search: CharacterSearchRepositoryImpl
    private lateinit var detail: CharacterDetailRepositoryImpl
    private lateinit var favorites: FavoriteRepositoryImpl

    /** Fake clock read by the local data source when it saves fetched characters. */
    private var now = NOW

    @BeforeTest
    fun setUp() {
        remote = FakeCharacterRemoteDataSource()
        characterDao = FakeCharacterDao()
        searchDao = FakeSearchCharacterDao()
        favoriteDao = FakeFavoriteCharacterDao()
        local = CharacterLookupLocalDataSourceImpl(characterDao, searchDao, favoriteDao, nowMillis = { now })
        search = CharacterSearchRepositoryImpl(remote, local)
        detail = CharacterDetailRepositoryImpl(remote, local, testSingleFlight())
        favorites = FavoriteRepositoryImpl(local)
    }

    private fun searchResponse(vararg ids: Int) = CharacterResponseDto(
        info = PageInfoDto(count = ids.size, pages = 1, next = null, prev = null),
        results = ids.map { testCharacterDto(it) }
    )

    // --- search ---

    @Test
    fun searchRemote_sendsApiFilterValues() = runTest {
        remote.searchResponse = searchResponse()

        search.searchRemote(
            CharacterFilter(
                name = "rick",
                status = CharacterFilter.Status.ALIVE,
                gender = CharacterFilter.Gender.GENDERLESS
            )
        )

        assertEquals(
            listOf(FakeCharacterRemoteDataSource.SearchQuery("rick", "alive", null, "genderless")),
            remote.searchQueries
        )
    }

    @Test
    fun searchRemote_withEmptyFilter_makesNoRequestAndObservesNothing() = runTest {
        search.searchRemote(CharacterFilter(name = ""))

        assertTrue(remote.searchQueries.isEmpty())
        assertTrue(search.observeSearchResults(CharacterFilter()).first().isEmpty())
    }

    @Test
    fun searchRemote_storesResultsOutsideMainListAndKeepsPaging() = runTest {
        // Character 1 is in the main list with stale data; 5 is only a search hit.
        characterDao.characters.value = listOf(testCharacter(1, status = "Dead").toEntity())
        val paging = CharacterPagingStateEntity(nextPage = 2, totalPages = 42)
        characterDao.pagingState.value = paging
        remote.searchResponse = searchResponse(1, 5)

        search.searchRemote(CharacterFilter(name = "character"))

        // The list row is refreshed in place; membership and paging are unchanged.
        assertEquals(listOf(testCharacter(1).toEntity()), characterDao.characters.value)
        assertEquals(paging, characterDao.pagingState.value)
        assertEquals(listOf(testCharacter(5).toSearchEntity(updatedAt = NOW)), searchDao.rows.value)
    }

    // --- search cache freshness and bounds ---

    @Test
    fun searchRemote_success_setsUpdatedAt_andRefetchMovesIt() = runTest {
        remote.searchResponse = searchResponse(5)
        search.searchRemote(CharacterFilter(name = "character"))
        assertEquals(NOW, searchDao.rows.value.single().updatedAt)

        now = NOW + ONE_DAY
        search.searchRemote(CharacterFilter(name = "character"))

        assertEquals(NOW + ONE_DAY, searchDao.rows.value.single().updatedAt)
    }

    @Test
    fun reading_doesNotChangeUpdatedAt() = runTest {
        searchDao.upsertCharacters(listOf(testCharacter(5).toSearchEntity(updatedAt = 1L)))
        now = NOW + ONE_DAY

        detail.observeCharacter(5).first()

        assertEquals(1L, searchDao.rows.value.single().updatedAt)
    }

    @Test
    fun searchRemote_savesThroughBoundedTransaction() = runTest {
        remote.searchResponse = searchResponse(5)

        search.searchRemote(CharacterFilter(name = "character"))

        assertEquals(CharacterLookupLocalDataSourceImpl.SEARCH_CACHE_MAX_ENTRIES, searchDao.lastMaxEntries)
        assertEquals(1_000, searchDao.lastMaxEntries)
    }

    @Test
    fun searchRemote_withOnlyListHits_doesNotTouchSearchCache() = runTest {
        characterDao.characters.value = listOf(testCharacter(1).toEntity())
        remote.searchResponse = searchResponse(1)

        search.searchRemote(CharacterFilter(name = "character"))

        assertNull(searchDao.lastMaxEntries)
        assertTrue(searchDao.rows.value.isEmpty())
    }

    @Test
    fun refreshCharacter_outsideList_setsUpdatedAt() = runTest {
        remote.characters[7] = testCharacterDto(7)

        detail.refreshCharacter(7)

        assertEquals(NOW, searchDao.rows.value.single().updatedAt)
    }

    @Test
    fun searchRemote_whenNothingMatches_isNotAnError() = runTest {
        remote.searchError = NetworkException.NotFound()

        search.searchRemote(CharacterFilter(name = "zzz"))

        assertTrue(searchDao.rows.value.isEmpty())
    }

    @Test
    fun searchRemote_whenOffline_throwsNoConnectionAndKeepsStaleCache() = runTest {
        // Older than the 7-day TTL: stale, but still the best data available offline.
        val stale = testCharacter(5).toSearchEntity(updatedAt = NOW - 8 * ONE_DAY)
        searchDao.upsertCharacters(listOf(stale))
        remote.searchError = NetworkException.NoInternet()

        assertFailsWith<DataError.NoConnection> {
            search.searchRemote(CharacterFilter(name = "rick"))
        }
        assertEquals(listOf(stale), searchDao.rows.value)
        assertEquals(testCharacter(5), detail.observeCharacter(5).first())
    }

    @Test
    fun searchRemote_propagatesCancellation() = runTest {
        remote.searchError = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            search.searchRemote(CharacterFilter(name = "rick"))
        }
    }

    // --- detail ---

    @Test
    fun observeCharacter_prefersListThenSearchThenFavorite() = runTest {
        favoriteDao.upsertFavorite(testCharacter(1, name = "Favorite").toFavoriteEntity())
        assertEquals("Favorite", detail.observeCharacter(1).first()?.name)

        searchDao.upsertCharacters(listOf(testCharacter(1, name = "Search").toSearchEntity(updatedAt = NOW)))
        assertEquals("Search", detail.observeCharacter(1).first()?.name)

        characterDao.characters.value = listOf(testCharacter(1, name = "List").toEntity())
        assertEquals("List", detail.observeCharacter(1).first()?.name)

        assertNull(detail.observeCharacter(99).first())
    }

    @Test
    fun refreshCharacter_updatesListRowAndFavoriteSnapshot() = runTest {
        characterDao.characters.value = listOf(testCharacter(1, status = "Alive").toEntity())
        favoriteDao.upsertFavorite(testCharacter(1, status = "Alive").toFavoriteEntity())
        remote.characters[1] = testCharacterDto(1, status = "Dead")

        detail.refreshCharacter(1)

        assertEquals("Dead", characterDao.characters.value.single().status)
        assertEquals("Dead", favoriteDao.rows.value.single().status)
        assertTrue(searchDao.rows.value.isEmpty())
    }

    @Test
    fun refreshCharacter_notInList_goesToSearchCache() = runTest {
        remote.characters[7] = testCharacterDto(7)

        detail.refreshCharacter(7)

        assertTrue(characterDao.characters.value.isEmpty())
        assertEquals(testCharacter(7), detail.observeCharacter(7).first())
    }

    @Test
    fun refreshCharacter_errors() = runTest {
        assertFailsWith<DataError.NotFound> { detail.refreshCharacter(404) }

        remote.characters[2] = testCharacterDto(3)
        assertFailsWith<DataError.InvalidData> { detail.refreshCharacter(2) }

        remote.characterError = NetworkException.NoInternet()
        assertFailsWith<DataError.NoConnection> { detail.refreshCharacter(1) }
    }

    // --- favorites ---

    @Test
    fun favorites_addRemoveAndSurviveListReplace() = runTest {
        characterDao.characters.value = listOf(testCharacter(1).toEntity())
        favorites.addFavorite(testCharacter(1))
        assertTrue(favorites.observeIsFavorite(1).first())

        characterDao.replaceFirstPage(emptyList(), nextPage = 2, totalPages = 42)
        assertEquals(listOf(testCharacter(1)), favorites.observeFavorites().first())

        favorites.removeFavorite(1)
        assertFalse(favorites.observeIsFavorite(1).first())
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val ONE_DAY = 24 * 60 * 60 * 1_000L
    }
}
