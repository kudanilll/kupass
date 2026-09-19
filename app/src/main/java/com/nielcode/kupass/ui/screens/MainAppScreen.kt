package com.nielcode.kupass.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nielcode.kupass.App
import com.nielcode.kupass.ui.components.BottomNav
import com.nielcode.kupass.ui.screens.data.BackupProgressDialog
import com.nielcode.kupass.ui.screens.data.ExportPasswordDialog
import com.nielcode.kupass.ui.screens.data.ImportPasswordDialog
import com.nielcode.kupass.ui.screens.detail.PasswordDetailScreen
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.home.HomeViewModel
import com.nielcode.kupass.ui.screens.home.message
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalAnimationApi::class) @Serializable object HomeBase

@Serializable data class PasswordEditor(val passwordId: Long = -1L)

@Serializable data class PasswordDetail(val passwordId: Long)

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = HomeBase) {
        composable<HomeBase> {
            MainPagerScreen(
                onNavigateToEditor = { navController.navigate(PasswordEditor()) },
                onNavigateToDetail = { passwordId ->
                    navController.navigate(PasswordDetail(passwordId))
                },
            )
        }

        composable<PasswordDetail>(
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 400))
            },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<PasswordDetail>()
            PasswordDetailScreen(
                passwordId = route.passwordId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(PasswordEditor(passwordId = id))
                },
            )
        }

        composable<PasswordEditor>(
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 400))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 400))
            },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<PasswordEditor>()
            PasswordEditorScreen(
                passwordId = route.passwordId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("FrequentlyChangingValue")
@Composable
fun MainPagerScreen(
    onNavigateToEditor: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {},
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val currentTab =
        when (pagerState.currentPage) {
            0 -> "home"
            1 -> "data"
            2 -> "settings"
            else -> "home"
        }

    var isBottomNavVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5f) isBottomNavVisible = false
                if (available.y > 5f) isBottomNavVisible = true
                return Offset.Zero
            }
        }
    }

    // Export/Import file pickers
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) homeViewModel.exportPasswords(uri) else homeViewModel.cancelExport()
        }

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri
            ->
            uri?.let { homeViewModel.importPasswords(it) }
        }

    val appContext = LocalContext.current.applicationContext
    val appLock = remember(appContext) { (appContext as App).container.appLock }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    val importPrompt by homeViewModel.importPrompt.collectAsStateWithLifecycle()
    val backupBusy by homeViewModel.backupBusy.collectAsStateWithLifecycle()

    if (showExportPasswordDialog) {
        ExportPasswordDialog(
            onConfirm = { password ->
                showExportPasswordDialog = false
                homeViewModel.prepareExport(password)
                appLock.allowNextBackground()
                exportLauncher.launch("kupass-backup.json")
            },
            onDismiss = { showExportPasswordDialog = false },
        )
    }
    importPrompt?.let { prompt ->
        if (!backupBusy) {
            ImportPasswordDialog(
                wrongPassword = prompt.wrongPassword,
                onConfirm = homeViewModel::submitImportPassword,
                onDismiss = homeViewModel::cancelImport,
            )
        }
    }
    if (backupBusy) BackupProgressDialog()

    // One-shot export/import results, collected only while the screen is started.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(homeViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            homeViewModel.events.collect { event ->
                Toast.makeText(context, event.message(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 ->
                        HomeScreen(
                            onNavigateToDetail = onNavigateToDetail,
                            viewModel = homeViewModel,
                        )

                    1 ->
                        com.nielcode.kupass.ui.screens.data.DataScreen(
                            onExportClick = { showExportPasswordDialog = true },
                            onImportClick = {
                                appLock.allowNextBackground()
                                importLauncher.launch(arrayOf("application/json"))
                            },
                        )

                    2 -> SettingsScreen()
                }
            }
        }

        AnimatedVisibility(
            visible = isBottomNavVisible,
            enter =
                slideInVertically(
                    initialOffsetY = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                ) + fadeIn(animationSpec = tween(durationMillis = 300, easing = LinearEasing)),
            exit =
                slideOutVertically(
                    targetOffsetY = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                ) + fadeOut(animationSpec = tween(durationMillis = 300, easing = LinearEasing)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomNav(
                currentRoute = currentTab,
                onNavigate = { selectedTab ->
                    coroutineScope.launch {
                        val targetPage =
                            when (selectedTab) {
                                "home" -> 0
                                "data" -> 1
                                "settings" -> 2
                                else -> 0
                            }
                        pagerState.animateScrollToPage(
                            page = targetPage,
                            animationSpec =
                                tween(durationMillis = 400, easing = FastOutSlowInEasing),
                        )
                    }
                },
                onAddClick = onNavigateToEditor,
                modifier =
                    Modifier.padding(
                        bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
            )
        }
    }
}
