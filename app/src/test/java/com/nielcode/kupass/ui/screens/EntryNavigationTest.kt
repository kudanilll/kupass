package com.nielcode.kupass.ui.screens

import androidx.navigation3.runtime.NavBackStack
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EntryNavigationTest {
    private val auth = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })

    @Test
    fun `navigation binds grants to new entry ids and preserves detail editor back`() {
        val nav = navigator(auth)
        auth.onForeground(true)
        val first = openAccount(nav, HomeBase.entryId)
        assertTrue(auth.canRead(first, ACCOUNT))
        auth.openEditor(first, ACCOUNT) { nav.navigate(PasswordEditor(ACCOUNT, newEntryId())) }
        val editor = nav.current.entryId
        assertTrue(auth.canRead(editor, ACCOUNT))
        nav.goBack()
        assertTrue(auth.canRead(first, ACCOUNT))
        nav.goBack()
        assertFalse(auth.canRead(first, ACCOUNT))
        val second = openAccount(nav, HomeBase.entryId)
        assertNotEquals(first, second)
        assertTrue(auth.canRead(second, ACCOUNT))
        assertFalse(auth.canRead(first, ACCOUNT))
    }

    @Test
    fun `group page is public and opening its child still needs a fresh bound grant`() {
        val nav = navigator(auth)
        auth.onForeground(true)
        nav.navigate(PasswordGroup(ACCOUNT, newEntryId()))
        val group = nav.current
        assertNull(group.protectedAccountId())
        assertFalse(auth.canRead(group.entryId, ACCOUNT))
        val detail = openAccount(nav, group.entryId)
        assertTrue(auth.canRead(detail, ACCOUNT))
        nav.goBack()
        assertFalse(auth.canRead(detail, ACCOUNT))
        assertFalse(auth.canRead(group.entryId, ACCOUNT))
        val second = openAccount(nav, group.entryId)
        assertNotEquals(detail, second)
        assertTrue(auth.canRead(second, ACCOUNT))
    }

    @Test
    fun `rotation keeps entry ids and so the grant of the open account`() {
        val stack = NavBackStack<Route>(HomeBase)
        val nav = AppNavigator(stack, auth).apply { reportDestination() }
        auth.onForeground(true)
        val detail = openAccount(nav, HomeBase.entryId)
        val restored =
            decodeFromSavedState(
                BackStackSerializer,
                encodeToSavedState(BackStackSerializer, stack),
            )
        assertEquals(stack.toList(), restored.toList())
        AppNavigator(restored, auth).reportDestination()
        assertTrue(auth.canRead(detail, ACCOUNT))
    }

    @Test
    fun `restored detail and editor routes have no grant and public create remains public`() {
        val stack = NavBackStack<Route>(HomeBase)
        val nav = AppNavigator(stack, auth).apply { reportDestination() }
        auth.onForeground(true)
        openAccount(nav, HomeBase.entryId)
        // A direct navigation is deliberately unauthorized.
        nav.navigate(PasswordEditor(ACCOUNT, newEntryId()))
        assertFalse(auth.canRead(nav.current.entryId, ACCOUNT))
        val saved = encodeToSavedState(BackStackSerializer, stack)
        // Process death: the activity-retained controller is new, the back stack is restored.
        val fresh = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })
        val restored = AppNavigator(decodeFromSavedState(BackStackSerializer, saved), fresh)
        restored.reportDestination()
        fresh.onForeground(true)
        assertFalse(fresh.canRead(restored.current.entryId, ACCOUNT))
        restored.goBack()
        assertFalse(fresh.canRead(restored.current.entryId, ACCOUNT))
        restored.goBack()
        assertEquals(HomeBase, restored.current)
        restored.navigate(PasswordEditor(-1L, newEntryId()))
        assertNull(restored.current.protectedAccountId())
    }

    @Test
    fun `back on the start destination keeps it`() {
        val nav = navigator(auth)
        nav.goBack()
        assertEquals(HomeBase, nav.current)
    }

    private fun openAccount(nav: AppNavigator, from: String): String {
        auth.request(Action.OpenAccount, from, ACCOUNT)
        auth.beginNativePrompt()?.let { auth.nativeResult(it.token, true) }
        val request = requireNotNull(auth.state.value.approvedOpen)
        auth.openApprovedAccount(request.token) { nav.navigate(PasswordDetail(it, newEntryId())) }
        return nav.current.entryId
    }

    private fun navigator(owner: EntryAuthenticationViewModel): AppNavigator =
        AppNavigator(NavBackStack<Route>(HomeBase), owner).apply { reportDestination() }

    private companion object {
        const val ACCOUNT = 42L
    }
}
