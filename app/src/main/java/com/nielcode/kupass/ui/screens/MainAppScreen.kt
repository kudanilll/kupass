package com.nielcode.kupass.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.home.components.BottomNav
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@SuppressLint("FrequentlyChangingValue")
@Composable
fun MainAppScreen() {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val currentTab =
        if (
            pagerState.currentPageOffsetFraction > 0.5f ||
                pagerState.currentPage == 1 && pagerState.currentPageOffsetFraction > -0.5f
        ) {
            "settings"
        } else {
            "home"
        }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 -> HomeScreen()
                    1 -> SettingsScreen()
                }
            }
        }

        BottomNav(
            currentRoute = currentTab,
            onNavigate = { selectedTab ->
                coroutineScope.launch {
                    val targetPage = if (selectedTab == "home") 0 else 1
                    pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
                }
            },
            onAddClick = { /* TODO: Navigasi */},
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .padding(
                        bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
        )
    }
}
