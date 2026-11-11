package com.amr.core.domain.model

/**
 * What the platform reports about the device's network. This says nothing about
 * whether the Rick and Morty API answers; see [ApiStatus] for that.
 */
data class NetworkStatus(
    /** A network path the OS considers usable exists. */
    val isConnected: Boolean,
    /**
     * Whether the OS verified real internet access on that path: true/false on
     * Android (NET_CAPABILITY_VALIDATED), null where the platform cannot tell (iOS).
     */
    val isInternetValidated: Boolean?
) {
    /** Connected and not known to lack internet access. */
    val isUsable: Boolean get() = isConnected && isInternetValidated != false

    companion object {
        val Disconnected = NetworkStatus(isConnected = false, isInternetValidated = false)
    }
}

/** API health derived only from the outcome of real requests (never from pings). */
enum class ApiStatus {
    /** No request finished since start or since the last offline period. */
    Unknown,
    /** The last request got an answer from the API. */
    Reachable,
    /**
     * The last request timed out. That proves nothing about the server: a slow or
     * lossy network looks the same, so the cause is reported as unknown.
     */
    Unstable,
    /** The server itself answered badly: 5xx, 429, or unreadable data. */
    Degraded
}

/** The single status the app shows globally, most severe first. */
enum class AppConnectivity {
    Offline,
    NoInternet,
    /** Requests time out; network or service, the cause is unknown. */
    Unstable,
    ApiDegraded,
    Normal
}
