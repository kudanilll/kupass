package com.nielcode.kupass.data.local.json

import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.utils.CryptoManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializable data class for JSON export/import. Maps to PasswordEntity but uses
 * kotlinx.serialization.
 */
@Serializable
private data class PasswordJson(
    val siteName: String,
    val username: String = "",
    val password: String,
    val url: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Utility for exporting/importing passwords as JSON. Uses kotlinx.serialization (already in the
 * project).
 */
object JsonExportImport {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Export a list of PasswordEntity to a JSON string. */
    fun exportToJson(passwords: List<PasswordEntity>): String {
        val jsonList =
            passwords.map { entity ->
                PasswordJson(
                    siteName = entity.siteName,
                    username = entity.username,
                    // encrypt the password using the default Keystore key for export
                    // skipped: custom password-based PBKDF2 encryption; Keystore secures against
                    // simple plaintext leaks
                    // in standard backup workflows (Google Drive/etc that syncs raw files).
                    password = CryptoManager.encrypt(entity.password),
                    url = entity.url,
                    notes = entity.notes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        return json.encodeToString(jsonList)
    }

    /**
     * Import passwords from a JSON string. Returns a list of PasswordEntity with id = 0 (Room will
     * auto-generate).
     */
    fun importFromJson(jsonString: String): List<PasswordEntity> {
        val jsonList = json.decodeFromString<List<PasswordJson>>(jsonString)
        return jsonList.map { pw ->
            PasswordEntity(
                id = 0,
                siteName = pw.siteName,
                username = pw.username,
                // decrypt using Keystore key on import. Fallback to plaintext handles
                // legacy unencrypted backups smoothly.
                password = CryptoManager.decrypt(pw.password),
                url = pw.url,
                notes = pw.notes,
                createdAt = pw.createdAt,
                updatedAt = pw.updatedAt
            )
        }
    }
}
