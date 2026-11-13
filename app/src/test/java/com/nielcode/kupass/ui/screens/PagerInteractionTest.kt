package com.nielcode.kupass.ui.screens

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
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
import androidx.compose.ui.unit.Velocity
import androidx.test.platform.app.InstrumentationRegistry
import com.nielcode.kupass.ui.components.MainTab
import com.nielcode.kupass.ui.screens.data.DataScreen
import com.nielcode.kupass.ui.theme.KupassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        compose.onNodeWithContentDescription("Vault").assertIsDisplayed()
        compose.onNodeWithText("Import Password").assertIsDisplayed().performClick()
        assertEquals(1, imports)
    }

    @Test
    fun `Data draws the stretch that its drags pull`() {
        val effects = mutableListOf<RecordingStretch>()
        val factory = RecordingFactory(effects)
        compose.setContent {
            // A short page switches from scrolling to stretching after its first layout. On a
            // Redmi Note 10 Pro, a stretch moved across that switch was never drawn, never settled,
            // and then took every later touch away from the pager.
            CompositionLocalProvider(LocalOverscrollFactory provides factory) {
                KupassTheme(dynamicColor = false) {
                    DataScreen(
                        onExportClick = {},
                        onImportClick = {},
                        modifier = Modifier.testTag("data-page"),
                    )
                }
            }
        }
        compose.onNodeWithTag("data-page").performTouchInput { swipeDown() }
        compose.waitForIdle()
        // A detached effect node is never drawn (confirmed with screenshots on that phone).
        assertTrue(effects.single { it.pulled }.attachedWhenPulled)
    }

    private class RecordingFactory(private val effects: MutableList<RecordingStretch>) :
        OverscrollFactory {
        override fun createOverscrollEffect(): OverscrollEffect =
            RecordingStretch().also(effects::add)

        override fun equals(other: Any?): Boolean = other === this

        override fun hashCode(): Int = System.identityHashCode(this)
    }

    private class RecordingStretch : OverscrollEffect {
        var pulled = false
        var attachedWhenPulled = false

        override val node: DelegatableNode = object : Modifier.Node() {}

        override val isInProgress = false

        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset {
            if (!pulled) {
                pulled = true
                attachedWhenPulled = node.node.isAttached
            }
            return performScroll(delta)
        }

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            performFling(velocity)
        }
    }
}
