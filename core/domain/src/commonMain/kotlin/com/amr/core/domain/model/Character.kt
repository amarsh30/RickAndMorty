package com.amr.core.domain.model

/**
 * A character. The detail fields are null when only the list data is known
 * (e.g. rows cached before detail data was stored) until it is fetched again.
 */
data class Character(
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
    val episodeIds: List<Int>? = null
) {
    /** True once the detail fields have been loaded. */
    val hasDetails: Boolean get() = episodeIds != null
}
