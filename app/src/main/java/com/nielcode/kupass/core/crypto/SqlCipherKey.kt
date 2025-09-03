package com.nielcode.kupass.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SqlCipherKey {
    private const val PREF = "kupass_secure_prefs"
    private const val SALT_KEY = "sqlcipher_salt_b64"
    private const val PASS_KEY = "sqlcipher_passphrase_b64"

    // Derive passphrase from a random app secret (no user PIN in this sample).
    fun getOrCreate(context: Context): ByteArray {
        val masterKey =
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            context, PREF, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        var saltB64 = prefs.getString(SALT_KEY, null)
        var passB64 = prefs.getString(PASS_KEY, null)

        if (saltB64 == null || passB64 == null) {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val appSecret = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val pass = pbkdf2(appSecret, salt, 100_000, 32) // 256-bit

            saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            passB64 = Base64.encodeToString(pass, Base64.NO_WRAP)
            prefs.edit().putString(SALT_KEY, saltB64).putString(PASS_KEY, passB64).apply()
        }

        return Base64.decode(passB64, Base64.NO_WRAP)
    }

    private fun pbkdf2(secret: ByteArray, salt: ByteArray, iter: Int, len: Int): ByteArray {
        val spec =
            PBEKeySpec(secret.toString(Charsets.ISO_8859_1).toCharArray(), salt, iter, len * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }
}
