package com.amr.core.domain.repository

import com.amr.core.domain.model.Location
import com.amr.core.domain.model.PaginationState
import kotlinx.coroutines.flow.Flow

/** Offline-first: Room is the source of every observed value; refreshes only write to it. */
interface LocationRepository {

    /** The paginated list, in id order. */
    fun observeLocations(): Flow<List<Location>>

    fun observePaginationState(): Flow<PaginationState>

    /** Replaces the list with page 1. @throws com.amr.core.domain.error.DataError */
    suspend fun refreshLocations()

    /** Appends the next page; does nothing at the end. @throws com.amr.core.domain.error.DataError */
    suspend fun loadNextPage()

    /** The cached location (listed or not), or null while not cached. */
    fun observeLocation(id: Int): Flow<Location?>

    /** Resident character ids in API order; empty while the location is not cached. */
    fun observeResidentIds(id: Int): Flow<List<Int>>

    /** Fetches one location into the cache. @throws com.amr.core.domain.error.DataError */
    suspend fun refreshLocation(id: Int)
}
