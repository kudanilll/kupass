package com.nielcode.kupass.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VaultUrlsTest {

    @Test
    fun `web host normalizes case port path trailing dot and www`() {
        assertEquals("github.com", webHostOf("https://www.GitHub.com:443/login"))
        assertEquals("github.com", webHostOf("HTTP://github.com."))
        assertEquals("github.com", webHostOf("github.com"))
        assertEquals("github.com", webHostOf("WWW.GitHub.com"))
        assertNull(webHostOf("ftp://github.com"))
        assertNull(webHostOf(""))
    }

    @Test
    fun `android package accepts plain and google facet forms`() {
        assertEquals("com.instagram.android", androidPackageOf("android://com.instagram.android"))
        assertEquals(
            "com.instagram.android",
            androidPackageOf("android://AbC1-_=@com.instagram.android/"),
        )
        assertEquals("com.a.b", androidPackageOf("ANDROID://com.a.b?x#y"))
        assertNull(androidPackageOf("android://"))
        assertNull(androidPackageOf("android://not a package"))
        assertNull(androidPackageOf("https://com.a.b"))
        assertEquals("android://com.a.b", androidUrlOf("com.a.b"))
    }
}
