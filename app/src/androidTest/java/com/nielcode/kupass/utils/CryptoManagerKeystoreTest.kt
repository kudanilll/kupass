package com.nielcode.kupass.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real AndroidKeyStore path, which JVM/Robolectric tests can't reach. */
@RunWith(AndroidJUnit4::class)
class CryptoManagerKeystoreTest {

    @Test
    fun roundTripsWithTheKeystoreKey() {
        val secret = "correct horse battery staple ✓ ünïcode"

        val encrypted = CryptoManager.encrypt(secret)

        assertTrue(encrypted.startsWith(CryptoManager.PREFIX_V2))
        assertFalse(encrypted.contains(secret))
        assertEquals(secret, CryptoManager.decrypt(encrypted))
    }

    @Test
    fun vaultKeyLivesInAndroidKeystore() {
        CryptoManager.encrypt("create the key if needed")

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertTrue(keyStore.containsAlias("kupass_vault_key"))
    }
}
