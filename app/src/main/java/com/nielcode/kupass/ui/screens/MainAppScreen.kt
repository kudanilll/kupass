package com.nielcode.kupass.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nielcode.kupass.ui.components.BottomNav
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
object HomeBase

@Serializable
object PasswordEditor

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeBase
    ) {
        composable<HomeBase> {
            MainPagerScreen(
                onNavigateToEditor = { navController.navigate(PasswordEditor) }
            )
        }

        composable<PasswordEditor>(
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 400))
            }
        ) {
            PasswordEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@SuppressLint("FrequentlyChangingValue")
@Composable
fun MainPagerScreen(onNavigateToEditor: () -> Unit) {
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
            onAddClick = onNavigateToEditor,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
        )
    }
}