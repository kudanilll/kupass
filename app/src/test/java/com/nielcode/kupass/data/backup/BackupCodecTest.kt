package com.nielcode.kupass.data.backup

import android.util.Base64
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.CryptoManager
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupCodecTest {

    private lateinit var deviceKey: SecretKey

    private val entries =
        listOf(
            PasswordEntity(
                id = 7,
                siteName = "Google",
                username = "alice",
                password = "p@ss-1",
                url = "google.com",
                notes = "recovery: 1234",
                createdAt = 1000,
                updatedAt = 2000,
            ),
            PasswordEntity(
                id = 9,
                siteName = "GitHub",
                username = "",
                password = "p@ss-2",
                url = "",
                notes = "",
                createdAt = 3000,
                updatedAt = 4000,
            ),
        )

    @Before
    fun setUp() {
        deviceKey = newKey()
        CryptoManager.setKeyProviderForTesting { deviceKey }
    }

    // --- v2 (portable) ---

    @Test
    fun `v2 round trip restores every field with a fresh id`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)

        val restored = BackupCodec.decode(backup, PASSWORD.toCharArray())

        assertEquals(entries.map { it.copy(id = 0) }, restored)
    }

    @Test
    fun `v2 restores on another device with a different Keystore key`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)
        CryptoManager.setKeyProviderForTesting { newKey() } // simulate a new phone / reinstall

        assertEquals(2, BackupCodec.decode(backup, PASSWORD.toCharArray()).size)
    }

    @Test
    fun `v2 file contains no plaintext vault data`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)

        listOf("Google", "alice", "p@ss-1", "google.com", "recovery").forEach {
            assertFalse("leaked $it", backup.contains(it))
        }
        assertTrue(BackupCodec.isPasswordProtected(backup))
    }

    @Test
    fun `v2 with wrong password throws WrongPassword`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)

        assertThrows(BackupException.WrongPassword::class.java) {
            BackupCodec.decode(backup, "not-the-password".toCharArray())
        }
    }

    @Test
    fun `v2 without password throws PasswordRequired`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)

        assertThrows(BackupException.PasswordRequired::class.java) {
            BackupCodec.decode(backup, null)
        }
    }

    @Test
    fun `v2 with tampered header is rejected`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)
        val root = Json.parseToJsonElement(backup).jsonObject
        val header = root.getValue("header").jsonObject
        val kdf = header.getValue("kdf").jsonObject
        val tamperedKdf = JsonObject(kdf + ("iterations" to JsonPrimitive(ITERATIONS + 1)))
        val tampered =
            JsonObject(root + ("header" to JsonObject(header + ("kdf" to tamperedKdf)))).toString()

        assertThrows(BackupException.WrongPassword::class.java) {
            BackupCodec.decode(tampered, PASSWORD.toCharArray())
        }
    }

    @Test
    fun `v2 from a future version is unsupported`() {
        val backup = BackupCodec.encode(entries, PASSWORD.toCharArray(), ITERATIONS)
        val root = Json.parseToJsonElement(backup).jsonObject
        val header =
            JsonObject(root.getValue("header").jsonObject + ("version" to JsonPrimitive(99)))
        val future = JsonObject(root + ("header" to header)).toString()

        assertThrows(BackupException.Unsupported::class.java) {
            BackupCodec.decode(future, PASSWORD.toCharArray())
        }
    }

    @Test
    fun `encode rejects short passwords`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.encode(entries, "short".toCharArray(), ITERATIONS)
        }
    }

    // --- legacy v1 ---

    @Test
    fun `legacy backup from this device imports`() {
        val legacy = legacyJson(password = CryptoManager.encrypt("secret"))

        val restored = BackupCodec.decode(legacy, password = null)

        assertEquals("secret", restored.single().password)
        assertFalse(BackupCodec.isPasswordProtected(legacy))
    }

    @Test
    fun `legacy plaintext backup imports`() {
        assertEquals(
            "plaintext_password",
            BackupCodec.decode(legacyJson("plaintext_password"), null).single().password,
        )
    }

    @Test
    fun `legacy backup from another device is rejected, never imported as ciphertext`() {
        // Regression test for CONCERNS C-1.
        val foreignV1 = encryptV1("secret", newKey())
        val foreignV2 = run {
            CryptoManager.setKeyProviderForTesting { newKey() }
            CryptoManager.encrypt("secret").also {
                CryptoManager.setKeyProviderForTesting { deviceKey }
            }
        }

        assertThrows(BackupException.ForeignDevice::class.java) {
            BackupCodec.decode(legacyJson(foreignV1), null)
        }
        assertThrows(BackupException.ForeignDevice::class.java) {
            BackupCodec.decode(legacyJson(foreignV2), null)
        }
    }

    @Test
    fun `garbage input is malformed`() {
        assertThrows(BackupException.Malformed::class.java) { BackupCodec.decode("not json", null) }
        assertThrows(BackupException.Malformed::class.java) { BackupCodec.decode("42", null) }
        assertFalse(BackupCodec.isPasswordProtected("not json"))
    }

    @Test
    fun `too many legacy entries is rejected`() {
        val many =
            (0..BackupCodec.MAX_ENTRIES).joinToString(",", "[", "]") {
                """{"siteName":"s","password":"p"}"""
            }

        assertThrows(BackupException.TooLarge::class.java) { BackupCodec.decode(many, null) }
    }

    private fun legacyJson(password: String) =
        """[{"siteName":"Twitter","username":"bird","password":"$password","url":"twitter.com","notes":"n","createdAt":1,"updatedAt":2}]"""

    private fun newKey(): SecretKey =
        KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun encryptV1(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return Base64.encodeToString(
            cipher.iv + cipher.doFinal(plaintext.toByteArray()),
            Base64.NO_WRAP,
        )
    }

    private companion object {
        const val PASSWORD = "correct horse battery"
        const val ITERATIONS = 10_000
    }
}
