package com.amr.feature.location.presentation

import com.amr.core.domain.error.DataError

internal const val GENERIC_ERROR_MESSAGE = "Terjadi kesalahan. Coba lagi."
internal const val STALE_DATA_MESSAGE = "Gagal memperbarui data. Menampilkan data tersimpan."
internal const val DATABASE_ERROR_MESSAGE = "Gagal membaca data tersimpan. Coba lagi."

internal fun DataError.toUserMessage(): String = when (this) {
    is DataError.NoConnection -> "Gagal memuat lokasi. Periksa koneksi internet."
    is DataError.Timeout -> "Server terlalu lama merespons. Coba lagi."
    is DataError.Unauthorized -> "Akses ke data ditolak."
    is DataError.NotFound -> "Data lokasi tidak ditemukan."
    is DataError.Server -> "Server sedang bermasalah. Coba lagi nanti."
    is DataError.InvalidData -> "Data dari server tidak dapat dibaca."
    is DataError.Unknown -> GENERIC_ERROR_MESSAGE
}

internal fun Throwable.toUserMessage(): String =
    (this as? DataError)?.toUserMessage() ?: GENERIC_ERROR_MESSAGE
