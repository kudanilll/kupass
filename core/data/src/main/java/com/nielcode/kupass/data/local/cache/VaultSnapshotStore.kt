package com.nielcode.kupass.data.local.cache

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Plaintext of an entry's five encrypted text fields. */
@Serializable
data class DecryptedFields(
    val siteName: String,
    val username: String,
    val password: String,
    val url: String,
    val notes: String,
)

/**
 * The decrypted vault kept on disk between launches, so the list opens without sending every field
 * through the Keystore again.
 *
 * It is a [SealedFile], so it is never stored in plaintext and a load costs one Keystore operation.
 * The file lives in `noBackupFilesDir` (the backup rules exclude it as well). It is only a cache:
 * entries are keyed by row fingerprints, so the database still decides what is shown.
 */
class VaultSnapshotStore(file: File) {
    private val sealed = SealedFile(file, "kupass-vault-cache-v1")

    /**
     * Cached fields by row fingerprint; empty when missing, unreadable, or sealed by another key.
     */
    fun load(): Map<String, DecryptedFields> {
        val text = sealed.read() ?: return emptyMap()
        val entries = readableOrNull { Json.decodeFromString<Snapshot>(text).entries }
        if (entries == null) sealed.delete()
        return entries.orEmpty()
    }

    /** Replaces the cache with [entries]. Returns false if it couldn't be written. */
    fun save(entries: Map<String, DecryptedFields>): Boolean =
        sealed.write(Json.encodeToString(Snapshot(entries)))

    @Serializable private class Snapshot(val entries: Map<String, DecryptedFields>)
}
