package com.nielcode.kupass.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersionTest {

    @Test
    fun `release tags are read as their version`() {
        assertEquals(AppVersion(3, 1, 0), AppVersion.fromTag("v3.1.0"))
        assertEquals(AppVersion(3, 1, 0, beta = 2), AppVersion.fromTag("v3.1.0-beta.2"))
    }

    @Test
    fun `tags the release workflow refuses are not versions`() {
        listOf(
                "3.1.0",
                "v3.1",
                "v3.1.0-beta.0",
                "v3.1.0-beta.01",
                "v3.1.0-beta.99",
                "v3.1.0-rc.1",
                "v03.1.0",
                "v3.1.0 ",
                "v99999999999.0.0",
            )
            .forEach { assertNull(it, AppVersion.fromTag(it)) }
    }

    @Test
    fun `the installed version is versionName plus the beta number`() {
        assertEquals(AppVersion(3, 1, 0, beta = 2), AppVersion.installed("3.1.0", 2))
        assertEquals(AppVersion(3, 1, 0), AppVersion.installed("3.1.0", 0))
        assertNull(AppVersion.installed("3.1", 0))
    }

    @Test
    fun `versions sort in release order, each stable release after its betas`() {
        val ordered =
            listOf("v3.1.0-beta.1", "v3.1.0-beta.2", "v3.1.0-beta.10", "v3.1.0", "v3.1.1-beta.1")
                .map { requireNotNull(AppVersion.fromTag(it)) }

        assertEquals(ordered, ordered.shuffled(java.util.Random(1)).sorted())
        assertEquals(ordered, ordered.reversed().sorted())
    }
}
