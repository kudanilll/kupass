package com.nielcode.kupass.data.local.cache

import com.nielcode.kupass.security.CryptoManager
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
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
class VaultSnapshotStoreTest {

    @get:Rule val folder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var store: VaultSnapshotStore
    private val entries =
        mapOf("fp1" to DecryptedFields("SecretBank", "alice", "hunter2-secret", "", "PIN 9911"))

    @Before
    fun setUp() {
        useKey(newKey())
        file = File(folder.root, "vault-cache")
        store = VaultSnapshotStore(file)
    }

    @Test
    fun `round-trips entries`() {
        assertTrue(store.save(entries))
        assertEquals(entries, VaultSnapshotStore(file).load())
    }

    @Test
    fun `never writes plaintext to disk`() {
        store.save(entries)

        val raw = file.readText()
        listOf("SecretBank", "alice", "hunter2-secret", "PIN 9911", "fp1").forEach {
            assertFalse("plaintext on disk: $it", raw.contains(it))
        }
    }

    @Test
    fun `a missing file is an empty cache`() {
        assertEquals(emptyMap<String, DecryptedFields>(), store.load())
    }

    @Test
    fun `a snapshot sealed by another vault key is discarded`() {
        store.save(entries)
        useKey(newKey())

        assertEquals(emptyMap<String, DecryptedFields>(), store.load())
        assertFalse(file.exists())
    }

    @Test
    fun `a corrupted snapshot is discarded`() {
        store.save(entries)
        file.writeText(file.readText().replace("\"data\":\"", "\"data\":\"AAAA"))

        assertEquals(emptyMap<String, DecryptedFields>(), store.load())
        assertFalse(file.exists())
    }

    @Test
    fun `a wrapped key outside the current format is rejected`() {
        file.writeText("""{"version":1,"key":"cGxhaW4=","data":"AAAAAAAAAAAAAAAAAAAAAA=="}""")

        assertEquals(emptyMap<String, DecryptedFields>(), store.load())
        assertFalse(file.exists())
    }

    private fun newKey(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun useKey(key: SecretKey) = CryptoManager.setKeyProviderForTesting { key }
}
