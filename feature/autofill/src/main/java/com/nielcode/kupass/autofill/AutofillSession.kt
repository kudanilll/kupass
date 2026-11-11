package com.nielcode.kupass.autofill

import android.os.SystemClock

/**
 * The autofill unlock window: one verification, then fills without a prompt for [timeoutMillis]
 * from that verification. Using the session never extends it; only a new verification does. Memory
 * only, separate from the in-app session, and the timeout is read at every check so lowering the
 * auto-lock setting takes effect at once.
 */
class AutofillSession(
    private val timeoutMillis: () -> Long,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private var verifiedAt: Long? = null

    /** Call after a successful verification. */
    @Synchronized
    fun start() {
        verifiedAt = clock()
    }

    @Synchronized
    fun isActive(): Boolean {
        val since = verifiedAt ?: return false
        val timeout = timeoutMillis()
        val active = timeout > 0 && clock() - since < timeout
        if (!active) verifiedAt = null
        return active
    }

    @Synchronized
    fun end() {
        verifiedAt = null
    }
}
