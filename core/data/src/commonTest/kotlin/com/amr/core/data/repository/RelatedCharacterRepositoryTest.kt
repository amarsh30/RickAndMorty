package com.amr.core.data.repository

import com.amr.core.data.fake.FakeCharacterDao
import com.amr.core.data.fake.FakeCharacterRemoteDataSource
import com.amr.core.data.fake.FakeFavoriteCharacterDao
import com.amr.core.data.fake.FakeSearchCharacterDao
import com.amr.core.data.fake.testCharacter
import com.amr.core.data.fake.testCharacterDto
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSourceImpl
import com.amr.core.data.mapper.toEntity
import com.amr.core.database.entity.CharacterPagingStateEntity
import com.amr.core.domain.error.DataError
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RelatedCharacterRepositoryTest {

    private lateinit var remote: FakeCharacterRemoteDataSource
    private lateinit var characterDao: FakeCharacterDao
    private lateinit var searchDao: FakeSearchCharacterDao
    private lateinit var repository: RelatedCharacterRepositoryImpl

    @BeforeTest
    fun setUp() {
        remote = FakeCharacterRemoteDataSource()
        characterDao = FakeCharacterDao()
        searchDao = FakeSearchCharacterDao()
        val local = CharacterLookupLocalDataSourceImpl(
            characterDao, searchDao, FakeFavoriteCharacterDao(),
            transactionRunner = com.amr.core.data.fake.FakeTransactionRunner(), nowMillis = { 1L }
        )
        repository = RelatedCharacterRepositoryImpl(remote, local)
        (1..200).forEach { remote.characters[it] = testCharacterDto(it) }
    }

    @Test
    fun fetchMissing_splitsIntoBatchesOfAtMost50() = runTest {
        repository.fetchMissing((1..120).toList())

        assertEquals(listOf(50, 50, 20), remote.requestedBatches.map { it.size })
        assertEquals((1..120).toList(), remote.requestedBatches.flatten())
    }

    @Test
    fun fetchMissing_skipsCachedIds_andStoresOutsideTheMainList() = runTest {
        val listRow = testCharacter(1).toEntity()
        characterDao.characters.value = listOf(listRow)
        searchDao.listRows.value = listOf(listRow)
        val paging = CharacterPagingStateEntity(nextPage = 2, totalPages = 42)
        characterDao.pagingState.value = paging

        repository.fetchMissing(listOf(1, 2, 3))

        assertEquals(listOf(listOf(2, 3)), remote.requestedBatches)
        // Main list and its paging are unchanged; fetched characters went to the search cache.
        assertEquals(listOf(listRow), characterDao.characters.value)
        assertEquals(paging, characterDao.pagingState.value)
        assertEquals(listOf(2, 3), searchDao.rows.value.map { it.id })
    }

    @Test
    fun fetchMissing_twice_doesNotRequestAgain() = runTest {
        repository.fetchMissing(listOf(1, 2))
        repository.fetchMissing(listOf(1, 2))

        assertEquals(1, remote.requestedBatches.size)
    }

    @Test
    fun unknownIds_areNotRequestedAgain() = runTest {
        repository.fetchMissing(listOf(5, 9999))
        repository.fetchMissing(listOf(5, 9999))

        assertEquals(listOf(listOf(5, 9999)), remote.requestedBatches)
    }

    @Test
    fun concurrentCalls_forTheSameIds_requestThemOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        remote.batchGate = gate

        val first = async { repository.fetchMissing(listOf(1, 2)) }
        runCurrent()
        val second = async { repository.fetchMissing(listOf(2, 3)) }
        runCurrent()
        gate.complete(Unit)
        first.await()
        second.await()

        assertEquals(listOf(listOf(1, 2), listOf(3)), remote.requestedBatches)
    }

    @Test
    fun failedBatch_releasesItsIds_soRetryRequestsThem() = runTest {
        remote.batchError = NetworkException.NoInternet()
        assertFailsWith<DataError.NoConnection> { repository.fetchMissing(listOf(1, 2)) }

        remote.batchError = null
        repository.fetchMissing(listOf(1, 2))

        assertEquals(listOf(listOf(1, 2), listOf(1, 2)), remote.requestedBatches)
        assertEquals(listOf(1, 2), searchDao.rows.value.map { it.id })
    }

    @Test
    fun responseWithUnrequestedCharacter_isInvalidData() = runTest {
        remote.characters[1] = testCharacterDto(77)

        assertFailsWith<DataError.InvalidData> { repository.fetchMissing(listOf(1)) }
        assertTrue(searchDao.rows.value.isEmpty())
    }

    @Test
    fun observeCharacters_followsRequestedOrder_andSkipsUncached() = runTest {
        repository.fetchMissing(listOf(3, 1))

        val observed = repository.observeCharacters(listOf(3, 2, 1)).first()

        assertEquals(listOf(3, 1), observed.map { it.id })
        assertEquals(emptyList(), repository.observeCharacters(emptyList()).first())
    }
}
