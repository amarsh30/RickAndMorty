package com.amr.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.amr.core.database.entity.FavoriteCharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCharacterDao {

    @Query("SELECT * FROM favorite_characters ORDER BY name ASC, id ASC")
    fun observeFavorites(): Flow<List<FavoriteCharacterEntity>>

    @Query("SELECT * FROM favorite_characters WHERE id = :id")
    fun observeFavorite(id: Int): Flow<FavoriteCharacterEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_characters WHERE id = :id)")
    fun observeIsFavorite(id: Int): Flow<Boolean>

    @Upsert
    suspend fun upsertFavorite(favorite: FavoriteCharacterEntity)

    /** Refreshes an existing snapshot only; does nothing when the character is not a favorite. */
    @Query(
        """
        UPDATE favorite_characters SET
            name = :name, status = :status, species = :species, imageUrl = :imageUrl,
            type = :type, gender = :gender, originName = :originName, originId = :originId,
            locationName = :locationName, locationId = :locationId, episodeIds = :episodeIds
        WHERE id = :id
        """
    )
    suspend fun updateFavoriteIfExists(
        id: Int,
        name: String,
        status: String,
        species: String,
        imageUrl: String?,
        type: String?,
        gender: String?,
        originName: String?,
        originId: Int?,
        locationName: String?,
        locationId: Int?,
        episodeIds: String?
    )

    @Query("DELETE FROM favorite_characters WHERE id = :id")
    suspend fun deleteFavorite(id: Int)
}
