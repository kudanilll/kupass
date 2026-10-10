package com.nielcode.kupass.ui.screens

import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EntryNavigationTest {
    private val auth = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })

    @Test
    fun `actual navigation binds grants to new entry ids and preserves detail editor back`() {
        val nav = controller(auth)
        val home = requireNotNull(nav.currentBackStackEntry).id
        auth.onForeground(true)
        val first = openAccount(nav, home)
        assertTrue(auth.canRead(first, ACCOUNT))
        auth.openEditor(first, ACCOUNT) {
            nav.navigate(PasswordEditor(ACCOUNT))
            nav.currentBackStackEntry?.id
        }
        val editor = requireNotNull(nav.currentBackStackEntry).id
        assertTrue(auth.canRead(editor, ACCOUNT))
        nav.popBackStack()
        assertTrue(auth.canRead(first, ACCOUNT))
        nav.popBackStack()
        assertFalse(auth.canRead(first, ACCOUNT))
        val second = openAccount(nav, home)
        assertNotEquals(first, second)
        assertTrue(auth.canRead(second, ACCOUNT))
        assertFalse(auth.canRead(first, ACCOUNT))
    }

    @Test
    fun `restored detail and editor routes have no grant and public create remains public`() {
        val nav = controller(auth)
        auth.onForeground(true)
        val home = requireNotNull(nav.currentBackStackEntry).id
        openAccount(nav, home)
        nav.navigate(PasswordEditor(ACCOUNT)) // A direct navigation is deliberately unauthorized.
        val saved = nav.saveState()
        val fresh = EntryAuthenticationViewModel({ 30_000L }, { 1_000L })
        val restored = newController()
        restored.restoreState(saved)
        installGraph(restored, fresh)
        fresh.onForeground(true)
        val editor = requireNotNull(restored.currentBackStackEntry)
        assertFalse(fresh.canRead(editor.id, ACCOUNT))
        restored.popBackStack()
        val detail = requireNotNull(restored.currentBackStackEntry)
        assertFalse(fresh.canRead(detail.id, ACCOUNT))
        restored.popBackStack()
        restored.navigate(PasswordEditor())
        assertTrue(restored.currentBackStackEntry?.protectedAccountId() == null)
    }

    private fun openAccount(nav: NavHostController, home: String): String {
        auth.request(Action.OpenAccount, home, ACCOUNT)
        val request = requireNotNull(auth.beginNativePrompt())
        auth.nativeResult(request.token, true)
        auth.openApprovedAccount(request.token) {
            nav.navigate(PasswordDetail(it))
            nav.currentBackStackEntry?.id
        }
        return requireNotNull(nav.currentBackStackEntry).id
    }

    private fun controller(owner: EntryAuthenticationViewModel): NavHostController =
        newController().also { installGraph(it, owner) }

    private fun newController(): NavHostController =
        NavHostController(RuntimeEnvironment.getApplication()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            setViewModelStore(ViewModelStore())
        }

    private fun installGraph(nav: NavHostController, owner: EntryAuthenticationViewModel) {
        nav.addOnDestinationChangedListener(
            NavController.OnDestinationChangedListener { controller, _, _ ->
                controller.currentBackStackEntry?.let { entry ->
                    owner.setDestination(entry.id, entry.protectedAccountId())
                }
            }
        )
        nav.graph =
            nav.createGraph(startDestination = HomeBase) {
                composable<HomeBase> {}
                composable<PasswordDetail> {}
                composable<PasswordEditor> {}
            }
    }

    private companion object {
        const val ACCOUNT = 42L
    }
}
