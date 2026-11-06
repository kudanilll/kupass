package com.nielcode.kupass.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.nielcode.kupass.R
import com.nielcode.kupass.data.backup.GooglePasswordCsv
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.testing.FakePasswordDao
import com.nielcode.kupass.testing.RecordingBiometricPromptShadow
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.editor.PasswordEditorViewModel
import javax.crypto.KeyGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Use the existing native-graphics sandbox and supported Compose/Espresso SDK.
@Config(
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
    shadows = [RecordingBiometricPromptShadow::class],
    instrumentedPackages = ["androidx.biometric"],
)
class ImportedEntryEditorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `protected imported editor accepts whitespace password preserves credentials and rejects empty`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val repository = PasswordRepository(FakePasswordDao())
        val original = runBlocking {
            val csv =
                "name,url,username,password,note\nImported,https://example.com, me , \t ,old note"
            assertEquals(
                1,
                repository.importPasswords(GooglePasswordCsv.decode(csv).entries).imported,
            )
            repository.getAllPasswords().first().single()
        }
        val auth = authenticatedEditor(original.id)
        val vm = PasswordEditorViewModel(repository, requestedId = original.id)
        var navigatedBack = false
        compose.setContent {
            MaterialTheme {
                AccountDestination(
                    entryId = "editor",
                    accountId = original.id,
                    authentication = auth,
                    onOpenSecuritySettings = {},
                    onCancel = {},
                ) {
                    PasswordEditorScreen(
                        passwordId = original.id,
                        onNavigateBack = { navigatedBack = true },
                        viewModel = vm,
                    )
                }
            }
        }
        compose.waitUntil { vm.existingPassword.value != null }
        val app = RuntimeEnvironment.getApplication()
        val save = compose.onNodeWithContentDescription(app.getString(R.string.button_save))
        save.assertIsEnabled()
        val password = compose.onNodeWithText(app.getString(R.string.password_hint))
        password.performTextReplacement("")
        save.assertIsNotEnabled()
        password.performTextReplacement(" \t ")
        save.assertIsEnabled()
        val site = compose.onNodeWithText(app.getString(R.string.site_or_app_hint))
        site.performTextReplacement(" \t ")
        save.assertIsNotEnabled()
        site.performTextReplacement(original.siteName)
        compose
            .onNodeWithText(app.getString(R.string.note_hint))
            .performTextReplacement(" edited note ")
        save.assertIsEnabled().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { compose.runOnIdle { navigatedBack } }

        val saved = runBlocking { repository.getPasswordById(original.id).first() }
        assertEquals(
            original.copy(notes = " edited note ", updatedAt = saved?.updatedAt ?: 0),
            saved,
        )
        assertEquals(" me ", saved?.username)
        assertEquals(" \t ", saved?.password)
    }

    @Test
    fun `clicking password generator button generates a password and enables save`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val repository = PasswordRepository(FakePasswordDao())
        val vm = PasswordEditorViewModel(repository, requestedId = -1L)
        compose.setContent {
            MaterialTheme {
                PasswordEditorScreen(
                    passwordId = -1L,
                    onNavigateBack = {},
                    viewModel = vm,
                )
            }
        }
        val app = RuntimeEnvironment.getApplication()
        val site = compose.onNodeWithText(app.getString(R.string.site_or_app_hint))
        site.performTextReplacement("Example Site")

        val save = compose.onNodeWithContentDescription(app.getString(R.string.button_save))
        save.assertIsNotEnabled()

        val generateButton =
            compose.onNodeWithContentDescription(app.getString(R.string.generate_password))
        generateButton.assertIsEnabled()
        generateButton.performClick()

        save.assertIsEnabled()
    }

    @Test
    fun `clicking password generator when password is present shows confirmation dialog`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val repository = PasswordRepository(FakePasswordDao())
        val vm = PasswordEditorViewModel(repository, requestedId = -1L)
        compose.setContent {
            MaterialTheme {
                PasswordEditorScreen(
                    passwordId = -1L,
                    onNavigateBack = {},
                    viewModel = vm,
                )
            }
        }
        val app = RuntimeEnvironment.getApplication()
        val site = compose.onNodeWithText(app.getString(R.string.site_or_app_hint))
        site.performTextReplacement("Example Site")

        val passwordField = compose.onNodeWithText(app.getString(R.string.password_hint))
        val initialPassword = "existingSecretPassword"
        passwordField.performTextReplacement(initialPassword)

        val generateButton =
            compose.onNodeWithContentDescription(app.getString(R.string.generate_password))
        generateButton.assertIsEnabled()
        generateButton.performClick()

        // Dialog konfirmasi muncul
        val dialogTitle =
            compose.onNodeWithText(app.getString(R.string.dialog_replace_password_title))
        val dialogMessage =
            compose.onNodeWithText(app.getString(R.string.dialog_replace_password_message))
        dialogTitle.assertIsDisplayed()
        dialogMessage.assertIsDisplayed()

        // Tekan batal -> password lama tidak berubah dan dialog ditutup
        val cancelButton = compose.onNodeWithText(app.getString(R.string.button_cancel))
        cancelButton.performClick()
        dialogTitle.assertDoesNotExist()
        dialogMessage.assertDoesNotExist()
        compose.onNodeWithText(initialPassword).assertExists()

        // Tekan tombol generator lagi -> dialog muncul kembali
        generateButton.performClick()
        dialogTitle.assertIsDisplayed()

        // Tekan ganti -> password terbarui dan save button aktif
        val replaceButton = compose.onNodeWithText(app.getString(R.string.button_replace))
        replaceButton.performClick()
        dialogTitle.assertDoesNotExist()
        compose.onNodeWithText(initialPassword).assertDoesNotExist()

        val save = compose.onNodeWithContentDescription(app.getString(R.string.button_save))
        save.assertIsEnabled()
    }

    private fun authenticatedEditor(id: Long) =
        EntryAuthenticationViewModel({ 30_000L }, { 1_000L }).apply {
            setDestination("editor", id)
            onForeground(true)
            request(EntryAuthenticationViewModel.Action.UnlockDestination, "editor", id)
            val request = requireNotNull(beginNativePrompt())
            nativeResult(request.token, true)
        }
}
