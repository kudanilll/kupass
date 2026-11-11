package com.nielcode.kupass.buildlogic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {

    @Test
    fun `a stable tag gets the stable versionCode and no beta number`() {
        assertEquals(
            ReleaseVersion(versionCode = 3_010_099, beta = 0),
            releaseVersion("v3.1.0", "3.1.0"),
        )
    }

    @Test
    fun `a beta tag gets its beta number in the versionCode`() {
        assertEquals(
            ReleaseVersion(versionCode = 3_010_002, beta = 2),
            releaseVersion("v3.1.0-beta.2", "3.1.0"),
        )
    }

    @Test
    fun `each release updates the one before it`() {
        val releases =
            listOf(
                "v3.1.0-beta.1" to "3.1.0",
                "v3.1.0-beta.2" to "3.1.0",
                "v3.1.0-beta.98" to "3.1.0",
                "v3.1.0" to "3.1.0",
                "v3.1.1-beta.1" to "3.1.1",
                "v3.1.1" to "3.1.1",
                "v3.2.0-beta.1" to "3.2.0",
                "v4.0.0" to "4.0.0",
            )

        val codes = releases.map { (tag, name) -> releaseVersion(tag, name).versionCode }

        assertEquals(codes.sorted().distinct(), codes)
        // Above every versionCode published before tags set it (v2.0.0 had 2, local builds have 5).
        assertTrue(codes.first() > 5)
    }

    @Test
    fun `a tag that does not match versionName is refused`() {
        assertThrows(IllegalArgumentException::class.java) { releaseVersion("v3.2.0", "3.1.0") }
        assertThrows(IllegalArgumentException::class.java) {
            releaseVersion("v3.2.0-beta.1", "3.1.0")
        }
    }

    @Test
    fun `malformed tags are refused`() {
        listOf(
                "3.1.0",
                "v3.1",
                "v3.1.0-",
                "v3.1.0-beta",
                "v3.1.0-beta.",
                "v3.1.0-beta.0",
                "v3.1.0-beta.01",
                "v3.1.0-beta.1.1",
                "v3.1.0-rc.1",
                "v3.1.0 ",
                "refs/tags/v3.1.0",
            )
            .forEach { tag ->
                assertThrows(tag, IllegalArgumentException::class.java) {
                    releaseVersion(tag, "3.1.0")
                }
            }
    }

    @Test
    fun `beta numbers stop at 98 so every beta stays below the stable release`() {
        assertEquals(3_010_098, releaseVersion("v3.1.0-beta.98", "3.1.0").versionCode)
        assertThrows(IllegalArgumentException::class.java) {
            releaseVersion("v3.1.0-beta.99", "3.1.0")
        }
        assertThrows(IllegalArgumentException::class.java) {
            releaseVersion("v3.1.0-beta.100", "3.1.0")
        }
    }

    @Test
    fun `versions that do not fit the versionCode are refused`() {
        assertEquals(2_099_999_999, releaseVersion("v2099.99.99", "2099.99.99").versionCode)
        listOf("3.100.0", "3.1.100", "2100.0.0").forEach { name ->
            assertThrows(name, IllegalArgumentException::class.java) {
                releaseVersion("v$name", name)
            }
        }
    }
}
