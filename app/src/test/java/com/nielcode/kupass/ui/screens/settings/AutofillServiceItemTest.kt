package com.nielcode.kupass.ui.screens.settings

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutofillServiceItemTest {

    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var pickerOpened = 0
    private var disabled = 0

    @Test
    fun `while Kupass is the service, a tap asks before turning autofill off`() {
        show(enabled = true)

        tapRow()
        compose.onNodeWithText(text(R.string.settings_autofill_disable_confirm)).performClick()

        assertEquals(1, disabled)
        assertEquals(0, pickerOpened)
    }

    @Test
    fun `cancel keeps autofill on`() {
        show(enabled = true)

        tapRow()
        compose.onNodeWithText(text(R.string.button_cancel)).performClick()

        assertEquals(0, disabled)
        compose.onNodeWithText(text(R.string.settings_autofill_disable_title)).assertDoesNotExist()
    }

    @Test
    fun `when autofill is off, a tap opens the system picker`() {
        show(enabled = false)

        tapRow()

        assertEquals(1, pickerOpened)
        compose.onNodeWithText(text(R.string.settings_autofill_disable_title)).assertDoesNotExist()
    }

    private fun show(enabled: Boolean) {
        compose.setContent {
            AutofillServiceItem(
                enabled = enabled,
                onOpenPicker = { pickerOpened++ },
                onDisable = { disabled++ },
            )
        }
    }

    private fun tapRow() {
        compose.onNodeWithText(text(R.string.settings_autofill_service)).performClick()
    }

    private fun text(id: Int) = context.getString(id)
}
