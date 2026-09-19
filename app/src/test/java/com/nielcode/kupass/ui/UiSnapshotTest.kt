package com.nielcode.kupass.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.ui.screens.data.DataScreen
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import com.nielcode.kupass.ui.theme.KupassTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders key screens on the JVM and writes PNGs to `app/build/ui-snapshots/` for visual review
 * (device screenshots are blank because of FLAG_SECURE). Doubles as a smoke test that each screen
 * composes without crashing.
 *
 * Opt-in, in its own run: `./gradlew :app:testDebugUnitTest -Pkupass.snapshots --tests
 * "*UiSnapshotTest"`
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// SDK 35: Espresso (used by compose-ui-test) does not support SDK 37 yet.
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class UiSnapshotTest {

    @get:Rule val compose = createComposeRule()

    private val sample =
        listOf(
            PasswordEntity(
                id = 1,
                siteName = "GitHub",
                username = "kudanilll",
                password = "x",
                url = "https://github.com",
            ),
            PasswordEntity(
                id = 2,
                siteName = "Google",
                username = "danil@gmail.com",
                password = "x",
            ),
            PasswordEntity(
                id = 3,
                siteName = "Netflix",
                username = "",
                password = "x",
                url = "netflix.com",
            ),
        )

    @Test fun homeWithEntries() = snapshot("home") { home(sample) }

    @Test fun homeEmpty() = snapshot("home_empty") { home(emptyList()) }

    @Test fun data() = snapshot("data") { DataScreen(onExportClick = {}, onImportClick = {}) }

    @Test fun settings() = snapshot("settings") { SettingsScreen() }

    @Composable
    private fun home(passwords: List<PasswordEntity>) =
        HomeScreen(
            passwords = passwords,
            searchQuery = "",
            onSearchQueryChange = {},
            onDeletePassword = {},
            onNavigateToDetail = {},
        )

    private fun snapshot(name: String, content: @Composable () -> Unit) {
        compose.setContent { KupassTheme(dynamicColor = false) { content() } }
        compose.waitForIdle()
        val dir = File("build/ui-snapshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
