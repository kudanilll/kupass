package com.nielcode.kupass.autofill

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.nielcode.kupass.data.local.db.PasswordEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutofillPickerTest {

    @get:Rule val compose = createComposeRule()

    private val github =
        PasswordEntity(id = 1, siteName = "GitHub", username = "octo", password = "1")
    private val gitlab =
        PasswordEntity(id = 2, siteName = "GitLab", username = "fox", password = "2")
    private val bank = PasswordEntity(id = 3, siteName = "Bank", username = "saver", password = "3")

    @Test
    fun `typing narrows the list in the next frame`() {
        show(searchable = true)
        compose.mainClock.autoAdvance = false

        compose.onNode(hasSetTextAction()).performTextInput("git")
        compose.mainClock.advanceTimeByFrame()

        compose.onNodeWithText("saver").assertDoesNotExist()
        compose.onNodeWithText("octo").assertExists()
        compose.onNodeWithText("fox").assertExists()
    }

    @Test
    fun `a picker without search shows every account it was given`() {
        show(searchable = false)

        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        listOf("octo", "fox", "saver").forEach { compose.onNodeWithText(it).assertExists() }
    }

    private fun show(searchable: Boolean) {
        compose.setContent {
            AutofillPicker(
                entries = listOf(github, gitlab, bank),
                searchable = searchable,
                onPick = {},
                onDismiss = {},
            )
        }
    }
}
