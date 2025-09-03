package com.nielcode.kupass.core.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager

class CryptoManager(context: Context) {
    private val aead: Aead

    init {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "kupass_keyset", "kupass_prefs")
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://kupass_master_key")
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encryptToBase64(plain: String, aad: String = ""): String {
        val ct = aead.encrypt(plain.toByteArray(), aad.toByteArray())
        return Base64.encodeToString(ct, Base64.NO_WRAP)
    }

    fun decryptFromBase64(cipherB64: String, aad: String = ""): String {
        val bytes = Base64.decode(cipherB64, Base64.NO_WRAP)
        val pt = aead.decrypt(bytes, aad.toByteArray())
        return String(pt)
    }
}
