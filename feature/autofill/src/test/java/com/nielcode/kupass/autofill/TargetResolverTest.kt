package com.nielcode.kupass.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetResolverTest {

    @Test
    fun `kupass itself gets nothing`() {
        assertNull(TargetResolver.resolve(OWN, webDomain = null, ownPackage = OWN))
        assertNull(TargetResolver.resolve(OWN, webDomain = "github.com", ownPackage = OWN))
    }

    @Test
    fun `browser with domain resolves to normalized web host`() {
        assertEquals(
            AutofillTarget.Web("github.com"),
            TargetResolver.resolve("com.android.chrome", "WWW.GitHub.com", OWN),
        )
    }

    @Test
    fun `browser without domain resolves to nothing`() {
        assertNull(
            TargetResolver.resolve("org.mozilla.firefox", webDomain = null, ownPackage = OWN)
        )
        assertNull(
            TargetResolver.resolve("org.mozilla.firefox", webDomain = "ftp://x", ownPackage = OWN)
        )
    }

    @Test
    fun `webview domain in a normal app is ignored`() {
        assertEquals(
            AutofillTarget.App("com.evil.app"),
            TargetResolver.resolve("com.evil.app", webDomain = "bank.com", ownPackage = OWN),
        )
    }

    @Test
    fun `allow-list holds the 13 known browsers`() {
        assertEquals(
            setOf(
                "com.android.chrome",
                "com.chrome.beta",
                "com.chrome.dev",
                "com.chrome.canary",
                "org.mozilla.firefox",
                "org.mozilla.firefox_beta",
                "org.mozilla.fenix",
                "com.sec.android.app.sbrowser",
                "com.microsoft.emmx",
                "com.brave.browser",
                "com.opera.browser",
                "com.vivaldi.browser",
                "com.duckduckgo.mobile.android",
            ),
            BROWSER_PACKAGES,
        )
    }

    private companion object {
        const val OWN = "com.nielcode.kupass"
    }
}
