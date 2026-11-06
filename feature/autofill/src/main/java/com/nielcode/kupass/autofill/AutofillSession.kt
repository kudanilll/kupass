package com.nielcode.kupass.autofill

import android.os.SystemClock

/**
 * The autofill unlock window: one verification, then fills without a prompt until the user has not
 * autofilled for [timeoutMillis]. Memory only, separate from the in-app session, and the timeout is
 * read at every check so lowering the auto-lock setting takes effect at once.
 */
class AutofillSession(
    private val timeoutMillis: () -> Long,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private var lastUse: Long? = null

    @Synchronized
    fun start() {
        lastUse = clock()
    }

    @Synchronized
    fun isActive(): Boolean {
        val since = lastUse ?: return false
        val timeout = timeoutMillis()
        val active = timeout > 0 && clock() - since < timeout
        if (!active) lastUse = null
        return active
    }

    /** Refreshes the idle timer; never revives an expired session. */
    @Synchronized
    fun touch() {
        if (isActive()) lastUse = clock()
    }

    @Synchronized
    fun end() {
        lastUse = null
    }
}
