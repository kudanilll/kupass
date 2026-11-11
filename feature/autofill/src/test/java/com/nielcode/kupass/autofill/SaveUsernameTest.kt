package com.nielcode.kupass.autofill

import org.junit.Assert.assertEquals
import org.junit.Test

class SaveUsernameTest {

    private fun form(
        username: String?,
        domain: String? = "accounts.example.com",
        pkg: String = "com.android.chrome",
    ) =
        ParsedForm(
            packageName = pkg,
            webDomain = domain,
            usernameField = username?.let { 0 },
            passwordFields = listOf(1),
            isSignUp = false,
            usernameValue = username,
            passwordValue = "secret",
        )

    @Test
    fun `a one-screen login keeps its own username`() {
        assertEquals("ana", saveUsername(listOf(form("ana"))))
    }

    @Test
    fun `a password-only screen takes the username typed on the screen before`() {
        assertEquals("ana", saveUsername(listOf(form("ana"), form(null))))
    }

    @Test
    fun `the newest earlier username wins`() {
        assertEquals("bo", saveUsername(listOf(form("ana"), form("bo"), form(null))))
    }

    @Test
    fun `an earlier screen of another site is ignored`() {
        assertEquals("", saveUsername(listOf(form("ana", domain = "evil.example"), form(null))))
    }

    @Test
    fun `an earlier screen of another app is ignored`() {
        assertEquals("", saveUsername(listOf(form("ana", pkg = "com.other.app"), form(null))))
    }

    @Test
    fun `a blank username on the last screen falls back to an earlier one`() {
        assertEquals("ana", saveUsername(listOf(form("ana"), form(" "))))
    }
}
