package com.nielcode.kupass.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.testing.RecordingBiometricPromptShadow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Compose test uses Espresso, whose current SDK support ends at 35.
// Share the native-graphics sandbox with activity tests; two font ZIP mounts fail on Windows.
@Config(
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
    shadows = [RecordingBiometricPromptShadow::class],
    instrumentedPackages = ["androidx.biometric"],
)
class EntryDestinationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `restored sensitive destination cannot create its ViewModel or compose secrets before auth`() {
        val auth = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })
        auth.setDestination("restored-entry", 42L)
        auth.onForeground(true)
        var creations = 0
        val factory = viewModelFactory {
            initializer {
                creations++
                SecretProbeViewModel()
            }
        }
        compose.setContent {
            MaterialTheme {
                AccountDestination(
                    entryId = "restored-entry",
                    accountId = 42L,
                    authentication = auth,
                    onOpenSecuritySettings = {},
                    onCancel = {},
                ) {
                    viewModel<SecretProbeViewModel>(factory = factory)
                    Text("synthetic-secret-content")
                }
            }
        }
        compose.onNodeWithText("synthetic-secret-content").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, creations)
            val failed = requireNotNull(auth.beginNativePrompt())
            auth.nativeResult(failed.token, false)
        }
        compose.onNodeWithText("synthetic-secret-content").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, creations)
            auth.request(
                EntryAuthenticationViewModel.Action.UnlockDestination,
                "restored-entry",
                42L,
            )
            val success = requireNotNull(auth.beginNativePrompt())
            auth.nativeResult(success.token, true)
        }
        compose.onNodeWithText("synthetic-secret-content").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, creations)
            auth.revoke()
        }
        compose.onNodeWithText("synthetic-secret-content").assertDoesNotExist()
    }

    private class SecretProbeViewModel : ViewModel()
}
