package com.amr.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Location cache. [inList] marks rows that belong to the paginated list; rows
 * fetched only for a detail screen have inList = false and stay out of the list.
 * Rows are never deleted by a list refresh, so detail data stays available.
 */
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val type: String,
    val dimension: String,
    val inList: Boolean
)

/**
 * Residents of a location, in API order. Character rows may live in any character
 * cache (or none yet), so only the location side has a foreign key.
 */
@Entity(
    tableName = "location_residents",
    primaryKeys = ["locationId", "characterId"],
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["characterId"])]
)
data class LocationResidentEntity(
    val locationId: Int,
    val characterId: Int,
    val position: Int
)

@Entity(tableName = "location_paging_state")
data class LocationPagingStateEntity(
    @PrimaryKey
    val id: Int = SINGLE_ROW_ID,
    val nextPage: Int?,
    val totalPages: Int?
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
