package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.testing.FakePasswordDao
import com.nielcode.kupass.utils.CryptoManager
import javax.crypto.KeyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PasswordRepositoryTest {

    private lateinit var dao: FakePasswordDao
    private lateinit var repository: PasswordRepository

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        dao = FakePasswordDao()
        repository = PasswordRepository(dao, cryptoDispatcher = UnconfinedTestDispatcher())
    }

    @Test
    fun `every text field is encrypted at rest`() = runTest {
        repository.insertPassword(entry("Google", "alice", "pw", "google.com", "recovery 1234"))

        val row = dao.stored.single()
        listOf(row.siteName, row.username, row.password, row.url, row.notes).forEach {
            assertTrue("not encrypted: $it", it.startsWith(CryptoManager.PREFIX_V2))
        }
        assertEquals(entry("Google", "alice", "pw", "google.com", "recovery 1234"), repository.getAllPasswords().first().single().copy(id = 0))
    }

    @Test
    fun `list is sorted case-insensitively by site name`() = runTest {
        listOf("zeta", "Alpha", "beta").forEach { repository.insertPassword(entry(it)) }

        assertEquals(listOf("Alpha", "beta", "zeta"), repository.getAllPasswords().first().map { it.siteName })
    }

    @Test
    fun `search matches decrypted site, username, url and notes case-insensitively`() = runTest {
        repository.insertPassword(entry("Google", username = "alice"))
        repository.insertPassword(entry("Bank", url = "mybank.example"))
        repository.insertPassword(entry("Router", notes = "Admin PIN in drawer"))

        assertEquals(listOf("Google"), repository.searchPasswords("ALI").first().map { it.siteName })
        assertEquals(listOf("Bank"), repository.searchPasswords("mybank").first().map { it.siteName })
        assertEquals(listOf("Router"), repository.searchPasswords("drawer").first().map { it.siteName })
        assertEquals(3, repository.searchPasswords("  ").first().size)
    }

    @Test
    fun `upgrade re-encrypts legacy rows and is idempotent`() = runTest {
        // A row written before full-field encryption: plaintext metadata, v2 password.
        dao.insert(PasswordEntity(siteName = "Legacy", username = "bob", password = CryptoManager.encrypt("pw"), url = "x.com", notes = "n"))

        assertEquals(1, repository.upgradeStoredFormat())
        assertEquals(0, repository.upgradeStoredFormat())

        val row = dao.stored.single()
        assertTrue(listOf(row.siteName, row.username, row.url, row.notes).all { it.startsWith(CryptoManager.PREFIX_V2) })
        assertEquals("Legacy", repository.getAllPasswords().first().single().siteName)
        assertEquals("pw", repository.getAllPasswords().first().single().password)
    }

    @Test
    fun `upgrade leaves undecryptable rows untouched`() = runTest {
        val otherKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val foreign = run {
            CryptoManager.setKeyProviderForTesting { otherKey }
            CryptoManager.encrypt("secret")
        }
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        dao.insert(PasswordEntity(siteName = "Plain", password = foreign))

        assertEquals(0, repository.upgradeStoredFormat())
        assertEquals("Plain", dao.stored.single().siteName)
        assertFalse(dao.stored.single().siteName.startsWith(CryptoManager.PREFIX_V2))
    }

    private fun entry(site: String, username: String = "", password: String = "pw", url: String = "", notes: String = "") =
        PasswordEntity(siteName = site, username = username, password = password, url = url, notes = notes, createdAt = 1, updatedAt = 2)
}
