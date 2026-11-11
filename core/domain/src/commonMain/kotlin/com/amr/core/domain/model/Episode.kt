package com.amr.core.domain.model

data class Episode(
    val id: Int,
    val name: String,
    /** As sent by the API, e.g. "December 2, 2013". */
    val airDate: String,
    /** Season/episode code, e.g. "S01E01". */
    val code: String
)
