package com.nielcode.kupass.data.siteicon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SiteDomainTest {

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
    fun `ignores site names that are not domains`() {
        assertNull(siteDomainOf("", "My Bank"))
        assertNull(siteDomainOf("", "Netflix"))
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
