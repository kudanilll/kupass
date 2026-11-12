package com.nielcode.kupass.ui.screens

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.nielcode.kupass.App
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.ui.theme.KupassTheme
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The real NavDisplay wiring: saved back stack, entry IDs, and the grant gate. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class MainAppScreenNavigationTest {
    @get:Rule(order = 0)
    val touchMode =
        object : ExternalResource() {
            override fun before() {
                InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
            }
        }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test
    fun `an open account survives rotation and after process death asks to unlock again`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val app = RuntimeEnvironment.getApplication() as App
        val id = runBlocking {
            app.container.passwordRepository.insertPassword(
                PasswordEntity(
                    siteName = "GitHub",
                    username = "kudanilll",
                    password = "hunter22",
                    notes = NOTE,
                )
            )
        }
        var auth = controller()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val current = auth
            val state by current.state.collectAsState()
            KupassTheme(dynamicColor = false) {
                MainAppScreen(current, state, onOpenSecuritySettings = {})
            }
        }
        compose.waitForIdle()
        auth.request(Action.OpenAccount, HomeBase.entryId, id)
        auth.beginNativePrompt()?.let { auth.nativeResult(it.token, true) }
        waitForText(NOTE)

        // Rotation: the activity-retained controller stays, the back stack is restored.
        restoration.emulateSavedInstanceStateRestore()
        waitForText(NOTE)

        // Process death: a new controller has no grants, so the restored detail is locked.
        auth = controller()
        restoration.emulateSavedInstanceStateRestore()
        val unlock = app.getString(R.string.lock_button_unlock)
        waitForText(unlock)
        compose.onNodeWithText(NOTE).assertDoesNotExist()
        compose.onNodeWithText(unlock).performClick()
        assertEquals(Action.UnlockDestination, auth.state.value.pending?.action)
        assertEquals(id, auth.state.value.pending?.accountId)
    }

    private fun controller() =
        EntryAuthenticationViewModel({ 30_000L }, { 1_000L }).apply { onForeground(true) }

    private fun waitForText(text: String) =
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }

    private companion object {
        const val NOTE = "shown only on the detail page"
    }
}
