package com.nielcode.kupass.data.local.cache

import android.util.AtomicFile
import com.nielcode.kupass.security.CryptoException
import com.nielcode.kupass.security.CryptoManager
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
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
 * It is never stored in plaintext. Entries are sealed with a fresh AES-256-GCM key per write, and
 * that key is wrapped by the vault's Keystore key, so a load costs one Keystore operation instead
 * of one per field. The file lives in `noBackupFilesDir` (the backup rules exclude it as well) and
 * is deleted whenever it can't be read. It is only a cache: entries are keyed by row fingerprints,
 * so the database still decides what is shown.
 */
class VaultSnapshotStore(private val file: File) {
    private val atomicFile = AtomicFile(file)
    private val random = SecureRandom()

    /**
     * Cached fields by row fingerprint; empty when missing, unreadable, or sealed by another key.
     */
    fun load(): Map<String, DecryptedFields> {
        if (!file.exists()) return emptyMap()
        val entries = readable {
            open(Json.decodeFromString<Envelope>(atomicFile.readFully().decodeToString()))
        }
        if (entries == null) delete()
        return entries.orEmpty()
    }

    /** Replaces the cache with [entries]. Returns false if it couldn't be written. */
    fun save(entries: Map<String, DecryptedFields>): Boolean {
        val bytes = readable { Json.encodeToString(seal(entries)).encodeToByteArray() }
        return bytes != null && write(bytes)
    }

    /** Writes [bytes] atomically: a failed write leaves the previous file in place. */
    private fun write(bytes: ByteArray): Boolean {
        val out =
            try {
                atomicFile.startWrite()
            } catch (_: IOException) {
                return false
            }
        return try {
            out.write(bytes)
            atomicFile.finishWrite(out)
            true
        } catch (_: IOException) {
            atomicFile.failWrite(out)
            false
        }
    }

    fun delete() = atomicFile.delete()

    private fun seal(entries: Map<String, DecryptedFields>): Envelope {
        val key = ByteArray(KEY_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        try {
            val sealed =
                cipher(Cipher.ENCRYPT_MODE, key, iv)
                    .doFinal(Json.encodeToString(Snapshot(entries)).encodeToByteArray())
            return Envelope(
                version = VERSION,
                key = CryptoManager.encrypt(base64(key)),
                data = base64(iv + sealed),
            )
        } finally {
            key.fill(0)
        }
    }

    private fun open(envelope: Envelope): Map<String, DecryptedFields> {
        // Wrong version or a wrapped key outside the current format: start over from the database.
        require(envelope.version == VERSION && envelope.key.startsWith(CryptoManager.PREFIX_V2))
        val key = Base64.getDecoder().decode(CryptoManager.decrypt(envelope.key))
        try {
            require(key.size == KEY_BYTES)
            val sealed = Base64.getDecoder().decode(envelope.data)
            require(sealed.size > IV_BYTES)
            val plain =
                cipher(Cipher.DECRYPT_MODE, key, sealed.copyOf(IV_BYTES))
                    .doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
            return Json.decodeFromString<Snapshot>(plain.decodeToString()).entries
        } finally {
            key.fill(0)
        }
    }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(AAD)
        }

    @Serializable private class Envelope(val version: Int, val key: String, val data: String)

    @Serializable private class Snapshot(val entries: Map<String, DecryptedFields>)

    private companion object {
        const val VERSION = 1
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val IV_BYTES = 12
        const val TAG_BITS = 128

        /** Binds the ciphertext to this purpose, so it can't be swapped with other GCM data. */
        val AAD = "kupass-vault-cache-v1".encodeToByteArray()

        fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

        /** [block]'s result, or null when the file, key or ciphertext turns out to be unusable. */
        inline fun <T> readable(block: () -> T): T? =
            try {
                block()
            } catch (_: IOException) {
                null
            } catch (_: GeneralSecurityException) {
                null
            } catch (_: CryptoException) {
                null
            } catch (_: IllegalArgumentException) {
                // Also covers SerializationException and malformed Base64.
                null
            }
    }
}
