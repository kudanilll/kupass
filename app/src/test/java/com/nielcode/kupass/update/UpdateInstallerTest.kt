package com.nielcode.kupass.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

// Default config on purpose: it reuses the sandbox most app tests share. A test that opens a new
// sandbox early can fail copying fonts in Robolectric 4.17 (EN-4.5). The pre-Android 12 path, with
// the system confirmation, was verified on an API 28 emulator instead.
@RunWith(RobolectricTestRunner::class)
class UpdateInstallerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageInstaller = context.packageManager.packageInstaller
    private val finished = mutableListOf<Boolean>()

    init {
        packageInstaller.registerSessionCallback(
            object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {}

                override fun onBadgingChanged(sessionId: Int) {}

                override fun onActiveChanged(sessionId: Int, active: Boolean) {}

                override fun onProgressChanged(sessionId: Int, progress: Float) {}

                override fun onFinished(sessionId: Int, success: Boolean) {
                    finished += success
                }
            }
        )
    }

    private fun apk(): File =
        File.createTempFile("kupass", ".apk").apply { writeBytes(ByteArray(2048) { it.toByte() }) }

    @Test
    fun `the APK is written to a committed session and then deleted`() {
        val apk = apk()

        assertTrue(UpdateInstaller(context).install(apk))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(true), finished)
        assertFalse(apk.exists())
    }

    @Test
    fun `a missing APK is refused`() {
        assertFalse(UpdateInstaller(context).install(File(context.cacheDir, "missing.apk")))
    }

    @Test
    fun `the session may only update Kupass, and asks for no user action as a self-update`() {
        val params = UpdateInstaller.sessionParams("com.nielcode.kupass")

        assertEquals(
            "com.nielcode.kupass",
            ReflectionHelpers.getField<String>(params, "appPackageName"),
        )
        assertEquals(
            PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED,
            ReflectionHelpers.getField<Int>(params, "requireUserAction"),
        )
    }

    @Test
    fun `the install permission comes from the system setting`() {
        shadowOf(context.packageManager).setCanRequestPackageInstalls(false)
        assertFalse(UpdateInstaller(context).canInstall())

        shadowOf(context.packageManager).setCanRequestPackageInstalls(true)
        assertTrue(UpdateInstaller(context).canInstall())
    }

    @Test
    fun `the permission screen is the unknown-apps setting for Kupass`() {
        val intent = UpdateInstaller.permissionIntent(context)

        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals(Uri.parse("package:com.nielcode.kupass"), intent.data)
    }

    @Test
    fun `a session that needs the user shows the system confirmation`() {
        val confirm = Intent("android.content.pm.action.CONFIRM_INSTALL")
        var failed = false

        handleInstallStatus(
            context,
            status(PackageInstaller.STATUS_PENDING_USER_ACTION)
                .putExtra(Intent.EXTRA_INTENT, confirm),
            onFailed = { failed = true },
        )

        val started = shadowOf(context as android.app.Application).nextStartedActivity
        assertEquals(confirm.action, started.action)
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertFalse(failed)
    }

    @Test
    fun `failures, and a confirmation without its screen, are reported`() {
        listOf(
                status(PackageInstaller.STATUS_FAILURE_ABORTED),
                status(PackageInstaller.STATUS_FAILURE_CONFLICT),
                status(PackageInstaller.STATUS_PENDING_USER_ACTION),
                Intent(),
            )
            .forEach { intent ->
                var failed = false
                handleInstallStatus(context, intent, onFailed = { failed = true })
                assertTrue(intent.toString(), failed)
            }
    }

    @Test
    fun `success needs nothing because Kupass is being replaced`() {
        var failed = false

        handleInstallStatus(context, status(PackageInstaller.STATUS_SUCCESS)) { failed = true }

        assertFalse(failed)
        assertNull(shadowOf(context as android.app.Application).nextStartedActivity)
    }

    private fun status(code: Int) = Intent().putExtra(PackageInstaller.EXTRA_STATUS, code)
}
