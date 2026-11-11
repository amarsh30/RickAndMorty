package com.amr.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Episode cache. Same list-membership rules as [LocationEntity]: [inList] marks
 * paginated-list rows, and a list refresh never deletes rows.
 */
@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    /** As sent by the API, e.g. "December 2, 2013". */
    val airDate: String,
    /** Season/episode code, e.g. "S01E01". */
    val code: String,
    val inList: Boolean
)

/** Characters appearing in an episode, in API order. */
@Entity(
    tableName = "episode_characters",
    primaryKeys = ["episodeId", "characterId"],
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["characterId"])]
)
data class EpisodeCharacterEntity(
    val episodeId: Int,
    val characterId: Int,
    val position: Int
)

@Entity(tableName = "episode_paging_state")
data class EpisodePagingStateEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val nextPage: Int?,
    val totalPages: Int?
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
