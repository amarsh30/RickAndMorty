package com.amr.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Snapshot of a favorited character. Independent of the list caches, so it
 * survives list refreshes and stays available offline. Columns mirror [CharacterEntity].
 */
@Entity(tableName = "favorite_characters")
data class FavoriteCharacterEntity(
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
    val episodeIds: String?
)
