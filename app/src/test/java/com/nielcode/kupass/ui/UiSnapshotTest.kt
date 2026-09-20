package com.nielcode.kupass.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.App
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.security.CryptoManager
import com.nielcode.kupass.ui.components.BottomNav
import com.nielcode.kupass.ui.components.MainTab
import com.nielcode.kupass.ui.screens.data.DataScreen
import com.nielcode.kupass.ui.screens.detail.PasswordDetailScreen
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import com.nielcode.kupass.ui.theme.KupassTheme
import java.io.File
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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

    @Test
    fun homeWithSiteIcons() =
        snapshot("home_site_icons") { home(sample, siteIcons = FakeSiteIcons) }

    @Test fun homeEmpty() = snapshot("home_empty") { home(emptyList()) }

    @Test
    fun homeNoResults() =
        snapshot("home_no_results") {
            HomeScreen(
                passwords = emptyList(),
                searchQuery = "bank",
                onSearchQueryChange = {},
                onDeletePassword = {},
                onNavigateToDetail = {},
                onAddPassword = {},
            )
        }

    @Test fun data() = snapshot("data") { DataScreen(onExportClick = {}, onImportClick = {}) }

    @Test fun settings() = snapshot("settings") { SettingsScreen() }

    @Test
    fun editorNew() =
        snapshot("editor") { PasswordEditorScreen(passwordId = -1, onNavigateBack = {}) }

    @Test
    fun detail() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        CryptoManager.setKeyProviderForTesting { key }
        val app = RuntimeEnvironment.getApplication() as App
        val id = runBlocking {
            app.container.passwordRepository.insertPassword(
                PasswordEntity(
                    siteName = "GitHub",
                    username = "kudanilll",
                    password = "hunter22",
                    url = "https://github.com",
                    notes = "2FA via app",
                )
            )
        }
        snapshot("detail", waitForText = "kudanilll") {
            PasswordDetailScreen(passwordId = id, onNavigateBack = {}, onNavigateToEdit = {})
        }
    }

    @Test
    fun bottomNav() =
        snapshot("bottom_nav") {
            Column(
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                MainTab.entries.forEach {
                    BottomNav(currentTab = it, onTabClick = {}, onAddClick = {})
                }
            }
        }

    @Composable
    private fun home(passwords: List<PasswordEntity>, siteIcons: SiteIcons? = null) =
        HomeScreen(
            passwords = passwords,
            siteIcons = siteIcons,
            searchQuery = "",
            onSearchQueryChange = {},
            onDeletePassword = {},
            onNavigateToDetail = {},
            onAddPassword = {},
        )

    private fun snapshot(
        name: String,
        waitForText: String? = null,
        content: @Composable () -> Unit,
    ) {
        compose.setContent { KupassTheme(dynamicColor = false) { content() } }
        compose.waitForIdle()
        if (waitForText != null) {
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(waitForText).fetchSemanticsNodes().isNotEmpty()
            }
        }
        val dir = File("build/ui-snapshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use {
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** Solid squares stand in for fetched icons; entries without a domain keep their initial. */
    private object FakeSiteIcons : SiteIcons {
        private val colors =
            mapOf("github.com" to 0xFF24292F.toInt(), "netflix.com" to 0xFFE50914.toInt())

        override fun peek(domain: String): ImageBitmap? =
            colors[domain]?.let { color ->
                Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    .apply { eraseColor(color) }
                    .asImageBitmap()
            }

        override suspend fun load(domain: String): ImageBitmap? = peek(domain)
    }
}
