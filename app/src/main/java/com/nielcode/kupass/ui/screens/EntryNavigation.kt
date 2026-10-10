package com.nielcode.kupass.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.ui.screens.detail.PasswordDetailScreen
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.lock.LockScreen

internal const val SCREEN_TRANSITION_MILLIS = 400

internal fun NavGraphBuilder.accountRoutes(
    navController: NavHostController,
    authentication: EntryAuthenticationViewModel,
    onOpenSecuritySettings: () -> Unit,
) {
    fullScreenRoute<PasswordDetail> { backStackEntry ->
        val id = backStackEntry.toRoute<PasswordDetail>().passwordId
        AccountDestination(
            entryId = backStackEntry.id,
            accountId = id,
            authentication = authentication,
            onOpenSecuritySettings = onOpenSecuritySettings,
            onCancel = {
                authentication.cancelPending()
                navController.popBackStack()
            },
        ) {
            PasswordDetailScreen(
                passwordId = id,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { accountId ->
                    authentication.openEditor(backStackEntry.id, accountId) {
                        navController.navigate(PasswordEditor(passwordId = accountId))
                        navController.currentBackStackEntry?.id
                    }
                },
            )
        }
    }
    fullScreenRoute<PasswordEditor> { backStackEntry ->
        val id = backStackEntry.toRoute<PasswordEditor>().passwordId
        if (id <= 0) {
            PasswordEditorScreen(passwordId = id, onNavigateBack = { navController.popBackStack() })
        } else {
            AccountDestination(
                entryId = backStackEntry.id,
                accountId = id,
                authentication = authentication,
                onOpenSecuritySettings = onOpenSecuritySettings,
                onCancel = {
                    authentication.cancelPending()
                    navController.popBackStack()
                },
            ) {
                PasswordEditorScreen(
                    passwordId = id,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}

internal fun NavBackStackEntry.protectedAccountId(): Long? =
    when {
        destination.hasRoute<PasswordDetail>() -> toRoute<PasswordDetail>().passwordId
        destination.hasRoute<PasswordEditor>() ->
            toRoute<PasswordEditor>().passwordId.takeIf { it > 0 }
        else -> null
    }

/** Invoke protected screen content, including its default ViewModel, only inside a valid grant. */
@Composable
internal fun AccountDestination(
    entryId: String,
    accountId: Long,
    authentication: EntryAuthenticationViewModel,
    onOpenSecuritySettings: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state by authentication.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize()) {
        if (state.grants[entryId] == accountId && authentication.canRead(entryId, accountId)) {
            content()
        } else {
            LockScreen(
                deviceSecure = state.deviceSecure,
                onUnlock = { authentication.request(Action.UnlockDestination, entryId, accountId) },
                onOpenSecuritySettings = onOpenSecuritySettings,
                onCancel = onCancel,
            )
        }
    }
}

/** Sensitive routes slide over the public pager and slide down when popped. */
private inline fun <reified T : Any> NavGraphBuilder.fullScreenRoute(
    crossinline content: @Composable (NavBackStackEntry) -> Unit
) =
    composable<T>(
        enterTransition = { slideUpEnter() },
        popExitTransition = { slideDownExit() },
    ) { backStackEntry ->
        content(backStackEntry)
    }

private fun slideUpEnter(): EnterTransition =
    slideInVertically(tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(SCREEN_TRANSITION_MILLIS))

private fun slideDownExit(): ExitTransition =
    slideOutVertically(tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } +
        fadeOut(tween(SCREEN_TRANSITION_MILLIS))
