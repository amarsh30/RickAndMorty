package com.amr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pagination metadata for the cached character list, stored as a single row.
 *
 * A missing row means no page has been stored yet. Once the row exists,
 * [nextPage] = null means the last page has been reached.
 */
@Entity(tableName = "character_paging_state")
data class CharacterPagingStateEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val nextPage: Int?,
    val totalPages: Int?
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
