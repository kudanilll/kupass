package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.local.db.PasswordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialsTest {

    private val instagram = AutofillTarget.App("com.instagram.android")
    private val github = AutofillTarget.Web("github.com")

    @Test
    fun `app matches both android url forms`() {
        val google = entry(1, url = "android://AbC=@com.instagram.android/")
        val kupass = entry(2, url = "android://com.instagram.android")
        val other = entry(3, url = "android://com.instagram.lite")
        assertEquals(
            listOf(google, kupass),
            CredentialMatcher.matches(listOf(google, kupass, other), instagram),
        )
    }

    @Test
    fun `web matches messy stored urls but not other schemes`() {
        val entries =
            listOf(
                entry(1, url = "https://www.GitHub.com:443/login"),
                entry(2, url = "HTTP://github.com."),
                entry(3, url = "github.com"),
                entry(4, url = "ftp://github.com"),
                entry(5, url = "https://gist.github.com"),
            )
        assertEquals(listOf(1L, 2L, 3L), CredentialMatcher.matches(entries, github).map { it.id })
    }

    @Test
    fun `site name alone never matches`() {
        val entries =
            listOf(entry(1, url = "", site = "github.com"), entry(2, url = "", site = "Instagram"))
        assertTrue(CredentialMatcher.matches(entries, github).isEmpty())
        assertTrue(CredentialMatcher.matches(entries, instagram).isEmpty())
    }

    @Test
    fun `new app entry uses label then package`() {
        val labelled =
            newEntry(instagram, appLabel = "Instagram", username = "dan", password = "pw")
        assertEquals("Instagram", labelled.siteName)
        assertEquals("android://com.instagram.android", labelled.url)
        assertEquals("dan", labelled.username)
        assertEquals("pw", labelled.password)
        assertEquals("", labelled.notes)
        assertEquals("com.instagram.android", newEntry(instagram, " ", "dan", "pw").siteName)
    }

    @Test
    fun `new web entry uses host and https url`() {
        val created = newEntry(github, appLabel = null, username = "dan", password = "pw")
        assertEquals("github.com", created.siteName)
        assertEquals("https://github.com", created.url)
    }

    @Test
    fun `empty password at submit changes nothing`() {
        val known = entry(1, url = "https://github.com", username = "", password = "pw")
        assertEquals(SaveDecision.None, resolveSave(listOf(known), github, "dan", "", null))
        assertEquals(SaveDecision.None, resolveSave(emptyList(), github, "dan", "", null))
    }

    @Test
    fun `same password with new username updates username`() {
        val known = entry(1, url = "https://github.com", username = "", password = "Gen-1")
        val decision = resolveSave(listOf(known), github, "dan", "Gen-1", null)
        assertTrue(decision is SaveDecision.Update)
        val updated = (decision as SaveDecision.Update).entry
        assertEquals(1L, updated.id)
        assertEquals("dan", updated.username)
        assertEquals("Gen-1", updated.password)
    }

    @Test
    fun `same password and blank or equal username keeps entry`() {
        val known = entry(1, url = "https://github.com", username = "dan", password = "Gen-1")
        assertEquals(SaveDecision.None, resolveSave(listOf(known), github, " ", "Gen-1", null))
        assertEquals(SaveDecision.None, resolveSave(listOf(known), github, "dan", "Gen-1", null))
    }

    @Test
    fun `same password under another saved username inserts instead of renaming`() {
        val alice = entry(1, url = "https://github.com", username = "alice", password = "shared")
        val decision = resolveSave(listOf(alice), github, "bob", "shared", null)
        assertTrue(decision is SaveDecision.Insert)
        assertEquals("bob", (decision as SaveDecision.Insert).entry.username)
    }

    @Test
    fun `same password fills in the entry that has no username yet`() {
        val alice =
            entry(1, url = "https://github.com", username = "alice", password = "Gen-1")
                .copy(updatedAt = 2)
        val generated =
            entry(2, url = "https://github.com", username = "", password = "Gen-1")
                .copy(updatedAt = 1)
        val decision = resolveSave(listOf(alice, generated), github, "bob", "Gen-1", null)
        assertTrue(decision is SaveDecision.Update)
        assertEquals(2L, (decision as SaveDecision.Update).entry.id)
        assertEquals("bob", decision.entry.username)
    }

    @Test
    fun `an account already saved under that username is left alone`() {
        val alice =
            entry(1, url = "https://github.com", username = "alice", password = "Gen-1")
                .copy(updatedAt = 1)
        val generated =
            entry(2, url = "https://github.com", username = "", password = "Gen-1")
                .copy(updatedAt = 2)
        assertEquals(
            SaveDecision.None,
            resolveSave(listOf(alice, generated), github, "alice", "Gen-1", null),
        )
    }

    @Test
    fun `unknown credentials insert`() {
        val known = entry(1, url = "https://github.com", username = "dan", password = "old")
        val decision = resolveSave(listOf(known), github, "dan", "new", null)
        assertTrue(decision is SaveDecision.Insert)
        val inserted = (decision as SaveDecision.Insert).entry
        assertEquals(0L, inserted.id)
        assertEquals("https://github.com", inserted.url)
        assertEquals("new", inserted.password)
    }

    @Test
    fun `save info rules`() {
        val signUp = form(isSignUp = true)
        val login = form(isSignUp = false)
        assertTrue(shouldAttachSaveInfo(signUp, hasEntryForTarget = true, offerToSave = false))
        assertTrue(shouldAttachSaveInfo(login, hasEntryForTarget = false, offerToSave = true))
        assertFalse(shouldAttachSaveInfo(login, hasEntryForTarget = true, offerToSave = true))
        assertFalse(shouldAttachSaveInfo(login, hasEntryForTarget = false, offerToSave = false))
        assertFalse(
            shouldAttachSaveInfo(
                login.copy(passwordFields = emptyList()),
                hasEntryForTarget = false,
                offerToSave = true,
            )
        )
    }

    private fun entry(
        id: Long,
        url: String,
        site: String = "Site",
        username: String = "user",
        password: String = "secret",
    ) =
        PasswordEntity(
            id = id,
            siteName = site,
            username = username,
            password = password,
            url = url,
        )

    private fun form(isSignUp: Boolean) =
        ParsedForm(
            packageName = "com.android.chrome",
            webDomain = "github.com",
            usernameField = 0,
            passwordFields = listOf(1),
            isSignUp = isSignUp,
            usernameValue = null,
            passwordValue = null,
        )
}
