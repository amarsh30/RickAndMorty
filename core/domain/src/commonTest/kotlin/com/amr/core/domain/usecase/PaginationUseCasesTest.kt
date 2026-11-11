package com.amr.core.domain.usecase

import com.amr.core.domain.error.DataError
import com.amr.core.domain.fake.FakeCharacterRepository
import com.amr.core.domain.model.PaginationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaginationUseCasesTest {

    private lateinit var repository: FakeCharacterRepository

    @BeforeTest
    fun setUp() {
        repository = FakeCharacterRepository()
    }

    private suspend fun observedState(): PaginationState =
        ObservePaginationStateUseCase(repository)().first()

    @Test
    fun observePaginationState_forwardsUninitialized() = runTest {
        assertEquals(PaginationState.Uninitialized, observedState())
    }

    @Test
    fun observePaginationState_forwardsHasMore() = runTest {
        repository.paginationState.value = PaginationState.HasMore

        assertEquals(PaginationState.HasMore, observedState())
    }

    @Test
    fun observePaginationState_forwardsEndReached() = runTest {
        repository.paginationState.value = PaginationState.EndReached

        assertEquals(PaginationState.EndReached, observedState())
    }

    @Test
    fun loadNextPage_callsRepositoryExactlyOnce() = runTest {
        LoadNextPageCharactersUseCase(repository)()

        assertEquals(1, repository.loadNextPageCount)
    }

    @Test
    fun loadNextPage_propagatesDataError() = runTest {
        repository.loadNextPageError = DataError.NoConnection()

        assertFailsWith<DataError.NoConnection> {
            LoadNextPageCharactersUseCase(repository)()
        }
    }

    @Test
    fun loadNextPage_propagatesCancellation() = runTest {
        repository.loadNextPageError = CancellationException("cancelled")

        assertFailsWith<CancellationException> {
            LoadNextPageCharactersUseCase(repository)()
        }
    }
}
