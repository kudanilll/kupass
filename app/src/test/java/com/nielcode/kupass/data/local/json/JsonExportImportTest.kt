package com.nielcode.kupass.data.local.json

import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.utils.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

@RunWith(RobolectricTestRunner::class)
class JsonExportImportTest {

    @Test
    fun `exportToJson outputs valid JSON array`() {
        // Arrange
        val passwords = listOf(
            PasswordEntity(id = 1, siteName = "Google", username = "user1", password = "pass1", url = "google.com", notes = "notes1", createdAt = 1000, updatedAt = 2000),
            PasswordEntity(id = 2, siteName = "Github", username = "user2", password = "pass2", url = "github.com", notes = "notes2", createdAt = 3000, updatedAt = 4000)
        )

        // Act
        val jsonString = JsonExportImport.exportToJson(passwords)

        // Assert
        val jsonElement = Json.parseToJsonElement(jsonString)
        assertTrue("Output should be a JSON array", jsonElement is kotlinx.serialization.json.JsonArray)
        assertEquals("Array should have 2 elements", 2, jsonElement.jsonArray.size)
    }

    @Test
    fun `exportToJson encrypts passwords`() {
        // Arrange
        val passwords = listOf(
            PasswordEntity(id = 1, siteName = "Google", password = "my_secret_password")
        )

        // Act
        val jsonString = JsonExportImport.exportToJson(passwords)

        // Assert
        assertTrue("JSON should contain siteName", jsonString.contains("Google"))
        // Password should be encrypted, not plaintext. If the fallback kicks in due to lack of keystore, it'll fail this test,
        // but we now provided a fallback Random Key in CryptoManager so this should pass!
        assertTrue("JSON should not contain plaintext password", !jsonString.contains("my_secret_password"))
        
        // Let's verify we can decrypt it back
        val imported = JsonExportImport.importFromJson(jsonString)
        assertEquals(1, imported.size)
        assertEquals("my_secret_password", imported[0].password)
    }

    @Test
    fun `importFromJson parses valid JSON into PasswordEntities`() {
        // Arrange
        val encryptedPass = CryptoManager.encrypt("pass123")
        val jsonString = """
            [
                {
                    "siteName": "Twitter",
                    "username": "bird_user",
                    "password": "$encryptedPass",
                    "url": "twitter.com",
                    "notes": "my notes",
                    "createdAt": 12345,
                    "updatedAt": 67890
                }
            ]
        """.trimIndent()

        // Act
        val imported = JsonExportImport.importFromJson(jsonString)

        // Assert
        assertEquals(1, imported.size)
        val entity = imported[0]
        
        // id should be reset to 0 for Room auto-generation
        assertEquals(0, entity.id)
        assertEquals("Twitter", entity.siteName)
        assertEquals("bird_user", entity.username)
        // Should be decrypted back to plaintext
        assertEquals("pass123", entity.password)
        assertEquals("twitter.com", entity.url)
        assertEquals("my notes", entity.notes)
        assertEquals(12345L, entity.createdAt)
        assertEquals(67890L, entity.updatedAt)
    }

    @Test
    fun `importFromJson handles legacy plaintext JSON`() {
        // Arrange - simulating a backup from an older version before encryption was added
        val jsonString = """
            [
                {
                    "siteName": "LegacySite",
                    "username": "legacy_user",
                    "password": "plaintext_password",
                    "url": "",
                    "notes": "",
                    "createdAt": 1000,
                    "updatedAt": 2000
                }
            ]
        """.trimIndent()

        // Act
        val imported = JsonExportImport.importFromJson(jsonString)

        // Assert
        assertEquals(1, imported.size)
        val entity = imported[0]
        
        // Our CryptoManager.decrypt fallback should handle this gracefully
        assertEquals("LegacySite", entity.siteName)
        assertEquals("plaintext_password", entity.password)
    }
}