package com.nielcode.kupass.data.local.json

import com.nielcode.kupass.data.local.db.PasswordEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializable data class for JSON export/import.
 * Maps to PasswordEntity but uses kotlinx.serialization.
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
 * Utility for exporting/importing passwords as JSON.
 * Uses kotlinx.serialization (already in the project).
 */
object JsonExportImport {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Export a list of PasswordEntity to a JSON string.
     */
    fun exportToJson(passwords: List<PasswordEntity>): String {
        val jsonList = passwords.map { entity ->
            PasswordJson(
                siteName = entity.siteName,
                username = entity.username,
                password = entity.password,
                url = entity.url,
                notes = entity.notes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        return json.encodeToString(jsonList)
    }

    /**
     * Import passwords from a JSON string.
     * Returns a list of PasswordEntity with id = 0 (Room will auto-generate).
     */
    fun importFromJson(jsonString: String): List<PasswordEntity> {
        val jsonList = json.decodeFromString<List<PasswordJson>>(jsonString)
        return jsonList.map { pw ->
            PasswordEntity(
                id = 0,
                siteName = pw.siteName,
                username = pw.username,
                password = pw.password,
                url = pw.url,
                notes = pw.notes,
                createdAt = pw.createdAt,
                updatedAt = pw.updatedAt
            )
        }
    }
}
