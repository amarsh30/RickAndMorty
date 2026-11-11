package com.amr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.amr.core.database.entity.EpisodeCharacterEntity
import com.amr.core.database.entity.EpisodeEntity
import com.amr.core.database.entity.EpisodePagingStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Episodes are written with upsert/update only: REPLACE would delete the row
 * first and cascade-delete its character links.
 */
@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE inList = 1 ORDER BY id ASC")
    fun observeListEpisodes(): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun observeEpisode(id: Int): Flow<EpisodeEntity?>

    @Query("SELECT characterId FROM episode_characters WHERE episodeId = :episodeId ORDER BY position ASC")
    fun observeCharacterIds(episodeId: Int): Flow<List<Int>>

    @Query("SELECT * FROM episode_paging_state WHERE id = ${EpisodePagingStateEntity.SINGLE_ROW_ID}")
    suspend fun getPagingState(): EpisodePagingStateEntity?

    @Query("SELECT * FROM episode_paging_state WHERE id = ${EpisodePagingStateEntity.SINGLE_ROW_ID}")
    fun observePagingState(): Flow<EpisodePagingStateEntity?>

    @Upsert
    suspend fun upsertPagingState(state: EpisodePagingStateEntity)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    /** Removes every row from the list without deleting it. */
    @Query("UPDATE episodes SET inList = 0 WHERE inList = 1")
    suspend fun clearListMembership()

    /** Updates the fields of an existing row, keeping its list membership. */
    @Query("UPDATE episodes SET name = :name, airDate = :airDate, code = :code WHERE id = :id")
    suspend fun updateEpisodeFields(id: Int, name: String, airDate: String, code: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisodeIfAbsent(episode: EpisodeEntity)

    @Query("DELETE FROM episode_characters WHERE episodeId IN (:episodeIds)")
    suspend fun deleteCharacterLinks(episodeIds: List<Int>)

    @Insert
    suspend fun insertCharacterLinks(links: List<EpisodeCharacterEntity>)

    /** Atomically makes [episodes] the whole list and resets the pagination metadata. */
    @Transaction
    suspend fun replaceFirstPage(
        episodes: List<EpisodeEntity>,
        links: List<EpisodeCharacterEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        clearListMembership()
        savePage(episodes, links, nextPage, totalPages)
    }

    /** Atomically adds a following page to the list and advances the pagination metadata. */
    @Transaction
    suspend fun appendPage(
        episodes: List<EpisodeEntity>,
        links: List<EpisodeCharacterEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        savePage(episodes, links, nextPage, totalPages)
    }

    /** Stores an episode fetched for its detail screen without changing list membership. */
    @Transaction
    suspend fun saveDetail(episode: EpisodeEntity, links: List<EpisodeCharacterEntity>) {
        val updated = updateEpisodeFields(episode.id, episode.name, episode.airDate, episode.code)
        if (updated == 0) insertEpisodeIfAbsent(episode.copy(inList = false))
        replaceCharacterLinks(listOf(episode.id), links)
    }

    private suspend fun savePage(
        episodes: List<EpisodeEntity>,
        links: List<EpisodeCharacterEntity>,
        nextPage: Int?,
        totalPages: Int?
    ) {
        upsertEpisodes(episodes.map { it.copy(inList = true) })
        replaceCharacterLinks(episodes.map { it.id }, links)
        upsertPagingState(EpisodePagingStateEntity(nextPage = nextPage, totalPages = totalPages))
    }

    private suspend fun replaceCharacterLinks(episodeIds: List<Int>, links: List<EpisodeCharacterEntity>) {
        deleteCharacterLinks(episodeIds)
        insertCharacterLinks(links)
    }
}
