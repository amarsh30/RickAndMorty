package com.amr.core.domain.repository

import com.amr.core.domain.model.Episode
import com.amr.core.domain.model.PaginationState
import kotlinx.coroutines.flow.Flow

/** Offline-first: Room is the source of every observed value; refreshes only write to it. */
interface EpisodeRepository {

    /** The paginated list, in id order. */
    fun observeEpisodes(): Flow<List<Episode>>

    fun observePaginationState(): Flow<PaginationState>

    /** Replaces the list with page 1. @throws com.amr.core.domain.error.DataError */
    suspend fun refreshEpisodes()

    /** Appends the next page; does nothing at the end. @throws com.amr.core.domain.error.DataError */
    suspend fun loadNextPage()

    /** The cached episode (listed or not), or null while not cached. */
    fun observeEpisode(id: Int): Flow<Episode?>

    /** Ids of the characters in the episode, in API order. */
    fun observeCharacterIds(id: Int): Flow<List<Int>>

    /** Fetches one episode into the cache. @throws com.amr.core.domain.error.DataError */
    suspend fun refreshEpisode(id: Int)
}
