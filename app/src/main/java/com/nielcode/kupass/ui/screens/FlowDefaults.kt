package com.nielcode.kupass.ui.screens

import android.database.SQLException
import com.nielcode.kupass.security.CryptoException
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted

/**
 * Keeps upstream flows alive for 5 seconds after the UI stops collecting, so a configuration change
 * doesn't restart them.
 */
internal val WhileUiSubscribed = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)

/**
 * Runs [block] and returns null if it fails in a way the UI can report and recover from: storage or
 * file access (I/O, a revoked document permission), crypto, or the database. Anything else,
 * including coroutine cancellation, propagates.
 */
internal inline fun <T> recoverable(block: () -> T): T? =
    try {
        block()
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: CryptoException) {
        null
    } catch (_: SQLException) {
        null
    }
