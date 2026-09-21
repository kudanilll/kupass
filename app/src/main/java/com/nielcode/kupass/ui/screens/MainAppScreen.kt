package com.nielcode.kupass.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nielcode.kupass.App
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.security.AppLock
import com.nielcode.kupass.ui.components.BottomNav
import com.nielcode.kupass.ui.components.BottomNavHeight
import com.nielcode.kupass.ui.components.MainTab
import com.nielcode.kupass.ui.components.navSpring
import com.nielcode.kupass.ui.screens.data.BackupProgressDialog
import com.nielcode.kupass.ui.screens.data.BackupViewModel
import com.nielcode.kupass.ui.screens.data.DataScreen
import com.nielcode.kupass.ui.screens.data.ExportPasswordDialog
import com.nielcode.kupass.ui.screens.data.ImportPasswordDialog
import com.nielcode.kupass.ui.screens.detail.PasswordDetailScreen
import com.nielcode.kupass.ui.screens.editor.PasswordEditorScreen
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.home.HomeViewModel
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object HomeBase

@Serializable data class PasswordEditor(val passwordId: Long = -1L)

@Serializable data class PasswordDetail(val passwordId: Long)

private const val SCREEN_TRANSITION_MILLIS = 400
private const val BACKUP_MIME_TYPE = "application/json"
private const val BACKUP_FILE_NAME = "kupass-backup.json"

/** Root navigation: the tabbed home pager plus full-screen detail and editor routes. */
@Composable
fun MainAppScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeBase, modifier = modifier) {
        composable<HomeBase> {
            MainPagerScreen(
                onNavigateToEditor = { navController.navigate(PasswordEditor()) },
                onNavigateToDetail = { id -> navController.navigate(PasswordDetail(id)) },
            )
        }
        fullScreenRoute<PasswordDetail> { backStackEntry ->
            PasswordDetailScreen(
                passwordId = backStackEntry.toRoute<PasswordDetail>().passwordId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(PasswordEditor(passwordId = id))
                },
            )
        }
        fullScreenRoute<PasswordEditor> { backStackEntry ->
            PasswordEditorScreen(
                passwordId = backStackEntry.toRoute<PasswordEditor>().passwordId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * A route that covers the pager: it slides up over it and slides back down when popped. Every
 * full-screen route shares these transitions, so they are declared once here.
 */
private inline fun <reified T : Any> NavGraphBuilder.fullScreenRoute(
    crossinline content: @Composable (NavBackStackEntry) -> Unit
) =
    composable<T>(
        enterTransition = { slideUpEnter() },
        popExitTransition = { slideDownExit() },
    ) { backStackEntry ->
        content(backStackEntry)
    }

private fun slideUpEnter(): EnterTransition =
    slideInVertically(tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(SCREEN_TRANSITION_MILLIS))

private fun slideDownExit(): ExitTransition =
    slideOutVertically(tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } +
        fadeOut(tween(SCREEN_TRANSITION_MILLIS))

/** Home, Data, and Settings pages with the floating bottom navigation and backup flows. */
@Composable
fun MainPagerScreen(
    onNavigateToEditor: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    backupViewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory),
) {
    val passwords by homeViewModel.passwords.collectAsStateWithLifecycle()
    val searchQuery by homeViewModel.searchQuery.collectAsStateWithLifecycle()
    val importPrompt by backupViewModel.importPrompt.collectAsStateWithLifecycle()
    val backupBusy by backupViewModel.backupBusy.collectAsStateWithLifecycle()
    val siteIcons = rememberSiteIcons()

    val filePickers =
        rememberBackupFilePickers(
            onExportFilePick = { uri ->
                if (uri != null) backupViewModel.exportPasswords(uri)
                else backupViewModel.cancelExport()
            },
            onImportFilePick = backupViewModel::importPasswords,
        )
    var showExportDialog by remember { mutableStateOf(false) }

    BackupDialogs(
        showExportDialog = showExportDialog,
        importWrongPassword = importPrompt?.wrongPassword,
        busy = backupBusy,
        onExportConfirm = { password ->
            showExportDialog = false
            backupViewModel.prepareExport(password)
            filePickers.pickExportFile()
        },
        onExportDismiss = { showExportDialog = false },
        onImportPassword = backupViewModel::submitImportPassword,
        onImportDismiss = backupViewModel::cancelImport,
    )
    VaultEventToasts(events = homeViewModel.events)
    VaultEventToasts(events = backupViewModel.events)

    MainPager(
        onAddClick = onNavigateToEditor,
        modifier = modifier,
    ) { tab, contentPadding ->
        when (tab) {
            MainTab.Home ->
                HomeScreen(
                    passwords = passwords,
                    searchQuery = searchQuery,
                    onSearchQueryChange = homeViewModel::onSearchQueryChange,
                    onDeletePassword = homeViewModel::deletePassword,
                    onNavigateToDetail = onNavigateToDetail,
                    contentPadding = contentPadding,
                    siteIcons = siteIcons,
                )
            MainTab.Data ->
                DataScreen(
                    onExportClick = { showExportDialog = true },
                    onImportClick = filePickers::pickImportFile,
                    contentPadding = contentPadding,
                )
            MainTab.Settings -> SettingsScreen(contentPadding = contentPadding)
        }
    }
}

/** The process-wide site icon loader. */
@Composable
private fun rememberSiteIcons(): SiteIcons {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { (appContext as App).container.siteIcons }
}

/** Storage Access Framework pickers for backups. Leaving for the picker never locks the vault. */
private class BackupFilePickers(
    private val appLock: AppLock,
    private val exportLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
) {
    fun pickExportFile() {
        appLock.allowNextBackground()
        exportLauncher.launch(BACKUP_FILE_NAME)
    }

    fun pickImportFile() {
        appLock.allowNextBackground()
        importLauncher.launch(arrayOf(BACKUP_MIME_TYPE))
    }
}

/**
 * @param onExportFilePick receives null when the user backs out of the picker.
 * @param onImportFilePick only called when a file was chosen.
 */
@Composable
private fun rememberBackupFilePickers(
    onExportFilePick: (Uri?) -> Unit,
    onImportFilePick: (Uri) -> Unit,
): BackupFilePickers {
    val appContext = LocalContext.current.applicationContext
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
            onExportFilePick,
        )
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onImportFilePick)
        }
    return remember(appContext, exportLauncher, importLauncher) {
        BackupFilePickers((appContext as App).container.appLock, exportLauncher, importLauncher)
    }
}

/**
 * Pager over [MainTab] with a bottom navigation that hides while scrolling down.
 *
 * @param page content for each tab.
 */
@Composable
private fun MainPager(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (tab: MainTab, contentPadding: PaddingValues) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val coroutineScope = rememberCoroutineScope()
    var bottomBarVisible by remember { mutableStateOf(true) }
    val hideOnScroll = remember { HideOnScrollConnection { bottomBarVisible = it } }

    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val pageContentPadding = PaddingValues(bottom = BottomNavHeight + navigationBar)

    Box(modifier = modifier.fillMaxSize().nestedScroll(hideOnScroll)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            Box(modifier = Modifier.fillMaxSize()) {
                page(MainTab.entries[index], pageContentPadding)
            }
        }
        AnimatedVisibility(
            visible = bottomBarVisible,
            enter = slideInVertically(navSpring()) { it } + fadeIn(navSpring()),
            exit = slideOutVertically(navSpring()) { it } + fadeOut(navSpring()),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BottomNav(
                currentTab = MainTab.entries[pagerState.currentPage],
                onTabClick = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = tab.ordinal,
                            animationSpec =
                                tween(SCREEN_TRANSITION_MILLIS, easing = FastOutSlowInEasing),
                        )
                    }
                },
                onAddClick = onAddClick,
                modifier =
                    Modifier.padding(
                        bottom =
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
            )
        }
    }
}

/** Reports scroll direction: hides the bottom bar on scroll down, shows it on scroll up. */
private class HideOnScrollConnection(private val onVisibilityChange: (Boolean) -> Unit) :
    NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y < -SCROLL_THRESHOLD_PX) onVisibilityChange(false)
        if (available.y > SCROLL_THRESHOLD_PX) onVisibilityChange(true)
        return Offset.Zero
    }

    private companion object {
        const val SCROLL_THRESHOLD_PX = 5f
    }
}

/**
 * Export password, import password, and progress dialogs.
 *
 * @param importWrongPassword null when no encrypted import is pending; otherwise whether the last
 *   attempt used a wrong password.
 */
@Composable
private fun BackupDialogs(
    showExportDialog: Boolean,
    importWrongPassword: Boolean?,
    busy: Boolean,
    onExportConfirm: (CharArray) -> Unit,
    onExportDismiss: () -> Unit,
    onImportPassword: (CharArray) -> Unit,
    onImportDismiss: () -> Unit,
) {
    if (showExportDialog) {
        ExportPasswordDialog(onConfirm = onExportConfirm, onDismiss = onExportDismiss)
    }
    if (importWrongPassword != null && !busy) {
        ImportPasswordDialog(
            wrongPassword = importWrongPassword,
            onConfirm = onImportPassword,
            onDismiss = onImportDismiss,
        )
    }
    if (busy) BackupProgressDialog()
}

/** Shows each export/import result once, only while the screen is at least started. */
@Composable
private fun VaultEventToasts(events: Flow<VaultEvent>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(events, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            events.collect { event ->
                Toast.makeText(context, event.message(context), Toast.LENGTH_LONG).show()
            }
        }
    }
}
