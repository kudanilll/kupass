package com.nielcode.kupass.data.siteicon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiteDomainTest {

    @Test
    fun `app entries use their site name`() {
        assertEquals(
            "instagram.com",
            siteDomainOf("android://cert@com.instagram.android/", "Instagram"),
        )
        assertEquals("google.com", siteDomainOf(" ANDROID://cert@com.example.app/", "google.com"))
    }

    @Test
    fun `app entries named after their package guess the domain from it`() {
        val facet = "android://cert@com.instagram.android/"
        assertEquals("instagram.com", siteDomainOf(facet, "com.instagram.android"))
        assertEquals("instagram.com", siteDomainOf(facet, ""))
        assertEquals("instagram.com", siteDomainOf(facet, "My Instagram"))
        assertEquals("dana.id", siteDomainOf("android://cert@id.dana/", ""))
        assertEquals("bri.co.id", siteDomainOf("android://cert@id.co.bri.brimo/", ""))
        assertEquals("example.org", siteDomainOf("android://cert@org.example.app/", ""))
    }

    @Test
    fun `app entries never send a package name or guess from unusable ones`() {
        assertNull(siteDomainOf("android://malformed", "Google"))
        assertNull(siteDomainOf("android://cert@android.app.settings/", ""))
        assertNull(siteDomainOf("android://cert@local.corp.app/", ""))
        assertNull(siteDomainOf("", "android://cert@com.example.app/"))
    }

    @Test
    fun `keeps only the bare hostname of a url`() {
        assertEquals(
            "github.com",
            siteDomainOf("https://user:secret@WWW.GitHub.com:443/login?next=/x#top", ""),
        )
    }

    @Test
    fun `accepts a url without a scheme`() {
        assertEquals("accounts.google.com", siteDomainOf("accounts.google.com/signin", ""))
    }

    @Test
    fun `falls back to a site name that looks like a domain`() {
        assertEquals("netflix.com", siteDomainOf("", "Netflix.com"))
    }

    @Test
    fun `guesses dot com for a single-word site name`() {
        assertEquals("netflix.com", siteDomainOf("", "Netflix"))
        assertEquals("github.com", siteDomainOf("", "GitHub"))
    }

    @Test
    fun `does not guess domains for ambiguous or private site names`() {
        assertNull(siteDomainOf("", "My Bank"))
        assertNull(siteDomainOf("", "localhost"))
        assertNull(siteDomainOf("", "internal"))
    }

    @Test
    fun `prefers the url over the site name`() {
        assertEquals("example.org", siteDomainOf("example.org", "other.com"))
    }

    @Test
    fun `never sends ip addresses or private names`() {
        assertNull(siteDomainOf("http://192.168.1.1/admin", ""))
        assertNull(siteDomainOf("http://[::1]:8080", ""))
        assertNull(siteDomainOf("http://localhost:3000", ""))
        assertNull(siteDomainOf("https://nas.local", ""))
        assertNull(siteDomainOf("https://router.home.arpa", ""))
        assertNull(siteDomainOf("https://wiki.corp", ""))
    }

    @Test
    fun `rejects blank and malformed input`() {
        assertNull(siteDomainOf("", ""))
        assertNull(siteDomainOf("   ", " "))
        assertNull(siteDomainOf("http://exa mple.com", ""))
        assertNull(siteDomainOf("not a url", ""))
    }
}
