package com.nielcode.kupass.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nielcode.kupass.data.local.db.KupassDatabase
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.CryptoManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upgrading a vault written by earlier builds, on a real device with the real Keystore key:
 * plaintext metadata + v1 password ciphertext → every field in v2.
 */
@RunWith(AndroidJUnit4::class)
class PasswordRepositoryUpgradeTest {

    private lateinit var db: KupassDatabase
    private lateinit var repository: PasswordRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    KupassDatabase::class.java,
                )
                .build()
        repository = PasswordRepository(db.passwordDao())
    }

    @After fun tearDown() = db.close()

    @Test
    fun upgradesLegacyRowsToFullFieldEncryption() = runBlocking {
        // v1 ciphertext is the v2 payload without the "kp2:" prefix.
        val v1Password = CryptoManager.encrypt("hunter22").removePrefix(CryptoManager.PREFIX_V2)
        db.passwordDao()
            .insert(
                PasswordEntity(
                    siteName = "Legacy Bank",
                    username = "alice",
                    password = v1Password,
                    url = "bank.example",
                    notes = "PIN in drawer",
                )
            )

        // Readable before the upgrade (legacy read path).
        assertEquals("hunter22", repository.getAllPasswords().first().single().password)

        assertEquals(1, repository.upgradeStoredFormat())
        assertEquals(0, repository.upgradeStoredFormat())

        val raw = db.passwordDao().getAllOnce().single()
        listOf(raw.siteName, raw.username, raw.password, raw.url, raw.notes).forEach {
            assertTrue("not upgraded: $it", it.startsWith(CryptoManager.PREFIX_V2))
        }
        val restored = repository.getAllPasswords().first().single()
        assertEquals("Legacy Bank", restored.siteName)
        assertEquals("alice", restored.username)
        assertEquals("hunter22", restored.password)
        assertEquals("bank.example", restored.url)
        assertEquals("PIN in drawer", restored.notes)
        assertEquals(
            listOf("Legacy Bank"),
            repository.searchPasswords("drawer").first().map { it.siteName },
        )
    }
}
