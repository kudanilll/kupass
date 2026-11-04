package com.nielcode.kupass.ui.screens.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.theme.KupassTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VaultLoadingTest {

    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `a vault still loading shows placeholder rows instead of the empty state`() {
        show(loading = true)

        compose
            .onNodeWithContentDescription(context.getString(R.string.vault_loading))
            .assertExists()
        compose.onNodeWithText(context.getString(R.string.empty_vault_title)).assertDoesNotExist()
    }

    @Test
    fun `a loaded empty vault shows the empty state`() {
        show(loading = false)

        compose.onNodeWithText(context.getString(R.string.empty_vault_title)).assertExists()
        compose
            .onNodeWithContentDescription(context.getString(R.string.vault_loading))
            .assertDoesNotExist()
    }

    private fun show(loading: Boolean) {
        compose.setContent {
            KupassTheme(dynamicColor = false) {
                HomeScreen(
                    passwords = emptyList(),
                    searchQuery = "",
                    onSearchQueryChange = {},
                    onDeletePassword = {},
                    onNavigateToDetail = {},
                    loading = loading,
                )
            }
        }
    }
}
