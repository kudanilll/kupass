package com.nielcode.kupass.security

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/** What may unlock Kupass: biometrics or the device PIN, pattern, or password. */
fun kupassAuthenticators(sdkInt: Int = Build.VERSION.SDK_INT): Int =
    if (sdkInt >= Build.VERSION_CODES.R) {
        BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    } else {
        // BIOMETRIC_STRONG | DEVICE_CREDENTIAL is unsupported on API 28-29.
        BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }
