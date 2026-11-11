package com.amr.core.domain.model

/**
 * Whether more characters can be loaded after the ones already cached.
 */
sealed interface PaginationState {

    /** No page has been stored yet, so it is unknown whether more pages exist. */
    data object Uninitialized : PaginationState

    /** At least one more page can be loaded. */
    data object HasMore : PaginationState

    /** The last page has been stored; loading more must not hit the network. */
    data object EndReached : PaginationState
}
