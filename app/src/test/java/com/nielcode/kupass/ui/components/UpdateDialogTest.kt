package com.nielcode.kupass.ui.components

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.R
import com.nielcode.kupass.update.AppVersion
import com.nielcode.kupass.update.AvailableUpdate
import com.nielcode.kupass.update.UpdateState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UpdateDialogTest {

    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val update =
        AvailableUpdate(
            version = AppVersion(3, 1, 0, beta = 2),
            tag = "v3.1.0-beta.2",
            apkUrl = "https://example.com/kupass.apk",
            apkSize = 10,
            checksumUrl = "https://example.com/kupass.apk.sha256",
            pageUrl = "https://example.com/release",
        )
    private val calls = mutableListOf<String>()

    private fun show(state: UpdateState) = compose.setContent {
        UpdateDialog(
            state = state,
            onDownload = { calls += "download" },
            onLater = { calls += "later" },
            onClose = { calls += "close" },
            onCancel = { calls += "cancel" },
            onOpenSettings = { calls += "settings" },
            onOpenPage = { calls += "page $it" },
        )
    }

    private fun text(id: Int, vararg args: Any) = context.getString(id, *args)

    @Test
    fun `an available update names the version and offers Download or Later`() {
        show(UpdateState.Available(update))

        compose.onNodeWithText(text(R.string.update_available, "3.1.0-beta.2")).assertExists()
        compose.onNodeWithText(text(R.string.update_download)).performClick()
        compose.onNodeWithText(text(R.string.update_later)).performClick()
        assertEquals(listOf("download", "later"), calls)
    }

    @Test
    fun `a download in progress can be cancelled`() {
        show(UpdateState.Downloading(update, percent = 40))

        compose.onNodeWithText(text(R.string.update_downloading, "3.1.0-beta.2")).assertExists()
        compose.onNodeWithText(text(R.string.button_cancel)).performClick()
        assertEquals(listOf("cancel"), calls)
    }

    @Test
    fun `a missing install permission is explained and opens the setting`() {
        show(UpdateState.NeedsPermission(update, File("kupass-update.apk")))

        compose.onNodeWithText(text(R.string.update_permission)).assertExists()
        compose.onNodeWithText(text(R.string.update_open_settings)).performClick()
        compose.onNodeWithText(text(R.string.button_cancel)).performClick()
        assertEquals(listOf("settings", "close"), calls)
    }

    @Test
    fun `installing says that Kupass closes`() {
        show(UpdateState.Installing(update))

        compose.onNodeWithText(text(R.string.update_installing, "3.1.0-beta.2")).assertExists()
    }

    @Test
    fun `a failure offers Try again and the release page in the browser`() {
        show(UpdateState.Failed(update))

        compose.onNodeWithText(text(R.string.update_failed, "3.1.0-beta.2")).assertExists()
        compose.onNodeWithText(text(R.string.update_retry)).performClick()
        compose.onNodeWithText(text(R.string.update_open_browser)).performClick()
        assertEquals(listOf("download", "page https://example.com/release"), calls)
    }

    @Test
    fun `nothing is shown while idle`() {
        show(UpdateState.Idle)

        compose.onAllNodesWithText(text(R.string.update_title)).assertCountEquals(0)
        compose.onAllNodesWithText(text(R.string.update_failed_title)).assertCountEquals(0)
    }
}
