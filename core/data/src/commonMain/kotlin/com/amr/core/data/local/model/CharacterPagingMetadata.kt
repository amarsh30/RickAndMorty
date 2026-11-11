package com.amr.core.data.local.model

/**
 * Stored pagination metadata of the character cache, independent of the Room entity.
 * [nextPage] = null means the last page has been stored.
 */
data class CharacterPagingMetadata(
    val nextPage: Int?,
    val totalPages: Int
)
