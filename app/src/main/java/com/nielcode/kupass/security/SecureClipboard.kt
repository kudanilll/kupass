package com.nielcode.kupass.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * Copies vault values to the clipboard. Sensitive values are flagged (API 33+ hides them from
 * previews and keyboards) and cleared after [CLEAR_AFTER_MILLIS] on every API level.
 *
 * Since Android 10, an app in the background can't read the clipboard. When the timer fires and
 * the clipboard can't be read, it's cleared anyway, because a password manager would rather clear a
 * newer clip than leave a password behind. If it can be read and holds something else, it's left
 * alone.
 */
object SecureClipboard {
    const val CLEAR_AFTER_MILLIS = 45_000L

    private val handler = Handler(Looper.getMainLooper())
    private var pendingClear: Runnable? = null

    fun copy(context: Context, label: String, text: String, sensitive: Boolean) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(label, text)
        if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
        clipboard.setPrimaryClip(clip)

        pendingClear?.let(handler::removeCallbacks)
        pendingClear = null
        if (sensitive) {
            val clear = Runnable { clearIfOurs(clipboard, text) }
            pendingClear = clear
            handler.postDelayed(clear, CLEAR_AFTER_MILLIS)
        }
    }

    private fun clearIfOurs(clipboard: ClipboardManager, copied: String) {
        pendingClear = null
        val current = runCatching { clipboard.primaryClip }.getOrNull()
        val readable = current != null && current.itemCount > 0
        if (readable && current.getItemAt(0).text?.toString() != copied) return // the user copied something else
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            // clearPrimaryClip() is API 28+; overwrite with an empty clip on 27.
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }
}
