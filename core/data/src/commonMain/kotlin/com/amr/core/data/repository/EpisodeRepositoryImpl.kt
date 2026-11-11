package com.amr.core.data.repository

import com.amr.core.data.error.toDataError
import com.amr.core.data.mapper.toCharacterLinks
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.mapper.toEntity
import com.amr.core.data.remote.datasource.EpisodeRemoteDataSource
import com.amr.core.data.remote.dto.EpisodeDto
import com.amr.core.data.remote.paging.PageMetadata
import com.amr.core.data.remote.paging.paginationStateOf
import com.amr.core.data.remote.paging.toValidatedPageMetadata
import com.amr.core.database.dao.EpisodeDao
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Episode
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.EpisodeRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EpisodeRepositoryImpl(
    private val remoteDataSource: EpisodeRemoteDataSource,
    private val episodeDao: EpisodeDao
) : EpisodeRepository {

    /** Serializes refresh and load-more so metadata is never read while another write is pending. */
    private val pagingMutex = Mutex()

    override fun observeEpisodes(): Flow<List<Episode>> =
        episodeDao.observeListEpisodes().map { rows -> rows.map { it.toDomain() } }

    override fun observePaginationState(): Flow<PaginationState> =
        episodeDao.observePagingState().map { paginationStateOf(it != null, it?.nextPage) }

    override suspend fun refreshEpisodes() {
        pagingMutex.withLock { replaceWithFirstPage() }
    }

    override suspend fun loadNextPage() {
        pagingMutex.withLock {
            // Read inside the lock so a refresh that just finished is taken into account.
            val state = episodeDao.getPagingState() ?: return replaceWithFirstPage()
            val nextPage = state.nextPage ?: return

            val (episodes, metadata) = fetchPage(nextPage)
            episodeDao.appendPage(
                episodes = episodes.map { it.toEntity() },
                links = episodes.flatMap { it.toCharacterLinks() },
                nextPage = metadata.nextPage,
                totalPages = metadata.totalPages
            )
        }
    }

    override fun observeEpisode(id: Int): Flow<Episode?> =
        episodeDao.observeEpisode(id).map { it?.toDomain() }

    override fun observeCharacterIds(id: Int): Flow<List<Int>> =
        episodeDao.observeCharacterIds(id)

    override suspend fun refreshEpisode(id: Int) {
        val dto = try {
            remoteDataSource.getEpisode(id)
        } catch (e: NetworkException) {
            throw e.toDataError()
        }
        // Guards against a response for a different resource.
        if (dto.id != id) throw DataError.InvalidData()

        episodeDao.saveDetail(dto.toEntity(inList = false), dto.toCharacterLinks())
    }

    /** The list is only replaced after page 1 has been fetched and validated. */
    private suspend fun replaceWithFirstPage() {
        val (episodes, metadata) = fetchPage(FIRST_PAGE)
        episodeDao.replaceFirstPage(
            episodes = episodes.map { it.toEntity() },
            links = episodes.flatMap { it.toCharacterLinks() },
            nextPage = metadata.nextPage,
            totalPages = metadata.totalPages
        )
    }

    private suspend fun fetchPage(page: Int): Pair<List<EpisodeDto>, PageMetadata> {
        val response = try {
            remoteDataSource.getEpisodes(page)
        } catch (e: NetworkException) {
            throw e.toDataError()
        }
        return response.results to response.info.toValidatedPageMetadata(requestedPage = page)
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
