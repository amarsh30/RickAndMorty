package com.amr.core.data.mapper

import com.amr.core.data.remote.dto.EpisodeDto
import com.amr.core.data.remote.dto.LocationDto
import com.amr.core.database.entity.EpisodeCharacterEntity
import com.amr.core.database.entity.EpisodeEntity
import com.amr.core.database.entity.LocationEntity
import com.amr.core.database.entity.LocationResidentEntity
import com.amr.core.domain.model.Episode
import com.amr.core.domain.model.Location

/** [inList] is only a default: the DAO decides list membership when saving. */
fun LocationDto.toEntity(inList: Boolean = true) =
    LocationEntity(id = id, name = name, type = type, dimension = dimension, inList = inList)

/** Residents in API order; duplicates are dropped so the composite key stays unique. */
fun LocationDto.toResidentEntities(): List<LocationResidentEntity> =
    residents.characterIdsFromUrls().mapIndexed { position, characterId ->
        LocationResidentEntity(locationId = id, characterId = characterId, position = position)
    }

fun LocationEntity.toDomain() = Location(id = id, name = name, type = type, dimension = dimension)

fun EpisodeDto.toEntity(inList: Boolean = true) =
    EpisodeEntity(id = id, name = name, airDate = airDate, code = code, inList = inList)

fun EpisodeDto.toCharacterLinks(): List<EpisodeCharacterEntity> =
    characters.characterIdsFromUrls().mapIndexed { position, characterId ->
        EpisodeCharacterEntity(episodeId = id, characterId = characterId, position = position)
    }

fun EpisodeEntity.toDomain() = Episode(id = id, name = name, airDate = airDate, code = code)

/** A malformed URL is reported as invalid data rather than silently skipped. */
private fun List<String>.characterIdsFromUrls(): List<Int> =
    mapNotNull { resourceIdOrNull(it) }.distinct()
