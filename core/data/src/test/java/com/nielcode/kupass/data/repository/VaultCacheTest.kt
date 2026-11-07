package com.nielcode.kupass.data.repository

import com.nielcode.kupass.data.local.cache.VaultSnapshotStore
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.testing.FakePasswordDao
import java.io.File
import javax.crypto.KeyGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Keystore uses are counted through the key provider: one per encrypt or decrypt. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VaultCacheTest {

    @get:Rule val folder = TemporaryFolder()

    private val dao = FakePasswordDao()
    private lateinit var file: File
    private var keyUses = 0

    @Before
    fun setUp() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting {
            keyUses++
            key
        }
        file = File(folder.root, "vault-cache")
    }

    @Test
    fun `a new session reads the whole vault with one Keystore use`() = runTest {
        val first = repository(withSnapshot = true)
        listOf("Alpha", "Beta", "Gamma").forEach { first.insertPassword(entry(it)) }
        persistBy(first)

        keyUses = 0
        val names = repository(withSnapshot = true).getAllPasswords().first().map { it.siteName }

        assertEquals(listOf("Alpha", "Beta", "Gamma"), names)
        assertEquals(1, keyUses)
    }

    @Test
    fun `rows changed or deleted after the snapshot are never served from it`() = runTest {
        val first = repository(withSnapshot = true)
        val alpha = first.insertPassword(entry("Alpha", password = "old"))
        val beta = first.insertPassword(entry("Beta"))
        persistBy(first)

        // Not collected, so the snapshot still describes the old rows.
        first.updatePassword(entry("Alpha", password = "new").copy(id = alpha))
        first.deletePasswordById(beta)

        val vault = repository(withSnapshot = true).getAllPasswords().first()
        assertEquals(listOf("Alpha" to "new"), vault.map { it.siteName to it.password })
    }

    @Test
    fun `imported and saved rows are not decrypted again`() = runTest {
        val repository = repository(withSnapshot = false)
        repository.importPasswords((1..20).map { entry("Site $it") })
        repository.insertPassword(entry("Manual"))

        keyUses = 0
        val vault = repository.getAllPasswords().first()

        assertEquals(21, vault.size)
        assertEquals(0, keyUses)
    }

    @Test
    fun `the snapshot is rewritten only when the vault changes`() = runTest {
        val repository = repository(withSnapshot = true)
        repository.insertPassword(entry("Alpha"))
        persistBy(repository)
        assertTrue(file.exists())
        val written = file.lastModified()
        file.setLastModified(written - 60_000)

        persistBy(repository)

        assertEquals(written - 60_000, file.lastModified())
    }

    private fun TestScope.repository(withSnapshot: Boolean) =
        PasswordRepository(
            dao,
            cryptoDispatcher = UnconfinedTestDispatcher(testScheduler),
            snapshotStore = VaultSnapshotStore(file).takeIf { withSnapshot },
        )

    /** Collects the vault once, the way the list does, so the snapshot gets written. */
    private fun TestScope.persistBy(repository: PasswordRepository) {
        val collector = launch { repository.getAllPasswords().collect {} }
        advanceUntilIdle()
        collector.cancel()
    }

    private fun entry(siteName: String, password: String = "pw") =
        PasswordEntity(siteName = siteName, username = "user", password = password)
}
