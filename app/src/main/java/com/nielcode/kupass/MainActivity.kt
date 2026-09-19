package com.nielcode.kupass

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nielcode.kupass.security.AppLock
import com.nielcode.kupass.ui.screens.MainAppScreen
import com.nielcode.kupass.ui.screens.lock.LockScreen
import com.nielcode.kupass.ui.theme.KupassTheme
import com.nielcode.kupass.utils.AppConfig

class MainActivity : AppCompatActivity() {

    private val appLock: AppLock
        get() = (application as App).container.appLock

    /** Whether the device has a PIN/pattern/password. Re-checked on every start. */
    private val deviceSecure = mutableStateOf(true)

    private var authenticating = false
    private lateinit var biometricPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // BiometricPrompt must be created in onCreate so it survives configuration changes.
        biometricPrompt =
            BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        authenticating = false
                        appLock.unlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        authenticating = false
                        if (errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)
                            deviceSecure.value = false
                        // Any other error (cancel, lockout, …) keeps the vault locked.
                    }
                },
            )

        enableEdgeToEdge()
        setContent {
            val prefs = remember { (application as App).container.preferenceManager }
            val isDynamicEnabled = prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE
            val locked by appLock.locked.collectAsStateWithLifecycle()
            KupassTheme(dynamicColor = isDynamicEnabled) {
                if (locked) {
                    LockScreen(
                        deviceSecure = deviceSecure.value,
                        onUnlock = ::promptUnlock,
                        onOpenSecuritySettings = ::openSecuritySettings,
                    )
                } else {
                    MainAppScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        deviceSecure.value = getSystemService(KeyguardManager::class.java).isDeviceSecure
        appLock.onForeground()
    }

    override fun onStop() {
        appLock.onBackground(isChangingConfigurations)
        super.onStop()
    }

    private fun promptUnlock() {
        if (authenticating || !deviceSecure.value) return
        authenticating = true
        val authenticators =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            } else {
                // BIOMETRIC_STRONG | DEVICE_CREDENTIAL is unsupported on API 28-29.
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            }
        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_prompt_title))
                .setSubtitle(getString(R.string.lock_prompt_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun openSecuritySettings() {
        appLock.allowNextBackground()
        try {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
