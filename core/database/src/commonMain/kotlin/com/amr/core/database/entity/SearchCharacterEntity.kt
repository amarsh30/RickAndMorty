package com.amr.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Characters found by remote search or opened from outside the main list.
 * Kept apart from [CharacterEntity] so they never enter the paginated list.
 * Columns mirror [CharacterEntity].
 *
 * One row is one character (not a query result), so [updatedAt] is the freshness
 * of that character's data. The table is bounded by evicting the oldest rows.
 */
@Entity(
    tableName = "search_characters",
    indices = [Index(value = ["updatedAt"])]
)
data class SearchCharacterEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val imageUrl: String?,
    val type: String?,
    val gender: String?,
    val originName: String?,
    val originId: Int?,
    val locationName: String?,
    val locationId: Int?,
    val episodeIds: String?,
    /** Epoch millis of the last successful API save; 0 for rows cached before v5. */
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Long = 0
)
