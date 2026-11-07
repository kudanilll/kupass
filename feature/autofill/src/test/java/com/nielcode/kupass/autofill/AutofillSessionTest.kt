package com.nielcode.kupass.autofill

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSessionTest {

    private var now = 0L
    private var timeout = 30_000L
    private val session = AutofillSession(timeoutMillis = { timeout }, clock = { now })

    @Test
    fun `new session is locked`() {
        assertFalse(session.isActive())
    }

    @Test
    fun `timeout zero never unlocks`() {
        timeout = 0
        session.start()
        assertFalse(session.isActive())
    }

    @Test
    fun `active until the timeout then locked`() {
        session.start()
        now = 29_999
        assertTrue(session.isActive())
        now = 30_000
        assertFalse(session.isActive())
    }

    @Test
    fun `touch extends while active`() {
        session.start()
        now = 20_000
        session.touch()
        now = 49_999
        assertTrue(session.isActive())
        now = 50_000
        assertFalse(session.isActive())
    }

    @Test
    fun `touch after expiry does not revive`() {
        session.start()
        now = 30_000
        session.touch()
        now = 30_001
        assertFalse(session.isActive())
    }

    @Test
    fun `lowering the timeout ends the session`() {
        timeout = 300_000
        session.start()
        now = 10_000
        assertTrue(session.isActive())
        timeout = 0
        assertFalse(session.isActive())
        timeout = 300_000
        assertFalse(session.isActive())
    }

    @Test
    fun `end locks immediately`() {
        session.start()
        session.end()
        assertFalse(session.isActive())
    }
}
