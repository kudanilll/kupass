package com.nielcode.kupass.security

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticatorsTest {

    @Test
    fun `android 11 and newer use strong biometrics or the device credential`() {
        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, kupassAuthenticators(30))
        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, kupassAuthenticators(37))
    }

    @Test
    fun `android 10 and older fall back to weak biometrics or the device credential`() {
        assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, kupassAuthenticators(29))
        assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, kupassAuthenticators(27))
    }
}
