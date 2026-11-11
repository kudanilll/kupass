package com.nielcode.kupass.ui.components

import com.nielcode.kupass.R
import com.nielcode.kupass.ui.components.UpdateAction.Cancel
import com.nielcode.kupass.ui.components.UpdateAction.Close
import com.nielcode.kupass.ui.components.UpdateAction.Download
import com.nielcode.kupass.ui.components.UpdateAction.Later
import com.nielcode.kupass.ui.components.UpdateAction.OpenPage
import com.nielcode.kupass.ui.components.UpdateAction.OpenSettings
import com.nielcode.kupass.update.AppVersion
import com.nielcode.kupass.update.AvailableUpdate
import com.nielcode.kupass.update.UpdateState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Plain JVM: Robolectric Compose tests need a sandbox that can fail to start (EN-4.5). */
class UpdateDialogContentTest {

    private val update =
        AvailableUpdate(
            version = AppVersion(3, 1, 0, beta = 2),
            tag = "v3.1.0-beta.2",
            apkUrl = "https://example.com/kupass.apk",
            apkSize = 10,
            checksumUrl = "https://example.com/kupass.apk.sha256",
            pageUrl = "https://example.com/release",
        )

    @Test
    fun `nothing is shown while idle`() {
        assertNull(updateDialogContent(UpdateState.Idle))
    }

    @Test
    fun `an available update names the version and offers Download or Later`() {
        assertEquals(
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_available,
                confirm = UpdateButton(R.string.update_download, Download),
                dismiss = UpdateButton(R.string.update_later, Later),
                onDismissRequest = Close,
            ),
            updateDialogContent(UpdateState.Available(update)),
        )
    }

    @Test
    fun `a download shows its progress, and back or Cancel stops it`() {
        assertEquals(
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_downloading,
                confirm = UpdateButton(R.string.button_cancel, Cancel),
                onDismissRequest = Cancel,
                progress = 0.4f,
            ),
            updateDialogContent(UpdateState.Downloading(update, percent = 40)),
        )
    }

    @Test
    fun `a missing install permission is explained and opens the setting`() {
        assertEquals(
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_permission,
                namesVersion = false,
                confirm = UpdateButton(R.string.update_open_settings, OpenSettings),
                dismiss = UpdateButton(R.string.button_cancel, Close),
                onDismissRequest = Close,
            ),
            updateDialogContent(UpdateState.NeedsPermission(update, File("kupass-update.apk"))),
        )
    }

    @Test
    fun `installing has no buttons and a busy bar`() {
        assertEquals(
            UpdateDialogContent(
                title = R.string.update_title,
                text = R.string.update_installing,
                onDismissRequest = Close,
                busy = true,
            ),
            updateDialogContent(UpdateState.Installing(update)),
        )
    }

    @Test
    fun `a failure offers Try again and the release page in the browser`() {
        assertEquals(
            UpdateDialogContent(
                title = R.string.update_failed_title,
                text = R.string.update_failed,
                confirm = UpdateButton(R.string.update_retry, Download),
                dismiss = UpdateButton(R.string.update_open_browser, OpenPage),
                onDismissRequest = Close,
            ),
            updateDialogContent(UpdateState.Failed(update)),
        )
    }
}
