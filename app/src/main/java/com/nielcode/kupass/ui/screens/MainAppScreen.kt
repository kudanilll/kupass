package com.nielcode.kupass.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.home.components.BottomNav
import com.nielcode.kupass.ui.screens.settings.SettingsScreen

@Composable
fun MainAppScreen() {
    var currentTab by remember { mutableStateOf("home") }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = currentTab, label = "tab_transition") { tab ->
            when (tab) {
                "home" -> HomeScreen()
                "settings" -> SettingsScreen()
            }
        }

        BottomNav(
            currentRoute = currentTab,
            onNavigate = { selectedTab ->
                currentTab = selectedTab
            },
            onAddClick = { /* TODO: Navigasi ke CreatePasswordActivity/Screen */ },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
        )
    }
}
