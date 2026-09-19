package com.nielcode.kupass.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.VisibleForTesting
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Thrown when vault data cannot be encrypted or decrypted. Never carries secret values. */
class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Encrypts vault fields with a non-exportable AES-256-GCM key in the Android Keystore.
 *
 * Stored format (v2): `kp2:` + Base64(IV[12] || ciphertext || tag[16]). Legacy formats are still
 * readable so existing vaults keep working:
 * - v1: Base64(IV || ciphertext || tag) without a prefix (builds before the v2 format).
 * - plaintext: rows written before encryption existed.
 *
 * Failure policy is fail-closed: [encrypt] never returns plaintext, and [decrypt] throws
 * [CryptoException] instead of returning ciphertext as if it were data.
 */
object CryptoManager {
    private const val ALIAS = "kupass_vault_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private const val TAG_SIZE = TAG_BITS / 8
    private const val KEY_SIZE_BITS = 256

    /**
     * v1 ciphertext is canonical Base64 of at least IV + tag + 1 byte: 40+ chars, length % 4 == 0.
     */
    private const val BASE64_BLOCK = 4
    private const val MIN_V1_CIPHERTEXT_CHARS = 40

    const val PREFIX_V2 = "kp2:"

    @Volatile private var cachedKey: SecretKey? = null

    private var keyProvider: () -> SecretKey = {
        cachedKey ?: synchronized(this) { cachedKey ?: keystoreKey().also { cachedKey = it } }
    }

    /** Replaces the Android Keystore key, for JVM tests where AndroidKeyStore doesn't exist. */
    @VisibleForTesting
    fun setKeyProviderForTesting(provider: () -> SecretKey) {
        keyProvider = provider
    }

    /** True if [stored] is already in the current format and needs no re-encryption. */
    fun isCurrentFormat(stored: String): Boolean = stored.isEmpty() || stored.startsWith(PREFIX_V2)

    /** Encrypts [plaintext] into the v2 format. The empty string stays empty. */
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return plaintext
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
            val iv = cipher.iv
            check(iv.size == IV_SIZE) { "Unexpected IV size" }
            PREFIX_V2 +
                Base64.encodeToString(
                    iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)),
                    Base64.NO_WRAP,
                )
        } catch (e: GeneralSecurityException) {
            throw CryptoException("Encryption failed", e)
        } catch (e: IllegalStateException) {
            throw CryptoException("Encryption failed", e)
        }
    }

    /**
     * Decrypts a stored value in any supported format.
     *
     * @throws CryptoException if a v2 value can't be decrypted, or a value that looks like v1
     *   ciphertext fails authentication (wrong key or corruption).
     */
    fun decrypt(stored: String): String =
        when {
            stored.isEmpty() -> stored
            stored.startsWith(PREFIX_V2) ->
                decryptPayload(
                    decodeBase64(stored.removePrefix(PREFIX_V2))
                        ?: throw CryptoException("Malformed ciphertext")
                )
            else -> decryptLegacy(stored)
        }

    /** A value without a prefix: v1 ciphertext, or plaintext written before encryption existed. */
    private fun decryptLegacy(stored: String): String {
        val payload = decodeBase64(stored)
        if (payload == null || payload.size < IV_SIZE + TAG_SIZE) return stored
        return try {
            decryptPayload(payload)
        } catch (e: CryptoException) {
            if (looksLikeV1Ciphertext(stored)) throw e else stored
        }
    }

    private fun decryptPayload(payload: ByteArray): String {
        if (payload.size < IV_SIZE + TAG_SIZE) throw CryptoException("Ciphertext too short")
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                keyProvider(),
                GCMParameterSpec(TAG_BITS, payload, 0, IV_SIZE),
            )
            String(cipher.doFinal(payload, IV_SIZE, payload.size - IV_SIZE), Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw CryptoException("Decryption failed", e)
        }
    }

    /**
     * v1 ciphertext was always canonical, unwrapped Base64 whose length is a multiple of 4. A
     * legacy plaintext password rarely satisfies that and also decodes to >= 28 bytes, so a failed
     * decrypt of such a value is treated as corruption rather than silently accepted.
     */
    private fun looksLikeV1Ciphertext(value: String): Boolean =
        value.length % BASE64_BLOCK == 0 &&
            value.length >= MIN_V1_CIPHERTEXT_CHARS &&
            value.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }

    private fun decodeBase64(value: String): ByteArray? =
        try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            null
        }

    /** Loads the vault key, creating it on first use. Keys from earlier builds are AES-128. */
    private fun keystoreKey(): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
                return it.secretKey
            }
            val generator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
            return generator.generateKey()
        } catch (e: GeneralSecurityException) {
            throw CryptoException("Vault key unavailable", e)
        } catch (e: java.io.IOException) {
            throw CryptoException("Vault key unavailable", e)
        }
    }
}
