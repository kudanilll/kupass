package com.nielcode.kupass.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Background timeout for sensitive entry screens and export consent (no Keystore auth binding).
 *
 * Sensitive access starts locked in every new process and re-locks when the app has been in the
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

    /** The current absence started from one of our own screens (file picker, browser, …). */
    private var exemptTrip = false
    /**
     * When [allowNextBackground] was called; the exemption expires after [ALLOWANCE_WINDOW_MILLIS].
     */
    private var backgroundAllowedAt: Long? = null

    fun unlock() {
        _locked.value = false
        backgroundedAt = null
        exemptTrip = false
        backgroundAllowedAt = null
    }

    fun lock() {
        _locked.value = true
    }

    /** Call right before starting an activity whose round trip must not lock the vault. */
    fun allowNextBackground() {
        backgroundAllowedAt = clock()
    }

    /** Activity `onStop`. Configuration changes (rotation, theme) don't count as leaving. */
    fun onBackground(isChangingConfigurations: Boolean): Boolean {
        if (isChangingConfigurations) return false
        val allowedAt = backgroundAllowedAt
        backgroundAllowedAt = null
        return if (_locked.value) {
            false
        } else {
            backgroundedAt = clock()
            // A stale allowance must not soften a later, unrelated departure.
            exemptTrip = allowedAt != null && clock() - allowedAt <= ALLOWANCE_WINDOW_MILLIS
            exemptTrip
        }
    }

    /**
     * Activity `onStart`: lock if the app was away long enough. A trip to one of our own screens
     * gets a longer grace period (so picking a backup file doesn't lock mid-export), but is still
     * timed: a long absence that merely started there locks like any other.
     */
    fun onForeground() {
        checkTimeout()
        backgroundedAt = null
        exemptTrip = false
    }

    /** Also check before accepting callbacks that can arrive before Activity.onStart. */
    fun checkTimeout() {
        val since = backgroundedAt ?: return
        val timeout =
            if (exemptTrip) maxOf(timeoutMillis(), EXEMPT_TRIP_GRACE_MILLIS) else timeoutMillis()
        if (clock() - since >= timeout) lock()
    }

    companion object {
        /** Auto-lock choices shown in Settings, in seconds. 0 = immediately. */
        val TIMEOUT_OPTIONS_SECONDS = listOf(0, 30, 60, 300)
        const val DEFAULT_TIMEOUT_SECONDS = 30
        private const val ALLOWANCE_WINDOW_MILLIS = 5_000L

        /** Grace period for trips to our own file picker, browser, licenses or settings screens. */
        const val EXEMPT_TRIP_GRACE_MILLIS = 5 * 60_000L
    }
}
