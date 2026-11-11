package com.amr.core.data.mapper

import com.amr.core.domain.error.DataError

/**
 * Extracts the numeric id from a resource URL such as
 * "https://rickandmortyapi.com/api/location/3". An empty URL means the
 * resource is unknown and yields null; any other URL without a positive id
 * is rejected as invalid data.
 */
internal fun resourceIdOrNull(url: String): Int? {
    if (url.isBlank()) return null
    return url.trimEnd('/').substringAfterLast('/').toIntOrNull()?.takeIf { it > 0 }
        ?: throw DataError.InvalidData()
}

internal fun List<Int>.toEpisodeIdsColumn(): String = joinToString(",")

/** Inverse of [toEpisodeIdsColumn]; a malformed stored value is reported, not guessed. */
internal fun String.toEpisodeIds(): List<Int> {
    if (isEmpty()) return emptyList()
    return split(",").map { it.toIntOrNull() ?: throw DataError.InvalidData() }
}
