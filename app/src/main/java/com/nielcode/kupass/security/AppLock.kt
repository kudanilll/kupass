package com.nielcode.kupass.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-level vault lock (decision Q3: the Keystore key is not bound to user authentication).
 *
 * The vault starts locked in every new process and re-locks when the app has been in the
 * background for at least [timeoutMillis]. Leaving the app on purpose (file picker, device
 * credential screen, browser, licenses) can be exempted with [allowNextBackground].
 *
 * Driven by [MainActivity][com.nielcode.kupass.MainActivity] lifecycle callbacks. Pure Kotlin so it
 * can be unit tested with a fake [clock].
 */
class AppLock(
    private val timeoutMillis: () -> Long,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _locked = MutableStateFlow(true)

    /** True while vault content must not be shown. */
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundedAt: Long? = null
    /** When [allowNextBackground] was called; the exemption expires after [ALLOWANCE_WINDOW_MILLIS]. */
    private var backgroundAllowedAt: Long? = null

    fun unlock() {
        _locked.value = false
        backgroundedAt = null
    }

    fun lock() {
        _locked.value = true
    }

    /** Call right before starting an activity whose round trip must not lock the vault. */
    fun allowNextBackground() {
        backgroundAllowedAt = clock()
    }

    /** Activity `onStop`. Configuration changes (rotation, theme) don't count as leaving. */
    fun onBackground(isChangingConfigurations: Boolean) {
        if (isChangingConfigurations) return
        val allowedAt = backgroundAllowedAt
        backgroundAllowedAt = null
        // Only honor an exemption requested just before leaving; a stale one (the activity never
        // started) must not let a later, real departure skip the lock.
        if (allowedAt != null && clock() - allowedAt <= ALLOWANCE_WINDOW_MILLIS) return
        if (!_locked.value) backgroundedAt = clock()
    }

    /** Activity `onStart`: lock if the app was away long enough. */
    fun onForeground() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (clock() - since >= timeoutMillis()) lock()
    }

    companion object {
        /** Auto-lock choices shown in Settings, in seconds. 0 = immediately. */
        val TIMEOUT_OPTIONS_SECONDS = listOf(0, 30, 60, 300)
        const val DEFAULT_TIMEOUT_SECONDS = 30
        private const val ALLOWANCE_WINDOW_MILLIS = 5_000L
    }
}
