package com.amr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Main character list cache (paginated).
 *
 * Detail columns were added in v4 and are null for rows cached before that,
 * until the character is fetched again.
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val imageUrl: String?,
    val type: String? = null,
    val gender: String? = null,
    val originName: String? = null,
    /** Location id of the origin; null when unknown. */
    val originId: Int? = null,
    val locationName: String? = null,
    /** Location id of the last known location; null when unknown. */
    val locationId: Int? = null,
    /** Comma-separated episode ids, e.g. "1,2,3". */
    val episodeIds: String? = null
)
