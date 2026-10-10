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
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.ui.screens.MainAppScreen
import com.nielcode.kupass.ui.theme.KupassTheme
import com.nielcode.kupass.utils.AppConfig
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var authentication: EntryAuthenticationViewModel
    private lateinit var biometricPrompt: BiometricPrompt
    private var userLeaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        authentication =
            ViewModelProvider(this, EntryAuthenticationViewModel.Factory)[
                EntryAuthenticationViewModel::class.java]
        // Reattach to the retained native session, without calling authenticate again on rotation.
        biometricPrompt = createPrompt(authentication.state.value.nativeRequest?.token)
        observeAuthentication()

        enableEdgeToEdge()
        setContent {
            val prefs = remember { (application as App).container.preferenceManager }
            val isDynamicEnabled = prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE
            val authState by authentication.state.collectAsStateWithLifecycle()
            KupassTheme(dynamicColor = isDynamicEnabled) {
                MainAppScreen(
                    authentication = authentication,
                    authState = authState,
                    onOpenSecuritySettings = ::openSecuritySettings,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        authentication.onForeground(getSystemService(KeyguardManager::class.java).isDeviceSecure)
    }

    override fun onStop() {
        // The hint and stop belong to one departure; do not consume an exemption twice.
        if (!userLeaving) authentication.onBackground(isChangingConfigurations)
        if (!isChangingConfigurations && authentication.state.value.pending == null) {
            biometricPrompt.cancelAuthentication()
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // A quick launcher round trip can resume without ever stopping or starting this activity.
        authentication.onForeground(getSystemService(KeyguardManager::class.java).isDeviceSecure)
        userLeaving = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        userLeaving = true
        authentication.onBackground(isChangingConfigurations = false)
    }

    private fun observeAuthentication() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                authentication.state.collect { state ->
                    if (state.nativeRequest != null && state.pending != state.nativeRequest) {
                        biometricPrompt.cancelAuthentication()
                    } else {
                        authentication.beginNativePrompt()?.let { request ->
                            biometricPrompt = createPrompt(request.token)
                            biometricPrompt.authenticate(promptInfo(request.action))
                        }
                    }
                }
            }
        }
    }

    private fun createPrompt(token: Long?): BiometricPrompt =
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    token?.let {
                        authentication.nativeResult(
                            it,
                            succeeded = true,
                            credentialAvailable =
                                getSystemService(KeyguardManager::class.java).isDeviceSecure,
                        )
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    token?.let {
                        authentication.nativeResult(
                            it,
                            succeeded = false,
                            credentialAvailable =
                                errorCode != BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL &&
                                    authentication.state.value.deviceSecure,
                        )
                    }
                }
            },
        )

    private fun promptInfo(
        action: EntryAuthenticationViewModel.Action
    ): BiometricPrompt.PromptInfo {
        val authenticators =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            } else {
                // BIOMETRIC_STRONG | DEVICE_CREDENTIAL is unsupported on API 28-29.
                BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            }
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_prompt_title))
            .setSubtitle(
                getString(
                    if (action == EntryAuthenticationViewModel.Action.Export)
                        R.string.lock_export_prompt_subtitle
                    else R.string.lock_prompt_subtitle
                )
            )
            .setAllowedAuthenticators(authenticators)
            .build()
    }

    private fun openSecuritySettings() {
        authentication.allowNextBackground()
        try {
            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
