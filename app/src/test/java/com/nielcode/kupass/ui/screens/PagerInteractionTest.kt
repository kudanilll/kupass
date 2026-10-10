package com.nielcode.kupass.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import com.nielcode.kupass.ui.components.MainTab
import com.nielcode.kupass.ui.screens.data.DataScreen
import com.nielcode.kupass.ui.theme.KupassTheme
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
class PagerInteractionTest {
    @get:Rule(order = 0)
    val touchMode =
        object : ExternalResource() {
            override fun before() {
                InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
            }
        }
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test
    fun `Data stretch gestures keep navigation visible and buttons usable`() {
        var imports = 0
        compose.setContent {
            KupassTheme(dynamicColor = false) {
                MainPager(onAddClick = {}, onTabChange = {}) { tab, padding ->
                    if (tab == MainTab.Data) {
                        DataScreen(
                            onExportClick = {},
                            onImportClick = { imports++ },
                            modifier = Modifier.testTag("data-page"),
                            contentPadding = padding,
                        )
                    } else Text(tab.name)
                }
            }
        }
        compose.onNodeWithContentDescription("Data").performClick()
        compose.onNodeWithTag("data-page").performTouchInput { swipeUp() }
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
        compose.onNodeWithTag("data-page").performTouchInput { swipeDown() }
        compose.onNodeWithContentDescription("Home").assertIsDisplayed()
        compose.onNodeWithText("Import Password").assertIsDisplayed().performClick()
        assertEquals(1, imports)
    }
}
