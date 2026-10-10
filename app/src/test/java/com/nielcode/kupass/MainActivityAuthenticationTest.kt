package com.nielcode.kupass

import android.app.KeyguardManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.nielcode.kupass.security.AppLock
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.testing.RecordingBiometricPromptShadow
import java.time.Duration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = "w411dp-h891dp-xxhdpi",
    shadows = [RecordingBiometricPromptShadow::class],
    instrumentedPackages = ["androidx.biometric"],
)
class MainActivityAuthenticationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val activities = mutableListOf<ActivityController<MainActivity>>()

    @Before
    fun prepareDevice() {
        RecordingBiometricPromptShadow.reset()
        val app = RuntimeEnvironment.getApplication() as App
        app.container.preferenceManager.autoLockSeconds = AppLock.DEFAULT_TIMEOUT_SECONDS
        shadowOf(app.getSystemService(KeyguardManager::class.java)).setIsDeviceSecure(true)
    }

    @After
    fun destroyActivities() {
        activities.reversed().forEach { controller ->
            if (controller.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                controller.pause()
            if (controller.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
                controller.stop()
            controller.destroy()
        }
        RecordingBiometricPromptShadow.reset()
    }

    @Test
    fun `activity B starting and cancelling native auth cannot revive expired activity A grants`() {
        assertIndependentAbsenceTimers(succeededInB = false)
    }

    @Test
    fun `activity B successfully authenticating cannot revive expired activity A grants`() {
        assertIndependentAbsenceTimers(succeededInB = true)
    }

    @Test
    fun `production activity retains pending native prompt across recreation without another authenticate`() {
        (RuntimeEnvironment.getApplication() as App).container.preferenceManager.autoLockSeconds = 0
        val controller = launch()
        val original = authentication(controller)
        requestExport(controller)
        val retainedSession = RecordingBiometricPromptShadow.sessions.single()
        val token = requireNotNull(original.state.value.nativeRequest).token
        assertEquals(1, RecordingBiometricPromptShadow.sessions.size)
        controller.recreate()
        idle()
        assertSame(original, authentication(controller))
        assertEquals(token, authentication(controller).state.value.nativeRequest?.token)
        assertEquals(1, RecordingBiometricPromptShadow.sessions.size)
        val reattached = RecordingBiometricPromptShadow.attachments.last()
        assertSame(controller.get(), reattached)
        retainedSession.succeed()
        idle()
        assertSame(controller.get(), retainedSession.deliveredTo)
        assertEquals(1, retainedSession.deliveries)
        assertNotNull(authentication(controller).state.value.export)
        compose
            .onNodeWithText(controller.get().getString(R.string.backup_export_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun `production native cancel drains its session and retired events cannot authorize a new export`() {
        val controller = launch()
        val auth = authentication(controller)
        val dialogTitle = controller.get().getString(R.string.backup_export_dialog_title)
        assertTrue(RecordingBiometricPromptShadow.sessions.isEmpty())
        requestExport(controller)
        val old = RecordingBiometricPromptShadow.sessions.last()
        assertEquals(
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
            RecordingBiometricPromptShadow.prompts.last().allowedAuthenticators,
        )
        compose.onNodeWithText(dialogTitle).assertDoesNotExist()
        old.fail()
        idle()
        assertNull(auth.state.value.export)
        auth.cancelPending()
        idle()
        assertTrue(RecordingBiometricPromptShadow.cancellations > 0)
        old.cancel()
        idle()
        assertNull(auth.state.value.export)
        compose.onNodeWithText(dialogTitle).assertDoesNotExist()
        requestExport(controller)
        val fresh = RecordingBiometricPromptShadow.sessions.last()
        assertEquals(2, RecordingBiometricPromptShadow.sessions.size)
        val freshToken = requireNotNull(auth.state.value.nativeRequest).token
        old.succeed()
        idle()
        assertEquals(1, old.ignoredEvents)
        assertEquals(2, old.deliveries) // Non-terminal failure, then terminal cancellation.
        assertNull(auth.state.value.export)
        assertEquals(freshToken, auth.state.value.nativeRequest?.token)
        compose.onNodeWithText(dialogTitle).assertDoesNotExist()
        fresh.succeed()
        idle()
        assertNotNull(auth.state.value.export)
        compose.onNodeWithText(dialogTitle).assertIsDisplayed()
    }

    @Test
    fun `cancelled retained session completes through current callback after rotation without granting export`() {
        val controller = launch()
        val auth = authentication(controller)
        requestExport(controller)
        val retained = RecordingBiometricPromptShadow.sessions.single()
        val oldToken = requireNotNull(auth.state.value.nativeRequest).token
        auth.cancelPending()
        idle()
        assertTrue(retained.cancellationRequested)
        controller.recreate()
        idle()
        assertSame(auth, authentication(controller))
        assertEquals(oldToken, auth.state.value.nativeRequest?.token)
        val source = requireNotNull(ReflectionHelpers.getField<String?>(auth, "destination"))
        auth.request(Action.Export, source)
        idle()
        assertNull(auth.state.value.pending)
        assertEquals(1, RecordingBiometricPromptShadow.sessions.size)
        retained.succeed() // A racing completion must use the newly attached activity's callback.
        idle()
        assertSame(controller.get(), retained.deliveredTo)
        assertEquals(1, retained.deliveries)
        assertNull(auth.state.value.nativeRequest)
        assertNull(auth.state.value.export)
        val title = controller.get().getString(R.string.backup_export_dialog_title)
        compose.onNodeWithText(title).assertDoesNotExist()
        requestExport(controller)
        val fresh = RecordingBiometricPromptShadow.sessions.last()
        val freshToken = requireNotNull(auth.state.value.nativeRequest).token
        retained.succeed()
        idle()
        assertEquals(1, retained.ignoredEvents)
        assertEquals(freshToken, auth.state.value.nativeRequest?.token)
        assertNull(auth.state.value.export)
        compose.onNodeWithText(title).assertDoesNotExist()
        fresh.succeed()
        idle()
        assertNotNull(auth.state.value.export)
        compose.onNodeWithText(title).assertIsDisplayed()
    }

    private fun assertIndependentAbsenceTimers(succeededInB: Boolean) {
        val first = launch()
        val authA = authentication(first)
        authA.setDestination("activity-a-detail", ACCOUNT)
        authA.request(Action.UnlockDestination, "activity-a-detail", ACCOUNT)
        idle()
        RecordingBiometricPromptShadow.sessions.last().succeed()
        idle()
        assertTrue(authA.canRead("activity-a-detail", ACCOUNT))
        first.pause().stop()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(AppLock.DEFAULT_TIMEOUT_SECONDS.toLong()))
        val second = launch()
        assertSame(
            (first.get().application as App).container,
            (second.get().application as App).container,
        )
        assertNotSame(authA, authentication(second))
        requestExport(second)
        val sessionB = RecordingBiometricPromptShadow.sessions.last()
        if (succeededInB) sessionB.succeed() else sessionB.cancel()
        idle()
        second.pause().stop()
        first.start().resume().visible()
        idle()
        assertFalse(authA.canRead("activity-a-detail", ACCOUNT))
        assertTrue(authA.state.value.grants.isEmpty())
    }

    @Test
    fun `immediate timeout revokes entry access on a launcher return without stop or start`() {
        (RuntimeEnvironment.getApplication() as App).container.preferenceManager.autoLockSeconds = 0
        val controller = launch()
        val auth = authentication(controller)
        auth.setDestination("rapid-return-detail", ACCOUNT)
        auth.request(Action.UnlockDestination, "rapid-return-detail", ACCOUNT)
        idle()
        RecordingBiometricPromptShadow.sessions.last().succeed()
        idle()
        assertTrue(auth.canRead("rapid-return-detail", ACCOUNT))

        ReflectionHelpers.callInstanceMethod<Void>(controller.get(), "onUserLeaveHint")
        controller.pause()
        ShadowSystemClock.advanceBy(Duration.ofMillis(400))
        controller.resume()
        idle()

        assertFalse(auth.canRead("rapid-return-detail", ACCOUNT))
        assertTrue(auth.state.value.grants.isEmpty())
    }

    @Test
    fun `native credential departure keeps its single exemption across leave hint and stop`() {
        (RuntimeEnvironment.getApplication() as App).container.preferenceManager.autoLockSeconds = 0
        val controller = launch()
        val auth = authentication(controller)
        requestExport(controller)
        val session = RecordingBiometricPromptShadow.sessions.single()

        ReflectionHelpers.callInstanceMethod<Void>(controller.get(), "onUserLeaveHint")
        controller.pause().stop()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        controller.start().resume().visible()
        idle()
        session.succeed()
        idle()
        assertNotNull(auth.state.value.export)

        ReflectionHelpers.callInstanceMethod<Void>(controller.get(), "onUserLeaveHint")
        controller.pause().resume()
        idle()
        assertNull(auth.state.value.export)
    }

    private fun requestExport(controller: ActivityController<MainActivity>) {
        val auth = authentication(controller)
        // Read the real NavHost source ID; native results are delivered through MainActivity
        // callbacks.
        val source = requireNotNull(ReflectionHelpers.getField<String?>(auth, "destination"))
        auth.request(Action.Export, source)
        idle()
        assertNotNull(auth.state.value.nativeRequest)
    }

    private fun authentication(
        controller: ActivityController<MainActivity>
    ): EntryAuthenticationViewModel =
        ViewModelProvider(controller.get())[EntryAuthenticationViewModel::class.java]

    private fun launch(): ActivityController<MainActivity> =
        Robolectric.buildActivity(MainActivity::class.java).setup().visible().also {
            activities += it
            idle()
        }

    private fun idle() {
        // ActivityController dialogs need frame queues and a draw pass to finish pending layout.
        repeat(3) { _ ->
            compose.mainClock.advanceTimeByFrame()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            ShadowDialog.getLatestDialog()
                ?.takeIf { it.isShowing }
                ?.window
                ?.decorView
                ?.let { decor ->
                    val metrics = decor.resources.displayMetrics
                    decor.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            metrics.widthPixels,
                            View.MeasureSpec.AT_MOST,
                        ),
                        View.MeasureSpec.makeMeasureSpec(
                            metrics.heightPixels,
                            View.MeasureSpec.AT_MOST,
                        ),
                    )
                    decor.layout(0, 0, decor.measuredWidth, decor.measuredHeight)
                    val bitmap =
                        Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                    try {
                        decor.draw(Canvas(bitmap))
                    } finally {
                        bitmap.recycle()
                    }
                }
        }
        compose.waitForIdle()
    }

    private companion object {
        const val ACCOUNT = 42L
    }
}
