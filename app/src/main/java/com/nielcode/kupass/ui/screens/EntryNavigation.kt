package com.nielcode.kupass.ui.screens

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.ui.screens.detail.PasswordDetailScreen
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.lock.LockScreen
import java.util.UUID
import kotlinx.serialization.serializer

internal const val SCREEN_TRANSITION_MILLIS = 400

/** Saves routes with their entry IDs, so rotation keeps each entry and its grant. */
internal val BackStackSerializer = NavBackStackSerializer(serializer<Route>())

/** A new identity for every navigation, so a grant never carries over to a later visit. */
internal fun newEntryId(): String = UUID.randomUUID().toString()

/**
 * The only writer of the back stack. Each change reports the new top entry to [authentication]
 * before returning, as Navigation 2's destination listener did, so a grant can bind to the entry
 * that was just created.
 */
internal class AppNavigator(
    private val backStack: MutableList<Route>,
    private val authentication: EntryAuthenticationViewModel,
) {
    val current: Route
        get() = backStack.last()

    /** @return the new entry's ID. */
    fun navigate(route: Route): String {
        backStack.add(route)
        reportDestination()
        return route.entryId
    }

    /** Never pops the start destination; leaving the app from it is the activity's job. */
    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        reportDestination()
    }

    /** Also called once for a restored back stack. */
    fun reportDestination() {
        authentication.setDestination(current.entryId, current.protectedAccountId())
    }
}

internal fun Route.protectedAccountId(): Long? =
    when (this) {
        is PasswordDetail -> passwordId
        is PasswordEditor -> passwordId.takeIf { it > 0 }
        HomeBase,
        is PasswordGroup -> null
    }

internal fun EntryProviderScope<Route>.accountEntries(
    navigator: AppNavigator,
    authentication: EntryAuthenticationViewModel,
    onOpenSecuritySettings: () -> Unit,
) {
    val cancel = {
        authentication.cancelPending()
        navigator.goBack()
    }
    entry<PasswordDetail>(clazzContentKey = { it.entryId }, metadata = FullScreen) { route ->
        AccountDestination(
            entryId = route.entryId,
            accountId = route.passwordId,
            authentication = authentication,
            onOpenSecuritySettings = onOpenSecuritySettings,
            onCancel = cancel,
        ) {
            PasswordDetailScreen(
                passwordId = route.passwordId,
                onNavigateBack = navigator::goBack,
                onNavigateToEdit = { accountId ->
                    authentication.openEditor(route.entryId, accountId) {
                        navigator.navigate(PasswordEditor(accountId, newEntryId()))
                    }
                },
            )
        }
    }
    entry<PasswordEditor>(clazzContentKey = { it.entryId }, metadata = FullScreen) { route ->
        if (route.passwordId <= 0) {
            PasswordEditorScreen(passwordId = route.passwordId, onNavigateBack = navigator::goBack)
        } else {
            AccountDestination(
                entryId = route.entryId,
                accountId = route.passwordId,
                authentication = authentication,
                onOpenSecuritySettings = onOpenSecuritySettings,
                onCancel = cancel,
            ) {
                PasswordEditorScreen(
                    passwordId = route.passwordId,
                    onNavigateBack = navigator::goBack,
                )
            }
        }
    }
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

// The app's only screen transitions: public pages cross-fade, and account pages slide up over
// them and slide back down when popped.

/** Public pages: Home and the account list of a site. */
internal fun crossFade(): ContentTransform =
    fadeIn(tween(SCREEN_TRANSITION_MILLIS)) togetherWith fadeOut(tween(SCREEN_TRANSITION_MILLIS))

private fun slideUp(): ContentTransform =
    (slideInVertically(tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(SCREEN_TRANSITION_MILLIS))) togetherWith
        fadeOut(tween(SCREEN_TRANSITION_MILLIS))

private fun slideDown(): ContentTransform =
    fadeIn(tween(SCREEN_TRANSITION_MILLIS)) togetherWith
        (slideOutVertically(tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } +
            fadeOut(tween(SCREEN_TRANSITION_MILLIS)))

/** Account detail and editor. */
private val FullScreen = metadata {
    put(NavDisplay.TransitionKey) { slideUp() }
    put(NavDisplay.PopTransitionKey) { slideDown() }
    put(NavDisplay.PredictivePopTransitionKey) { slideDown() }
}
