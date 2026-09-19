package com.nielcode.kupass.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoManagerTest {

    private lateinit var key: SecretKey

    @Before
    fun setUp() {
        key = newKey()
        CryptoManager.setKeyProviderForTesting { key }
    }

    @Test
    fun `encrypt produces v2 format and decrypts back`() {
        val original = "super_secret_password_123!@#"

        val encrypted = CryptoManager.encrypt(original)

        assertTrue(encrypted.startsWith(CryptoManager.PREFIX_V2))
        assertFalse(encrypted.contains(original))
        assertEquals(original, CryptoManager.decrypt(encrypted))
    }

    @Test
    fun `encrypting the same value twice uses a fresh IV`() {
        assertNotEquals(CryptoManager.encrypt("same"), CryptoManager.encrypt("same"))
    }

    @Test
    fun `empty string stays empty but whitespace is encrypted`() {
        assertEquals("", CryptoManager.encrypt(""))
        assertEquals("", CryptoManager.decrypt(""))

        val encrypted = CryptoManager.encrypt("   ")
        assertTrue(encrypted.startsWith(CryptoManager.PREFIX_V2))
        assertEquals("   ", CryptoManager.decrypt(encrypted))
    }

    @Test
    fun `decrypts legacy v1 ciphertext without prefix`() {
        val legacy = encryptV1("legacy_value", key)

        assertEquals("legacy_value", CryptoManager.decrypt(legacy))
        assertFalse(CryptoManager.isCurrentFormat(legacy))
    }

    @Test
    fun `returns legacy plaintext that is not ciphertext`() {
        assertEquals("hunter2", CryptoManager.decrypt("hunter2"))
        assertEquals("plain text with spaces!", CryptoManager.decrypt("plain text with spaces!"))
    }

    @Test
    fun `tampered v2 ciphertext throws instead of returning data`() {
        val encrypted = CryptoManager.encrypt("secret")
        val bytes = Base64.decode(encrypted.removePrefix(CryptoManager.PREFIX_V2), Base64.NO_WRAP)
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()
        val tampered = CryptoManager.PREFIX_V2 + Base64.encodeToString(bytes, Base64.NO_WRAP)

        assertThrows(CryptoException::class.java) { CryptoManager.decrypt(tampered) }
    }

    @Test
    fun `v2 ciphertext from another key throws`() {
        val encrypted = CryptoManager.encrypt("secret")
        CryptoManager.setKeyProviderForTesting { newKey() }

        assertThrows(CryptoException::class.java) { CryptoManager.decrypt(encrypted) }
    }

    @Test
    fun `v1 ciphertext from another key throws instead of being treated as plaintext`() {
        val foreign = encryptV1("secret", newKey())

        assertThrows(CryptoException::class.java) { CryptoManager.decrypt(foreign) }
    }

    @Test
    fun `malformed v2 value throws`() {
        assertThrows(CryptoException::class.java) { CryptoManager.decrypt(CryptoManager.PREFIX_V2 + "***") }
        assertThrows(CryptoException::class.java) { CryptoManager.decrypt(CryptoManager.PREFIX_V2 + "AAAA") }
    }

    @Test
    fun `encrypt fails closed when the key is unavailable`() {
        CryptoManager.setKeyProviderForTesting { throw CryptoException("Vault key unavailable") }

        assertThrows(CryptoException::class.java) { CryptoManager.encrypt("secret") }
    }

    @Test
    fun `isCurrentFormat only accepts v2 or empty`() {
        assertTrue(CryptoManager.isCurrentFormat(""))
        assertTrue(CryptoManager.isCurrentFormat(CryptoManager.encrypt("x")))
        assertFalse(CryptoManager.isCurrentFormat("plaintext"))
    }

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    /** Reproduces the pre-v2 format: Base64(IV || ciphertext || tag) with no prefix. */
    private fun encryptV1(plaintext: String, key: SecretKey): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plaintext.toByteArray()), Base64.NO_WRAP)
    }
}
