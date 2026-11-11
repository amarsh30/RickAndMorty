package com.amr.core.data.local.datasource

import com.amr.core.data.fake.FakeCharacterDao
import com.amr.core.data.fake.FakeCharacterRemoteDataSource
import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.data.repository.CharacterRepositoryImpl
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.CharacterPagingStateEntity
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.PaginationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real Room-row mapping in [CharacterLocalDataSourceImpl],
 * through the repository, with a DAO that can hold an invalid metadata row.
 */
class CharacterPagingMetadataMappingTest {

    private lateinit var dao: FakeCharacterDao
    private lateinit var remoteDataSource: FakeCharacterRemoteDataSource
    private lateinit var localDataSource: CharacterLocalDataSourceImpl
    private lateinit var repository: CharacterRepositoryImpl

    private val cachedEntities = listOf(
        CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png"),
        CharacterEntity(2, "Morty Smith", "Alive", "Human", "morty.png")
    )

    /** A metadata row exists, but totalPages was never written correctly. */
    private val invalidRow = CharacterPagingStateEntity(nextPage = 2, totalPages = null)

    @BeforeTest
    fun setUp() {
        dao = FakeCharacterDao()
        remoteDataSource = FakeCharacterRemoteDataSource()
        localDataSource = CharacterLocalDataSourceImpl(dao)
        repository = CharacterRepositoryImpl(remoteDataSource, localDataSource)
    }

    @Test
    fun noMetadataRow_isNullAndUninitialized() = runTest {
        assertNull(localDataSource.getPagingMetadata())
        assertNull(localDataSource.observePagingMetadata().first())
        assertEquals(PaginationState.Uninitialized, repository.observePaginationState().first())
    }

    @Test
    fun validRowWithNextPage_isHasMore() = runTest {
        dao.pagingState.value = CharacterPagingStateEntity(nextPage = 2, totalPages = 42)

        assertEquals(CharacterPagingMetadata(nextPage = 2, totalPages = 42), localDataSource.getPagingMetadata())
        assertEquals(PaginationState.HasMore, repository.observePaginationState().first())
    }

    @Test
    fun validRowWithoutNextPage_isEndReached() = runTest {
        dao.pagingState.value = CharacterPagingStateEntity(nextPage = null, totalPages = 42)

        assertEquals(PaginationState.EndReached, repository.observePaginationState().first())
    }

    @Test
    fun rowWithNullTotalPages_isInvalidData_notNull() = runTest {
        dao.pagingState.value = invalidRow

        assertFailsWith<DataError.InvalidData> { localDataSource.getPagingMetadata() }
        assertFailsWith<DataError.InvalidData> { localDataSource.observePagingMetadata().first() }
    }

    @Test
    fun rowWithNullTotalPages_paginationStateIsNotUninitialized() = runTest {
        dao.pagingState.value = invalidRow

        assertFailsWith<DataError.InvalidData> {
            repository.observePaginationState().first()
        }
    }

    @Test
    fun rowWithNullTotalPages_loadNextPageMakesNoRequestAndKeepsCache() = runTest {
        dao.characters.value = cachedEntities
        dao.pagingState.value = invalidRow

        assertFailsWith<DataError.InvalidData> {
            repository.loadNextPage()
        }

        assertTrue(remoteDataSource.requestedPages.isEmpty())
        assertEquals(cachedEntities, dao.characters.value)
        assertEquals(invalidRow, dao.pagingState.value)
    }
}
