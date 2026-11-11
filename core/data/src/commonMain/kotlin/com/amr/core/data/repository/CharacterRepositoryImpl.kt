package com.amr.core.data.repository

import com.amr.core.data.error.toDataError
import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.local.model.CharacterPagingMetadata
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.paging.toValidatedPageMetadata
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.CharacterRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CharacterRepositoryImpl(
    private val remoteDataSource: CharacterRemoteDataSource,
    private val localDataSource: CharacterLocalDataSource
) : CharacterRepository {

    /** Serializes refresh and load-more so metadata is never read while another write is pending. */
    private val pagingMutex = Mutex()

    override fun observeCharacters(): Flow<List<Character>> {
        return localDataSource.observeCharacters()
    }

    override fun observePaginationState(): Flow<PaginationState> {
        return localDataSource.observePagingMetadata()
            .map { metadata ->
                when {
                    metadata == null -> PaginationState.Uninitialized
                    metadata.nextPage != null -> PaginationState.HasMore
                    else -> PaginationState.EndReached
                }
            }
    }

    override suspend fun refreshCharacters() {
        pagingMutex.withLock {
            replaceWithFirstPage()
        }
    }

    override suspend fun loadNextPage() {
        pagingMutex.withLock {
            // Read inside the lock so a refresh that just finished is taken into account.
            val metadata = localDataSource.getPagingMetadata()
                ?: return replaceWithFirstPage()
            val nextPage = metadata.nextPage ?: return

            val page = fetchPage(nextPage)
            localDataSource.appendPage(page.characters, page.metadata)
        }
    }

    /** The cache is only replaced after page 1 has been fetched and validated. */
    private suspend fun replaceWithFirstPage() {
        val page = fetchPage(CharacterRemoteDataSource.FIRST_PAGE)
        localDataSource.replaceFirstPage(page.characters, page.metadata)
    }

    private suspend fun fetchPage(page: Int): FetchedPage {
        val response = try {
            remoteDataSource.getCharacters(page)
        } catch (e: NetworkException) {
            throw e.toDataError()
        }

        val metadata = response.info.toValidatedPageMetadata(requestedPage = page)
        return FetchedPage(
            characters = response.results.map { it.toDomain() },
            metadata = CharacterPagingMetadata(
                nextPage = metadata.nextPage,
                totalPages = metadata.totalPages
            )
        )
    }

    private class FetchedPage(
        val characters: List<Character>,
        val metadata: CharacterPagingMetadata
    )
}
