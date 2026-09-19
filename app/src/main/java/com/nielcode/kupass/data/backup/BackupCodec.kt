package com.nielcode.kupass.data.backup

import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.utils.CryptoException
import com.nielcode.kupass.utils.CryptoManager
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

/** Why a backup couldn't be read. Messages never contain secret values. */
sealed class BackupException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {
    /** The file is an encrypted v2 backup and no password was supplied. */
    class PasswordRequired : BackupException("Backup password required")

    /** Wrong backup password, or the file was modified. AES-GCM can't tell these apart. */
    class WrongPassword(cause: Throwable? = null) :
        BackupException("Wrong password or damaged file", cause)

    /** A legacy (v1) backup whose passwords were encrypted with another install's device key. */
    class ForeignDevice(cause: Throwable? = null) :
        BackupException("Legacy backup from another device", cause)

    class Unsupported(detail: String) : BackupException("Unsupported backup: $detail")

    class Malformed(cause: Throwable? = null) : BackupException("Malformed backup file", cause)

    class TooLarge : BackupException("Backup has too many entries")
}

/**
 * Portable, password-protected vault backups.
 *
 * Format v2 is a JSON envelope. The whole vault (every field of every entry) is one AES-256-GCM
 * ciphertext. The key comes from the user's backup password via PBKDF2-HMAC-SHA256 with a random
 * salt, so the file can be restored on any device. The envelope header is authenticated as AAD, so
 * the KDF parameters can't be changed without detection.
 *
 * Legacy v1 files (a JSON array whose `password` fields were encrypted with the exporting device's
 * Keystore key, or plaintext) are still importable, but only when this device can decrypt them.
 * Otherwise [BackupException.ForeignDevice] is thrown; ciphertext is never imported as a password.
 */
object BackupCodec {
    const val FORMAT = "kupass-backup"
    const val VERSION = 2
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"

    /** OWASP 2023 guidance for PBKDF2-HMAC-SHA256. */
    const val DEFAULT_ITERATIONS = 600_000
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_ENTRIES = 10_000

    private const val MIN_ITERATIONS = 10_000
    private const val MAX_ITERATIONS = 10_000_000
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** True if [content] is a v2 envelope, meaning a password is needed to read it. */
    fun isPasswordProtected(content: String): Boolean =
        try {
            val root = json.parseToJsonElement(content)
            val header = (root as? JsonObject)?.get("header") as? JsonObject
            header?.get("format")?.jsonPrimitive?.content == FORMAT
        } catch (_: IllegalArgumentException) { // includes SerializationException
            false
        }

    /** Encrypts [entries] into a v2 backup. [password] is not cleared; the caller owns it. */
    fun encode(
        entries: List<PasswordEntity>,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): String {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Backup password too short" }
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Invalid iteration count" }

        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val header =
            Header(
                kdf = Kdf(KDF_ALGORITHM, iterations, b64(salt)),
                cipher = CipherParams(CIPHER_ALGORITHM, b64(iv)),
            )
        val payload =
            json
                .encodeToString(Payload(entries.map { it.toBackupEntry() }))
                .toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                deriveKey(password, salt, iterations),
                GCMParameterSpec(TAG_BITS, iv),
            )
            cipher.updateAAD(header.aad())
            val data = cipher.doFinal(payload)
            return json.encodeToString(Envelope(header, b64(data)))
        } catch (e: GeneralSecurityException) {
            throw CryptoException("Backup encryption failed", e)
        } finally {
            payload.fill(0)
        }
    }

    /**
     * Reads a backup of any supported version into entities with `id = 0`.
     *
     * @param password required for v2 files; ignored for legacy files.
     * @throws BackupException describing why the file can't be imported.
     */
    fun decode(content: String, password: CharArray?): List<PasswordEntity> =
        when (val root = parse { json.parseToJsonElement(content) }) {
            is JsonArray -> decodeLegacy(root)
            is JsonObject -> decodeV2(root, password ?: throw BackupException.PasswordRequired())
            else -> throw BackupException.Malformed()
        }

    private fun decodeV2(root: JsonObject, password: CharArray): List<PasswordEntity> {
        val envelope = parse { json.decodeFromJsonElement<Envelope>(root) }
        val header = envelope.header
        requireSupported(header)
        val salt = unb64(header.kdf.salt)
        val iv = unb64(header.cipher.iv)
        if (salt.size != SALT_SIZE || iv.size != IV_SIZE) throw BackupException.Malformed()

        val plain = decryptPayload(unb64(envelope.data), password, header, salt, iv)
        try {
            val payload = parse { json.decodeFromString<Payload>(plain.toString(Charsets.UTF_8)) }
            if (payload.entries.size > MAX_ENTRIES) throw BackupException.TooLarge()
            return payload.entries.map { it.toEntity(it.password) }
        } finally {
            plain.fill(0)
        }
    }

    /** Rejects headers this version can't read, before any expensive key derivation. */
    private fun requireSupported(header: Header) {
        val problem =
            when {
                header.format != FORMAT -> "format ${header.format}"
                header.version != VERSION -> "version ${header.version}"
                header.kdf.algorithm != KDF_ALGORITHM -> "kdf ${header.kdf.algorithm}"
                header.cipher.algorithm != CIPHER_ALGORITHM -> "cipher ${header.cipher.algorithm}"
                header.kdf.iterations !in MIN_ITERATIONS..MAX_ITERATIONS -> "iterations"
                else -> null
            }
        if (problem != null) throw BackupException.Unsupported(problem)
    }

    private fun decryptPayload(
        data: ByteArray,
        password: CharArray,
        header: Header,
        salt: ByteArray,
        iv: ByteArray,
    ): ByteArray =
        try {
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                deriveKey(password, salt, header.kdf.iterations),
                GCMParameterSpec(TAG_BITS, iv),
            )
            cipher.updateAAD(header.aad())
            cipher.doFinal(data)
        } catch (e: AEADBadTagException) {
            throw BackupException.WrongPassword(e)
        } catch (e: GeneralSecurityException) {
            throw BackupException.Malformed(e)
        }

    private fun decodeLegacy(root: JsonArray): List<PasswordEntity> {
        if (root.size > MAX_ENTRIES) throw BackupException.TooLarge()
        val entries = parse { json.decodeFromJsonElement<List<BackupEntry>>(root) }
        return entries.map { entry ->
            val password =
                try {
                    CryptoManager.decrypt(entry.password)
                } catch (e: CryptoException) {
                    throw BackupException.ForeignDevice(e)
                }
            entry.toEntity(password)
        }
    }

    /** Runs a kotlinx.serialization call, reporting any parsing failure as a malformed file. */
    private inline fun <T> parse(block: () -> T): T =
        try {
            block()
        } catch (e: IllegalArgumentException) { // includes SerializationException
            throw BackupException.Malformed(e)
        }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            val encoded = SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
            try {
                return SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    private fun Header.aad(): ByteArray =
        "$format|$version|${kdf.algorithm}|${kdf.iterations}|${kdf.salt}|${cipher.algorithm}|${cipher.iv}"
            .toByteArray(Charsets.UTF_8)

    @Serializable private data class Envelope(val header: Header, val data: String)

    @Serializable
    private data class Header(
        val format: String = FORMAT,
        val version: Int = VERSION,
        val kdf: Kdf,
        val cipher: CipherParams,
    )

    @Serializable
    private data class Kdf(val algorithm: String, val iterations: Int, val salt: String)

    @Serializable private data class CipherParams(val algorithm: String, val iv: String)

    @Serializable private data class Payload(val entries: List<BackupEntry>)
}

private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

private fun unb64(value: String): ByteArray =
    try {
        Base64.getDecoder().decode(value)
    } catch (e: IllegalArgumentException) {
        throw BackupException.Malformed(e)
    }

/** One vault entry inside a backup. Field names match the legacy v1 array format. */
@Serializable
internal data class BackupEntry(
    @SerialName("siteName") val siteName: String,
    val username: String = "",
    val password: String,
    val url: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun toEntity(plainPassword: String) =
        PasswordEntity(
            id = 0,
            siteName = siteName,
            username = username,
            password = plainPassword,
            url = url,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}

internal fun PasswordEntity.toBackupEntry() =
    BackupEntry(
        siteName = siteName,
        username = username,
        password = password,
        url = url,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
