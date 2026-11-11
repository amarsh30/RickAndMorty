package com.amr.feature.character.presentation

import com.amr.core.domain.error.DataError

internal const val GENERIC_ERROR_MESSAGE = "Terjadi kesalahan. Coba lagi."

internal fun DataError.toUserMessage(): String {
    return when (this) {
        is DataError.NoConnection -> "Gagal memuat karakter. Periksa koneksi internet."
        is DataError.Timeout -> "Server terlalu lama merespons. Coba lagi."
        is DataError.Unauthorized -> "Akses ke data ditolak."
        is DataError.NotFound -> "Data karakter tidak ditemukan."
        is DataError.Server -> "Server sedang bermasalah. Coba lagi nanti."
        is DataError.InvalidData -> "Data dari server tidak dapat dibaca."
        is DataError.Unknown -> GENERIC_ERROR_MESSAGE
    }
}

/** Failures for which cached data is still the best answer available. */
internal val DataError.isConnectivityProblem: Boolean
    get() = this is DataError.NoConnection || this is DataError.Timeout
