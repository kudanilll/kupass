package com.nielcode.kupass.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.ui.theme.KupassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real pager + HomeScreen: gestures start ON an account, not a mocked swipe callback. */
@RunWith(RobolectricTestRunner::class)
// Layout/semantics/input need no pixel snapshots; native graphics cannot share the other SDK
// sandboxes.
@GraphicsMode(GraphicsMode.Mode.LEGACY)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
// Keep the real pager regressions together so both directions share exactly the same harness.
@Suppress("TooManyFunctions")
class HomeInteractionTest {
    // SDK-35 window attachment must start in touch mode, before ActivityScenario launches.
    @get:Rule(order = 0)
    val touchMode =
        object : ExternalResource() {
            override fun before() {
                InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
            }
        }

    @get:Rule(order = 1) val compose = createComposeRule()

    private lateinit var pager: PagerState
    private var touchSlop = 0f
    private val navigated = mutableListOf<Long>()
    private val openedGroups = mutableListOf<Long>()
    private val deleted = mutableListOf<PasswordEntity>()
    private val sample =
        listOf(
            account(101, "alice"),
            account(202, "bob").copy(notes = "recovery marker"),
            account(303, "singleton").copy(siteName = "Other", url = "other.test"),
        )

    @Test
    fun `group uses a normal row with a total count badge and opens an account page`() {
        showHome(grouped = true)
        compose.onNodeWithText("alice").assertDoesNotExist()
        compose.onNodeWithContentDescription("2 accounts").assertIsDisplayed()
        compose.onNodeWithText("example.com").performClick()
        assertEquals(listOf(101L), openedGroups)
        assertTrue(navigated.isEmpty())
        compose.onNodeWithText("singleton").assertIsDisplayed()
        assertTrue(deleted.isEmpty())
        assertFalse(
            compose
                .onNodeWithText("example.com")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsActions.CustomActions)
        )
        compose.onNodeWithText("singleton").performClick()
        assertEquals(listOf(303L), navigated)
        compose.onNodeWithText("alice").assertDoesNotExist()
        assertNoDeleteAndPage(0)
    }

    @Test
    fun `rightward account swipe asks once resets and confirms only that account with no pager movement`() {
        showHome()
        val before = compose.onNodeWithText("bob").fetchSemanticsNode().boundsInRoot.left
        compose.onNodeWithText("bob").performTouchInput { swipeRight() }
        compose.onNodeWithText("Delete password").assertIsDisplayed()
        assertEquals(emptyList<PasswordEntity>(), deleted)
        compose.runOnIdle { assertEquals(0, pager.currentPage) }
        compose.onNodeWithText("CANCEL").performClick()
        assertEquals(
            before,
            compose.onNodeWithText("bob").fetchSemanticsNode().boundsInRoot.left,
            1f,
        )
        compose.onNodeWithText("bob").performTouchInput { swipeRight() }
        compose.onNodeWithText("DELETE").performClick()
        assertEquals(listOf(sample[1]), deleted)
        compose.onNodeWithText("Delete password").assertDoesNotExist()
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `rightward first move at exact touch slop owns deletion instead of the real pager`() {
        showHome()
        val row = compose.onNodeWithText("bob")
        val before = row.fetchSemanticsNode().boundsInRoot.left
        row.performTouchInput {
            // Doubling the actual pixel slop makes the first delta exactly slop, not above it.
            down(Offset(touchSlop, centerY))
            moveTo(Offset(touchSlop * 2f, centerY), delayMillis = 100)
            moveTo(Offset(width * 0.8f, centerY), delayMillis = 100)
            up()
        }
        compose.onNodeWithText("Delete password").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, pager.currentPage)
            assertEquals(0f, pager.currentPageOffsetFraction, 0f)
        }
        assertTrue(deleted.isEmpty())
        assertTrue(navigated.isEmpty())
        compose.onNodeWithText("CANCEL").performClick()
        assertEquals(before, row.fetchSemanticsNode().boundsInRoot.left, 1f)
    }

    @Test
    fun `leftward swipe starting on account reaches actual Data pager page without delete`() {
        showHome()
        compose.onNodeWithText("alice").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Data page").assertIsDisplayed()
        assertNoDeleteAndPage(1)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `rightward group swipe never requests account or bulk deletion`() {
        showHome(grouped = true)
        compose.onNodeWithText("example.com").performTouchInput { swipeRight() }
        assertNoDeleteAndPage(0)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `vertical swipe on account scrolls list and does not navigate or delete`() {
        val many = (1L..30L).map { account(it, "account-$it") }
        showHome(accounts = many)
        val before = compose.onNodeWithText("account-1").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithText("account-1").performTouchInput { swipeUp() }
        val after = compose.onNodeWithText("account-1").fetchSemanticsNode().boundsInRoot.top
        assertTrue("List must actually scroll", after < before)
        assertNoDeleteAndPage(0)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `short owned drag and cancelled full drag reset without confirmation`() {
        showHome()
        val row = compose.onNodeWithText("alice")
        val before = row.fetchSemanticsNode().boundsInRoot.left
        row.performTouchInput {
            down(Offset(width * 0.2f, centerY))
            moveTo(Offset(width * 0.4f, centerY), delayMillis = 100)
            up()
        }
        assertNoDeleteAndPage(0)
        assertEquals(before, row.fetchSemanticsNode().boundsInRoot.left, 1f)
        row.performTouchInput {
            down(Offset(width * 0.1f, centerY))
            moveTo(Offset(width * 0.8f, centerY), delayMillis = 100)
            cancel()
        }
        assertNoDeleteAndPage(0)
        assertEquals(before, row.fetchSemanticsNode().boundsInRoot.left, 1f)
        assertTrue(navigated.isEmpty())
        row.performTouchInput { swipeRight() }
        compose.onNodeWithText("Delete password").assertIsDisplayed()
        compose.onNodeWithText("CANCEL").performClick()
    }

    @Test
    fun `owned rightward gesture keeps ownership through leftward reversal`() {
        showHome()
        compose.onNodeWithText("alice").performTouchInput {
            down(Offset(width * 0.4f, centerY))
            moveTo(Offset(width * 0.8f, centerY), delayMillis = 100)
            moveTo(Offset(width * 0.1f, centerY), delayMillis = 100)
            up()
        }
        assertNoDeleteAndPage(0)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `consumed movement is abandoned without reacquiring later rightward movement`() {
        showHome(consumeFirstMove = true)
        compose.onNodeWithText("alice").performTouchInput {
            down(Offset(width * 0.1f, centerY))
            moveTo(Offset(width * 0.2f, centerY), delayMillis = 100)
            moveTo(Offset(width * 0.9f, centerY), delayMillis = 100)
            up()
        }
        assertNoDeleteAndPage(0)
    }

    @Test
    fun `multitouch abandonment cannot reacquire after the extra pointer lifts`() {
        showHome()
        compose.onNodeWithText("alice").performTouchInput {
            down(0, Offset(width * 0.1f, centerY))
            down(1, Offset(width * 0.2f, centerY))
            up(1)
            moveTo(0, Offset(width * 0.9f, centerY), delayMillis = 100)
            up(0)
        }
        assertNoDeleteAndPage(0)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `accessibility delete opens identical account confirmation without immediate deletion`() {
        showHome()
        val actions =
            compose
                .onNodeWithText("bob")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            assertEquals(1, actions.size)
            assertTrue(actions.single().action())
        }
        compose.onNodeWithText("Delete password").assertIsDisplayed()
        assertTrue(deleted.isEmpty())
        compose.onNodeWithText("DELETE").performClick()
        assertEquals(listOf(sample[1]), deleted)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `live notes search shows the matching site with its full account count`() {
        showHome(grouped = true)
        val search = compose.onNode(hasSetTextAction())
        search.performTextInput("recovery")
        compose.onNodeWithText("example.com").assertIsDisplayed()
        compose.onNodeWithText("alice").assertDoesNotExist()
        compose.onNodeWithText("other.test").assertDoesNotExist()
        compose.onNodeWithContentDescription("2 accounts").assertIsDisplayed()
        compose.onNodeWithText("example.com").performClick()
        assertEquals(listOf(101L), openedGroups)
        compose.onNodeWithContentDescription("Clear search").performClick()
        search.assertIsFocused()
        compose.onNodeWithText("alice").assertDoesNotExist()
        compose.onNodeWithText("singleton").assertIsDisplayed()
        search.performTextInput("bob")
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("example.com").assertIsDisplayed()
        compose.onNodeWithContentDescription("2 accounts").assertIsDisplayed()
        assertNoDeleteAndPage(0)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun `search empty states and IME close retain the query without any expanded overlay`() {
        showHome()
        val search = compose.onNode(hasSetTextAction())
        search.performTouchInput { click() }
        search.performTextInput("missing")
        compose.onNodeWithText("No results").assertIsDisplayed()
        compose.onNodeWithText("Searching: missing").assertDoesNotExist()
        search.performImeAction()
        search.assertIsNotFocused()
        compose.onNodeWithText("No results").assertIsDisplayed()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("alice").assertIsDisplayed()
    }

    @Test
    fun `empty vault displays its existing empty state`() {
        showHome(accounts = emptyList())
        compose.onNodeWithText("Your vault is empty").assertIsDisplayed()
    }

    private fun showHome(
        accounts: List<PasswordEntity> = sample,
        consumeFirstMove: Boolean = false,
        grouped: Boolean = false,
    ) {
        compose.setContent {
            KupassTheme(dynamicColor = false) {
                touchSlop = LocalViewConfiguration.current.touchSlop
                pager = rememberPagerState(pageCount = { 2 })
                var query by remember { mutableStateOf("") }
                val intercept = if (consumeFirstMove) Modifier.consumeFirstMovement() else Modifier
                Box(modifier = Modifier.fillMaxSize().then(intercept)) {
                    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { index ->
                        if (index == 0) {
                            HomeScreen(
                                passwords = accounts,
                                searchQuery = query,
                                onSearchQueryChange = { query = it },
                                onDeletePassword = { deleted += it },
                                onNavigateToDetail = { navigated += it },
                                onNavigateToGroup =
                                    if (grouped) { id -> openedGroups += id } else null,
                            )
                        } else {
                            Text("Data page")
                        }
                    }
                }
            }
        }
    }

    private fun assertNoDeleteAndPage(page: Int) {
        compose.onNodeWithText("Delete password").assertDoesNotExist()
        compose.runOnIdle { assertEquals(page, pager.currentPage) }
        assertTrue(deleted.isEmpty())
    }

    private fun account(id: Long, username: String) =
        PasswordEntity(
            id = id,
            siteName = "Example",
            username = username,
            password = "unused",
            url = "https://example.com",
        )

    private fun Modifier.consumeFirstMovement(): Modifier =
        pointerInput(Unit) {
            var consumed = false
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (
                        !consumed &&
                            event.changes.any { it.pressed && it.position != it.previousPosition }
                    ) {
                        event.changes.forEach { it.consume() }
                        consumed = true
                    }
                }
            }
        }
}
