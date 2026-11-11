package com.amr.core.data.repository

import com.amr.core.data.error.toDataError
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.mapper.toEntity
import com.amr.core.data.mapper.toResidentEntities
import com.amr.core.data.remote.datasource.LocationRemoteDataSource
import com.amr.core.data.remote.dto.LocationDto
import com.amr.core.data.remote.paging.PageMetadata
import com.amr.core.data.remote.paging.paginationStateOf
import com.amr.core.data.remote.paging.toValidatedPageMetadata
import com.amr.core.database.dao.LocationDao
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Location
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.LocationRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocationRepositoryImpl(
    private val remoteDataSource: LocationRemoteDataSource,
    private val locationDao: LocationDao
) : LocationRepository {

    /** Serializes refresh and load-more so metadata is never read while another write is pending. */
    private val pagingMutex = Mutex()

    override fun observeLocations(): Flow<List<Location>> =
        locationDao.observeListLocations().map { rows -> rows.map { it.toDomain() } }

    override fun observePaginationState(): Flow<PaginationState> =
        locationDao.observePagingState().map { paginationStateOf(it != null, it?.nextPage) }

    override suspend fun refreshLocations() {
        pagingMutex.withLock { replaceWithFirstPage() }
    }

    override suspend fun loadNextPage() {
        pagingMutex.withLock {
            // Read inside the lock so a refresh that just finished is taken into account.
            val state = locationDao.getPagingState() ?: return replaceWithFirstPage()
            val nextPage = state.nextPage ?: return

            val (locations, metadata) = fetchPage(nextPage)
            locationDao.appendPage(
                locations = locations.map { it.toEntity() },
                residents = locations.flatMap { it.toResidentEntities() },
                nextPage = metadata.nextPage,
                totalPages = metadata.totalPages
            )
        }
    }

    override fun observeLocation(id: Int): Flow<Location?> =
        locationDao.observeLocation(id).map { it?.toDomain() }

    override fun observeResidentIds(id: Int): Flow<List<Int>> =
        locationDao.observeResidentIds(id)

    override suspend fun refreshLocation(id: Int) {
        val dto = try {
            remoteDataSource.getLocation(id)
        } catch (e: NetworkException) {
            throw e.toDataError()
        }
        // Guards against a response for a different resource.
        if (dto.id != id) throw DataError.InvalidData()

        locationDao.saveDetail(dto.toEntity(inList = false), dto.toResidentEntities())
    }

    /** The list is only replaced after page 1 has been fetched and validated. */
    private suspend fun replaceWithFirstPage() {
        val (locations, metadata) = fetchPage(FIRST_PAGE)
        locationDao.replaceFirstPage(
            locations = locations.map { it.toEntity() },
            residents = locations.flatMap { it.toResidentEntities() },
            nextPage = metadata.nextPage,
            totalPages = metadata.totalPages
        )
    }

    private suspend fun fetchPage(page: Int): Pair<List<LocationDto>, PageMetadata> {
        val response = try {
            remoteDataSource.getLocations(page)
        } catch (e: NetworkException) {
            throw e.toDataError()
        }
        return response.results to response.info.toValidatedPageMetadata(requestedPage = page)
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
