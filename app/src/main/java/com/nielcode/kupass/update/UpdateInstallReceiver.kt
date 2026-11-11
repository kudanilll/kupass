package com.nielcode.kupass.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nielcode.kupass.App

/**
 * Receives install session statuses. Not exported: only the system, through the session's own
 * PendingIntent, can deliver them.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updates = (context.applicationContext as App).container.updates
        handleInstallStatus(context, intent, onFailed = updates::onInstallFailed)
    }
}
