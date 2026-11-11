package com.amr.core.domain.repository

import com.amr.core.domain.model.ApiStatus
import com.amr.core.domain.model.NetworkStatus
import kotlinx.coroutines.flow.Flow

/**
 * Platform network status. Implementations register their OS callback only while
 * collected and release it when collection stops; they never poll.
 */
interface ConnectivityObserver {
    val networkStatus: Flow<NetworkStatus>
}

/** API health as recorded from real request outcomes. */
interface ApiStatusRepository {
    val apiStatus: Flow<ApiStatus>
}
