package com.nielcode.kupass.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CryptoManagerTest {

    @Test
    fun `encrypt and decrypt string successfully`() {
        // Arrange
        val originalText = "super_secret_password_123!@#"

        // Act
        val encryptedText = CryptoManager.encrypt(originalText)
        val decryptedText = CryptoManager.decrypt(encryptedText)

        // Assert
        assertTrue("Encrypted text should not equal original text", originalText != encryptedText)
        assertEquals("Decrypted text should equal original text", originalText, decryptedText)
    }

    @Test
    fun `encrypting blank string returns blank string`() {
        assertEquals("", CryptoManager.encrypt(""))
        assertEquals("   ", CryptoManager.encrypt("   "))
    }

    @Test
    fun `decrypting blank string returns blank string`() {
        assertEquals("", CryptoManager.decrypt(""))
        assertEquals("   ", CryptoManager.decrypt("   "))
    }

    @Test
    fun `decrypting invalid base64 or plaintext falls back to original input`() {
        // Arrange
        val legacyPlaintext = "just_a_normal_string_not_encrypted"
        
        // Act
        val decryptedText = CryptoManager.decrypt(legacyPlaintext)
        
        // Assert
        assertEquals("Should fall back to plaintext on decrypt failure", legacyPlaintext, decryptedText)
    }
}