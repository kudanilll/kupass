package com.nielcode.kupass.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {

    private var now = 1_000L
    private var timeout = 30_000L
    private val lock = AppLock(timeoutMillis = { timeout }, clock = { now })

    @Test
    fun `starts locked`() {
        assertTrue(lock.locked.value)
    }

    @Test
    fun `stays unlocked when back before the timeout`() {
        lock.unlock()
        lock.onBackground(isChangingConfigurations = false)
        now += 29_999
        lock.onForeground()

        assertFalse(lock.locked.value)
    }

    @Test
    fun `locks when away for the timeout`() {
        lock.unlock()
        lock.onBackground(isChangingConfigurations = false)
        now += 30_000
        lock.onForeground()

        assertTrue(lock.locked.value)
    }

    @Test
    fun `immediate timeout locks on any return`() {
        timeout = 0
        lock.unlock()
        lock.onBackground(isChangingConfigurations = false)
        lock.onForeground()

        assertTrue(lock.locked.value)
    }

    @Test
    fun `rotation never locks`() {
        timeout = 0
        lock.unlock()
        lock.onBackground(isChangingConfigurations = true)
        now += 60_000
        lock.onForeground()

        assertFalse(lock.locked.value)
    }

    @Test
    fun `an exempted round trip such as the file picker does not lock within the grace period`() {
        timeout = 0
        lock.unlock()
        lock.allowNextBackground()
        lock.onBackground(isChangingConfigurations = false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS - 1
        lock.onForeground()

        assertFalse(lock.locked.value)
    }

    @Test
    fun `a long absence that started with an exempted trip still locks`() {
        // Security review finding: the exemption used to skip timing entirely.
        timeout = 0
        lock.unlock()
        lock.allowNextBackground() // e.g. GitHub link opens the browser
        lock.onBackground(isChangingConfigurations = false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS
        lock.onForeground() // back from Recents much later

        assertTrue(lock.locked.value)
    }

    @Test
    fun `the exempt grace never shortens a longer user timeout`() {
        timeout = AppLock.EXEMPT_TRIP_GRACE_MILLIS * 2
        lock.unlock()
        lock.allowNextBackground()
        lock.onBackground(isChangingConfigurations = false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS + 1
        lock.onForeground()

        assertFalse(lock.locked.value)
    }

    @Test
    fun `the exemption is used once`() {
        timeout = 0
        lock.unlock()
        lock.allowNextBackground()
        lock.onBackground(isChangingConfigurations = false)
        lock.onForeground()
        lock.onBackground(isChangingConfigurations = false)
        lock.onForeground()

        assertTrue(lock.locked.value)
    }

    @Test
    fun `a stale exemption does not skip a later real departure`() {
        timeout = 0
        lock.unlock()
        lock.allowNextBackground() // e.g. the activity failed to start
        now += 60_000
        lock.onBackground(isChangingConfigurations = false)
        lock.onForeground()

        assertTrue(lock.locked.value)
    }

    @Test
    fun `leaving while locked does not affect unlocking`() {
        timeout = 0
        lock.onBackground(isChangingConfigurations = false) // device credential screen
        lock.unlock()
        lock.onForeground()

        assertFalse(lock.locked.value)
    }
}
