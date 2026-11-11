package com.amr.core.data.remote.paging

import com.amr.core.data.remote.dto.PageInfoDto
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.PaginationState

/** Maps stored metadata to the domain state: no row yet means nothing was loaded. */
fun paginationStateOf(hasMetadata: Boolean, nextPage: Int?): PaginationState = when {
    !hasMetadata -> PaginationState.Uninitialized
    nextPage != null -> PaginationState.HasMore
    else -> PaginationState.EndReached
}

/** Pagination metadata derived from a validated list response. */
data class PageMetadata(
    val nextPage: Int?,
    val totalPages: Int
)

/**
 * Derives the metadata to store from `info`, rejecting responses whose
 * pagination fields contradict each other or the requested page.
 *
 * @throws DataError.InvalidData when the response is inconsistent.
 */
fun PageInfoDto.toValidatedPageMetadata(requestedPage: Int): PageMetadata {
    val hasNext = next != null
    val isValid = requestedPage >= 1 &&
        count >= 0 &&
        pages >= 1 &&
        requestedPage <= pages &&
        // A next page exists exactly when this is not the last page.
        hasNext == (requestedPage < pages)

    if (!isValid) throw DataError.InvalidData()

    return PageMetadata(
        nextPage = if (hasNext) requestedPage + 1 else null,
        totalPages = pages
    )
}
