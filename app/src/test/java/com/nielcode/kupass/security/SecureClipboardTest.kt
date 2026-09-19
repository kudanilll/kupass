package com.nielcode.kupass.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Looper
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SecureClipboardTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val clipboard = context.getSystemService(ClipboardManager::class.java)

    private fun text(): String? =
        clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

    private fun advance(millis: Long) =
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test
    @Config(sdk = [33])
    fun `sensitive copy is flagged and cleared after the timeout on API 33`() {
        SecureClipboard.copy(context, "Password", "hunter2", sensitive = true)

        assertEquals("hunter2", text())
        assertTrue(
            clipboard.primaryClip!!
                .description
                .extras!!
                .getBoolean(ClipDescription.EXTRA_IS_SENSITIVE)
        )
        advance(SecureClipboard.CLEAR_AFTER_MILLIS - 1)
        assertEquals("hunter2", text())
        advance(1)
        assertTrue(text().isNullOrEmpty())
    }

    @Test
    @Config(sdk = [27])
    fun `sensitive copy is cleared on API 27 without clearPrimaryClip`() {
        SecureClipboard.copy(context, "Password", "hunter2", sensitive = true)

        advance(SecureClipboard.CLEAR_AFTER_MILLIS)

        assertTrue(text().isNullOrEmpty())
    }

    @Test
    fun `non-sensitive copy is not cleared`() {
        SecureClipboard.copy(context, "Username", "alice", sensitive = false)

        advance(SecureClipboard.CLEAR_AFTER_MILLIS * 2)

        assertEquals("alice", text())
    }

    @Test
    fun `a newer clip from the user is left alone`() {
        SecureClipboard.copy(context, "Password", "hunter2", sensitive = true)
        clipboard.setPrimaryClip(ClipData.newPlainText("note", "grocery list"))

        advance(SecureClipboard.CLEAR_AFTER_MILLIS)

        assertEquals("grocery list", text())
    }

    @Test
    fun `copying again restarts the timer`() {
        SecureClipboard.copy(context, "Password", "first", sensitive = true)
        advance(SecureClipboard.CLEAR_AFTER_MILLIS - 5_000)
        SecureClipboard.copy(context, "Password", "second", sensitive = true)

        advance(10_000)
        assertEquals("second", text())
        advance(SecureClipboard.CLEAR_AFTER_MILLIS)
        assertTrue(text().isNullOrEmpty())
    }
}
