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

/**
 * An app-private file that is never stored in plaintext.
 *
 * Each write is sealed with a fresh AES-256-GCM key, and that key is wrapped by the vault's
 * Keystore key, so a read costs one Keystore operation however large the file is. [purpose] is
 * bound to the ciphertext, so one sealed file can't be passed off as another. A file that can't be
 * opened is deleted. Writes are atomic: a failed write leaves the previous file in place.
 */
class SealedFile(private val file: File, purpose: String) {
    private val atomicFile = AtomicFile(file)
    private val aad = purpose.encodeToByteArray()
    private val random = SecureRandom()

    /** The text last written, or null when missing, unreadable, or sealed by another key. */
    fun read(): String? {
        if (!file.exists()) return null
        val text = readableOrNull {
            open(Json.decodeFromString<Envelope>(atomicFile.readFully().decodeToString()))
        }
        if (text == null) delete()
        return text
    }

    /** Replaces the contents with [text]. Returns false if it couldn't be sealed or written. */
    fun write(text: String): Boolean {
        val bytes = readableOrNull { Json.encodeToString(seal(text)).encodeToByteArray() }
        return bytes != null && writeAtomically(bytes)
    }

    fun delete() = atomicFile.delete()

    private fun writeAtomically(bytes: ByteArray): Boolean {
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

    private fun seal(text: String): Envelope {
        val key = ByteArray(KEY_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        try {
            val sealed = cipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(text.encodeToByteArray())
            return Envelope(
                version = VERSION,
                key = CryptoManager.encrypt(base64(key)),
                data = base64(iv + sealed),
            )
        } finally {
            key.fill(0)
        }
    }

    private fun open(envelope: Envelope): String {
        // Wrong version or a wrapped key outside the current format: treat it as unreadable.
        require(envelope.version == VERSION && envelope.key.startsWith(CryptoManager.PREFIX_V2))
        val key = Base64.getDecoder().decode(CryptoManager.decrypt(envelope.key))
        try {
            require(key.size == KEY_BYTES)
            val sealed = Base64.getDecoder().decode(envelope.data)
            require(sealed.size > IV_BYTES)
            return cipher(Cipher.DECRYPT_MODE, key, sealed.copyOf(IV_BYTES))
                .doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
                .decodeToString()
        } finally {
            key.fill(0)
        }
    }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(aad)
        }

    @Serializable private class Envelope(val version: Int, val key: String, val data: String)

    private companion object {
        const val VERSION = 1
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val IV_BYTES = 12
        const val TAG_BITS = 128

        fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    }
}

/** [block]'s result, or null when a file, key, ciphertext or JSON turns out to be unusable. */
internal inline fun <T> readableOrNull(block: () -> T): T? =
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
