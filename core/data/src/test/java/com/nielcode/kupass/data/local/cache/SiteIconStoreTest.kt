package com.nielcode.kupass.data.local.cache

import com.nielcode.kupass.security.CryptoManager
import java.io.File
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SiteIconStoreTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var file: File
    private val now = 100L * DAY

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        file = File(folder.root, "site-icons")
    }

    private fun store() = SiteIconStore(file, clock = { now })

    @Test
    fun `round-trips icons`() {
        assertTrue(store().save(mapOf("secretbank.example" to StoredIcon(byteArrayOf(1, 2), now))))

        val icon = store().load().getValue("secretbank.example")
        assertArrayEquals(byteArrayOf(1, 2), icon.png)
        assertEquals(now, icon.fetchedAt)
    }

    @Test
    fun `never writes domains in plaintext`() {
        store().save(mapOf("secretbank.example" to StoredIcon(byteArrayOf(1), now)))

        assertFalse(file.readText().contains("secretbank"))
    }

    @Test
    fun `icons older than 30 days are dropped`() {
        store()
            .save(
                mapOf(
                    "fresh.example" to StoredIcon(byteArrayOf(1), now - 29 * DAY),
                    "stale.example" to StoredIcon(byteArrayOf(2), now - 31 * DAY),
                )
            )

        assertEquals(setOf("fresh.example"), store().load().keys)
    }

    @Test
    fun `a corrupted file is discarded`() {
        file.writeText("not a sealed file")

        assertEquals(emptyMap<String, StoredIcon>(), store().load())
        assertFalse(file.exists())
    }

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
