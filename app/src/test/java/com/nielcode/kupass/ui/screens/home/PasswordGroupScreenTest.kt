package com.nielcode.kupass.ui.screens.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.testing.FakePasswordDao
import com.nielcode.kupass.ui.theme.KupassTheme
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class PasswordGroupScreenTest {
    @get:Rule(order = 0)
    val touchMode =
        object : ExternalResource() {
            override fun before() {
                InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
            }
        }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test
    fun `account page forwards selected ID and retains remaining accounts after deleting its anchor`() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val repository = PasswordRepository(FakePasswordDao())
        val ids = runBlocking {
            listOf("alice", "bob", "other").map { username ->
                repository.insertPassword(
                    PasswordEntity(
                        siteName = "Example",
                        username = username,
                        password = "not shown on the public page",
                        url =
                            if (username == "other") "https://other.example"
                            else "https://example.com",
                    )
                )
            }
        }
        val vm = HomeViewModel(repository)
        val opened = mutableListOf<Long>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            KupassTheme(dynamicColor = false) {
                PasswordGroupScreen(
                    groupId = ids.first(),
                    onNavigateBack = {},
                    onNavigateToDetail = { opened += it },
                    viewModel = vm,
                )
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("alice").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("bob").assertIsDisplayed().performClick()
        assertEquals(listOf(ids[1]), opened)
        compose.onNodeWithText("other").assertDoesNotExist()
        compose.onNodeWithText("not shown on the public page").assertDoesNotExist()
        runBlocking { repository.deletePasswordById(ids.first()) }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("alice").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("bob").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("bob").assertIsDisplayed()
        compose.onNodeWithText("example.com").assertIsDisplayed()
    }
}
