package com.amr.core.data.repository

import com.amr.core.data.fake.FakeCharacterLocalDataSource
import com.amr.core.data.fake.FakeCharacterRemoteDataSource
import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.data.remote.dto.PageInfoDto
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit test untuk [CharacterRepositoryImpl].
 *
 * Repository hanya bergantung pada abstraksi (CharacterRemoteDataSource &
 * CharacterLocalDataSource) — prinsip Dependency Inversion — sehingga kita
 * cukup menyuntikkan fake tanpa Ktor, Room, maupun Koin. Fake local meniru
 * semantik DAO yang dibuktikan oleh CharacterDaoTest di :core:database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterRepositoryImplTest {

    private lateinit var remoteDataSource: FakeCharacterRemoteDataSource
    private lateinit var localDataSource: FakeCharacterLocalDataSource
    private lateinit var repository: CharacterRepositoryImpl

    private val cachedCharacters = listOf(
        Character(1, "Rick Sanchez", "Alive", "Human", "rick.png"),
        Character(2, "Morty Smith", "Alive", "Human", "morty.png")
    )

    @BeforeTest
    fun setUp() {
        remoteDataSource = FakeCharacterRemoteDataSource()
        localDataSource = FakeCharacterLocalDataSource()
        repository = CharacterRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource
        )
    }

    // --- helpers ---

    private fun dto(id: Int, name: String = "Character $id") =
        CharacterDto(id, name, "Alive", "Human", "img$id.png")

    private fun character(id: Int, name: String = "Character $id") =
        Character(id, name, "Alive", "Human", "img$id.png")

    /** Response shaped like the real API: next/prev follow [page] and [pages]. */
    private fun pageResponse(page: Int, ids: List<Int>, pages: Int = 3) = CharacterResponseDto(
        info = PageInfoDto(
            count = pages * 20,
            pages = pages,
            next = if (page < pages) "https://rickandmortyapi.com/api/character?page=${page + 1}" else null,
            prev = if (page > 1) "https://rickandmortyapi.com/api/character?page=${page - 1}" else null
        ),
        results = ids.map { dto(it) }
    )

    private fun stubPage(page: Int, ids: List<Int>, pages: Int = 3) {
        remoteDataSource.responses[page] = pageResponse(page, ids, pages)
    }

    private suspend fun paginationState() = repository.observePaginationState().first()

    // --- observeCharacters / refreshCharacters (E3/E4 behaviour) ---

    @Test
    fun observeCharacters_initially_emitsEmptyList() = runTest {
        assertTrue(repository.observeCharacters().first().isEmpty())
    }

    @Test
    fun refreshCharacters_whenApiSucceeds_savesMappedDataToLocal() = runTest {
        repository.refreshCharacters()

        val expected = listOf(
            Character(1, "Rick Sanchez", "Alive", "Human", "https://example.com/rick.png"),
            Character(2, "Morty Smith", "Alive", "Human", "https://example.com/morty.png")
        )
        assertEquals(expected, repository.observeCharacters().first())
        assertEquals(listOf(1), remoteDataSource.requestedPages)
    }

    @Test
    fun refreshCharacters_whenApiFails_throwsAndKeepsLocalCache() = runTest {
        localDataSource.seed(cachedCharacters, CharacterPagingMetadata(nextPage = 2, totalPages = 3))
        remoteDataSource.failWithNoInternet()

        val error = assertFailsWith<DataError.NoConnection> {
            repository.refreshCharacters()
        }

        assertIs<NetworkException.NoInternet>(error.cause)
        assertEquals(cachedCharacters, repository.observeCharacters().first())
        assertEquals(CharacterPagingMetadata(2, 3), localDataSource.currentMetadata)
    }

    @Test
    fun refreshCharacters_whenResponseInvalid_throwsInvalidDataAndKeepsLocalCache() = runTest {
        localDataSource.seed(cachedCharacters, null)
        val source = NetworkException.Serialization()
        remoteDataSource.errorToThrow = source

        val error = assertFailsWith<DataError.InvalidData> {
            repository.refreshCharacters()
        }

        assertSame(source, error.cause)
        assertEquals(cachedCharacters, repository.observeCharacters().first())
    }

    @Test
    fun refreshCharacters_whenCancelled_propagatesCancellationAndKeepsLocalCache() = runTest {
        localDataSource.seed(cachedCharacters, CharacterPagingMetadata(nextPage = 2, totalPages = 3))
        remoteDataSource.errorToThrow = CancellationException("cancelled")

        // CancellationException and DataError are disjoint types, so a passing
        // assertFailsWith here also proves cancellation was not mapped.
        assertFailsWith<CancellationException> {
            repository.refreshCharacters()
        }

        assertEquals(cachedCharacters, repository.observeCharacters().first())
        assertEquals(CharacterPagingMetadata(2, 3), localDataSource.currentMetadata)
    }

    @Test
    fun refreshCharacters_whenStorageFails_keepsCacheAndMetadata() = runTest {
        localDataSource.seed(cachedCharacters, CharacterPagingMetadata(nextPage = 2, totalPages = 3))
        stubPage(1, listOf(10))
        localDataSource.writeError = IllegalStateException("disk full")

        assertFailsWith<IllegalStateException> {
            repository.refreshCharacters()
        }

        assertEquals(cachedCharacters, localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(2, 3), localDataSource.currentMetadata)
    }

    // --- PaginationState mapping ---

    @Test
    fun paginationState_whenNoMetadata_isUninitialized() = runTest {
        assertEquals(PaginationState.Uninitialized, paginationState())
    }

    @Test
    fun paginationState_whenNextPageStored_isHasMore() = runTest {
        localDataSource.seed(cachedCharacters, CharacterPagingMetadata(nextPage = 2, totalPages = 3))

        assertEquals(PaginationState.HasMore, paginationState())
    }

    @Test
    fun paginationState_whenNextPageNull_isEndReached() = runTest {
        localDataSource.seed(cachedCharacters, CharacterPagingMetadata(nextPage = null, totalPages = 1))

        assertEquals(PaginationState.EndReached, paginationState())
    }

    // --- pagination ---

    @Test
    fun refreshCharacters_storesFirstPageAndNextPage() = runTest {
        stubPage(1, listOf(1, 2))

        repository.refreshCharacters()

        assertEquals(listOf(character(1), character(2)), localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 3), localDataSource.currentMetadata)
        assertEquals(PaginationState.HasMore, paginationState())
    }

    @Test
    fun loadNextPage_appendsPage2AndKeepsPage1() = runTest {
        stubPage(1, listOf(1, 2))
        stubPage(2, listOf(3, 4))
        repository.refreshCharacters()

        repository.loadNextPage()

        assertEquals((1..4).map { character(it) }, repository.observeCharacters().first())
        assertEquals(CharacterPagingMetadata(nextPage = 3, totalPages = 3), localDataSource.currentMetadata)
        assertEquals(listOf(1, 2), remoteDataSource.requestedPages)
    }

    @Test
    fun loadNextPage_onLastPage_reachesEnd() = runTest {
        localDataSource.seed(listOf(character(1)), CharacterPagingMetadata(nextPage = 3, totalPages = 3))
        stubPage(3, listOf(5))

        repository.loadNextPage()

        assertNull(localDataSource.currentMetadata?.nextPage)
        assertEquals(PaginationState.EndReached, paginationState())
    }

    @Test
    fun refreshCharacters_afterSeveralPages_replacesWholeCache() = runTest {
        localDataSource.seed(
            (1..6).map { character(it) },
            CharacterPagingMetadata(nextPage = null, totalPages = 3)
        )
        stubPage(1, listOf(1, 2))

        repository.refreshCharacters()

        assertEquals(listOf(character(1), character(2)), localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 3), localDataSource.currentMetadata)
    }

    @Test
    fun loadNextPage_whenRequestFails_doesNotAdvanceNextPage() = runTest {
        localDataSource.seed(listOf(character(1)), CharacterPagingMetadata(nextPage = 2, totalPages = 3))
        remoteDataSource.failWithNoInternet()

        assertFailsWith<DataError.NoConnection> {
            repository.loadNextPage()
        }

        assertEquals(listOf(character(1)), localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 3), localDataSource.currentMetadata)
    }

    @Test
    fun loadNextPage_whenEndReached_makesNoRequest() = runTest {
        localDataSource.seed(listOf(character(1)), CharacterPagingMetadata(nextPage = null, totalPages = 1))

        repository.loadNextPage()

        assertTrue(remoteDataSource.requestedPages.isEmpty())
        assertEquals(listOf(character(1)), localDataSource.currentCharacters)
    }

    @Test
    fun loadNextPage_whenUninitialized_startsFromPage1() = runTest {
        // A cache migrated from database v2 has characters but no metadata.
        localDataSource.seed(cachedCharacters, null)
        stubPage(1, listOf(1, 2))

        repository.loadNextPage()

        assertEquals(listOf(1), remoteDataSource.requestedPages)
        assertEquals(listOf(character(1), character(2)), localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 3), localDataSource.currentMetadata)
    }

    @Test
    fun loadNextPage_withExistingIds_doesNotDuplicateCharacters() = runTest {
        localDataSource.seed(
            listOf(character(1), character(2)),
            CharacterPagingMetadata(nextPage = 2, totalPages = 3)
        )
        remoteDataSource.responses[2] = pageResponse(2, ids = emptyList()).copy(
            results = listOf(dto(2, name = "Morty Updated"), dto(3))
        )

        repository.loadNextPage()

        assertEquals(
            listOf(character(1), character(2, name = "Morty Updated"), character(3)),
            localDataSource.currentCharacters
        )
    }

    @Test
    fun refreshThenLoadNextPage_concurrently_loadNextUsesFreshMetadata() = runTest {
        // Pages 1-2 cached; stale nextPage would be 3.
        localDataSource.seed(
            (1..4).map { character(it) },
            CharacterPagingMetadata(nextPage = 3, totalPages = 3)
        )
        stubPage(1, listOf(1, 2))
        stubPage(2, listOf(3, 4))
        val gate = CompletableDeferred<Unit>()
        remoteDataSource.gate = gate

        val refresh = launch { repository.refreshCharacters() }
        advanceUntilIdle()
        val loadMore = launch { repository.loadNextPage() }
        advanceUntilIdle()

        // Load-more is waiting for the lock and has not requested anything yet.
        assertEquals(listOf(1), remoteDataSource.requestedPages)

        gate.complete(Unit)
        refresh.join()
        loadMore.join()

        assertEquals(listOf(1, 2), remoteDataSource.requestedPages)
        assertEquals((1..4).map { character(it) }, localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(nextPage = 3, totalPages = 3), localDataSource.currentMetadata)
    }

    @Test
    fun loadNextPage_whenCancelledDuringRequest_keepsMetadata() = runTest {
        localDataSource.seed(listOf(character(1)), CharacterPagingMetadata(nextPage = 2, totalPages = 3))
        stubPage(2, listOf(3))
        remoteDataSource.gate = CompletableDeferred()

        val job = launch { repository.loadNextPage() }
        advanceUntilIdle()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(listOf(character(1)), localDataSource.currentCharacters)
        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 3), localDataSource.currentMetadata)
    }

    @Test
    fun loadNextPage_whenCancellationThrown_propagates() = runTest {
        localDataSource.seed(listOf(character(1)), CharacterPagingMetadata(nextPage = 2, totalPages = 3))
        remoteDataSource.errorToThrow = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            repository.loadNextPage()
        }

        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 3), localDataSource.currentMetadata)
    }

    // --- invalid pagination metadata ---

    private suspend fun assertInvalidResponseKeepsCache(
        page: Int,
        info: PageInfoDto,
        action: suspend () -> Unit
    ) {
        val seeded = CharacterPagingMetadata(nextPage = 2, totalPages = 3)
        localDataSource.seed(cachedCharacters, seeded)
        remoteDataSource.responses[page] = CharacterResponseDto(info = info, results = listOf(dto(9)))

        assertFailsWith<DataError.InvalidData> { action() }

        assertEquals(cachedCharacters, localDataSource.currentCharacters)
        assertEquals(seeded, localDataSource.currentMetadata)
    }

    @Test
    fun refresh_whenNextMissingButMorePagesExist_isInvalid() = runTest {
        assertInvalidResponseKeepsCache(
            page = 1,
            info = PageInfoDto(count = 60, pages = 3, next = null, prev = null),
            action = { repository.refreshCharacters() }
        )
    }

    @Test
    fun loadNextPage_whenNextPresentOnLastPage_isInvalid() = runTest {
        assertInvalidResponseKeepsCache(
            page = 2,
            info = PageInfoDto(count = 40, pages = 2, next = "https://x/api/character?page=3", prev = null),
            action = { repository.loadNextPage() }
        )
    }

    @Test
    fun refresh_whenPagesIsZero_isInvalid() = runTest {
        assertInvalidResponseKeepsCache(
            page = 1,
            info = PageInfoDto(count = 0, pages = 0, next = null, prev = null),
            action = { repository.refreshCharacters() }
        )
    }

    @Test
    fun refresh_whenCountNegative_isInvalid() = runTest {
        assertInvalidResponseKeepsCache(
            page = 1,
            info = PageInfoDto(count = -1, pages = 1, next = null, prev = null),
            action = { repository.refreshCharacters() }
        )
    }

    @Test
    fun loadNextPage_whenRequestedPageBeyondTotal_isInvalid() = runTest {
        assertInvalidResponseKeepsCache(
            page = 2,
            info = PageInfoDto(count = 20, pages = 1, next = null, prev = null),
            action = { repository.loadNextPage() }
        )
    }
}
