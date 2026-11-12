package com.nielcode.kupass.update

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import java.io.File
import java.io.IOException

/** What [UpdateManager] needs from the system installer. */
internal interface PackageUpdater {
    /** "Install unknown apps" is allowed for Kupass; Android asks the user once. */
    fun canInstall(): Boolean

    /** Hands [apk] to the system installer and deletes it; false if the system refused it. */
    fun install(apk: File): Boolean
}

/**
 * Installs a downloaded update through a [PackageInstaller] session that may only update Kupass
 * itself. The result arrives in [UpdateInstallReceiver].
 */
internal class UpdateInstaller(private val context: Context) : PackageUpdater {

    override fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun install(apk: File): Boolean {
        val handedOver =
            apk.isFile &&
                try {
                    commit(apk)
                } catch (_: IOException) {
                    false
                } catch (_: SecurityException) {
                    false
                } catch (_: IllegalStateException) {
                    // For example, too many open sessions.
                    false
                }
        apk.delete()
        return handedOver
    }

    /** Writes [apk] into a new session and commits it; the session is abandoned if that fails. */
    private fun commit(apk: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val sessionId = installer.createSession(sessionParams(context.packageName))
        var committed = false
        try {
            installer.openSession(sessionId).use { session ->
                write(session, apk)
                session.commit(statusSender(sessionId))
            }
            committed = true
        } finally {
            if (!committed) installer.abandonSession(sessionId)
        }
        return committed
    }

    private fun write(session: PackageInstaller.Session, apk: File) {
        apk.inputStream().use { input ->
            session.openWrite(SESSION_APK, 0, apk.length()).use { output ->
                input.copyTo(output)
                session.fsync(output)
            }
        }
    }

    /** The system fills in the status, so the PendingIntent is mutable, with an explicit target. */
    private fun statusSender(sessionId: Int): IntentSender {
        val mutable =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val intent = Intent(context, UpdateInstallReceiver::class.java)
        return PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
            .intentSender
    }

    companion object {
        private const val SESSION_APK = "kupass.apk"

        /** The system screen where the user allows Kupass to install apps. */
        fun permissionIntent(context: Context): Intent =
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${context.packageName}".toUri(),
            )

        /**
         * A full install of [packageName] only; on Android 12+ without asking, as a self-update.
         */
        fun sessionParams(packageName: String): PackageInstaller.SessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                // The session fails if the APK is any other app.
                setAppPackageName(packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
    }
}

/** Routes an install session status: shows the system confirmation, or reports a failure. */
internal fun handleInstallStatus(context: Context, intent: Intent, onFailed: () -> Unit) {
    when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
        PackageInstaller.STATUS_SUCCESS -> Unit
        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
            val confirm =
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
            try {
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    ?: onFailed()
            } catch (_: ActivityNotFoundException) {
                onFailed()
            }
        }
        else -> onFailed()
    }
}
