package com.amr.core.data.mapper

import com.amr.core.database.entity.CharacterEntity
import com.amr.core.database.entity.FavoriteCharacterEntity
import com.amr.core.database.entity.SearchCharacterEntity
import com.amr.core.domain.model.Character

fun CharacterEntity.toDomain(): Character {
    return Character(
        id = id,
        name = name,
        status = status,
        species = species,
        imageUrl = imageUrl,
        type = type,
        gender = gender,
        originName = originName,
        originId = originId,
        locationName = locationName,
        locationId = locationId,
        episodeIds = episodeIds?.toEpisodeIds()
    )
}

fun Character.toEntity(): CharacterEntity {
    return CharacterEntity(
        id = id,
        name = name,
        status = status,
        species = species,
        imageUrl = imageUrl,
        type = type,
        gender = gender,
        originName = originName,
        originId = originId,
        locationName = locationName,
        locationId = locationId,
        episodeIds = episodeIds?.toEpisodeIdsColumn()
    )
}

fun SearchCharacterEntity.toDomain(): Character = Character(
    id = id, name = name, status = status, species = species, imageUrl = imageUrl,
    type = type, gender = gender, originName = originName, originId = originId,
    locationName = locationName, locationId = locationId,
    episodeIds = episodeIds?.toEpisodeIds()
)

/** [updatedAt] is the epoch millis at which the character was fetched from the API. */
fun Character.toSearchEntity(updatedAt: Long): SearchCharacterEntity = SearchCharacterEntity(
    id = id, name = name, status = status, species = species, imageUrl = imageUrl,
    type = type, gender = gender, originName = originName, originId = originId,
    locationName = locationName, locationId = locationId,
    episodeIds = episodeIds?.toEpisodeIdsColumn(),
    updatedAt = updatedAt
)

fun FavoriteCharacterEntity.toDomain(): Character = Character(
    id = id, name = name, status = status, species = species, imageUrl = imageUrl,
    type = type, gender = gender, originName = originName, originId = originId,
    locationName = locationName, locationId = locationId,
    episodeIds = episodeIds?.toEpisodeIds()
)

fun Character.toFavoriteEntity(): FavoriteCharacterEntity = FavoriteCharacterEntity(
    id = id, name = name, status = status, species = species, imageUrl = imageUrl,
    type = type, gender = gender, originName = originName, originId = originId,
    locationName = locationName, locationId = locationId,
    episodeIds = episodeIds?.toEpisodeIdsColumn()
)
