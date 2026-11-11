package com.amr.core.data.mapper

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.domain.model.Character

/** @throws com.amr.core.domain.error.DataError.InvalidData when a resource URL is malformed. */
fun CharacterDto.toDomain(): Character {
    return Character(
        id = id,
        name = name,
        status = status,
        species = species,
        imageUrl = imageUrl,
        type = type,
        gender = gender,
        originName = origin.name,
        originId = resourceIdOrNull(origin.url),
        locationName = location.name,
        locationId = resourceIdOrNull(location.url),
        episodeIds = episode.map { url -> requireNotNull(resourceIdOrNull(url)) }
    )
}
