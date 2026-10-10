package com.nielcode.kupass.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nielcode.kupass.App
import com.nielcode.kupass.data.siteicon.SiteIcons
import com.nielcode.kupass.security.EntryAuthenticationViewModel
import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import com.nielcode.kupass.security.ExportConsent
import com.nielcode.kupass.ui.components.BottomNav
import com.nielcode.kupass.ui.components.BottomNavHeight
import com.nielcode.kupass.ui.components.MainTab
import com.nielcode.kupass.ui.components.navSpring
import com.nielcode.kupass.ui.screens.data.BackupProgressDialog
import com.nielcode.kupass.ui.screens.data.BackupViewModel
import com.nielcode.kupass.ui.screens.data.DataScreen
import com.nielcode.kupass.ui.screens.data.ExportPasswordDialog
import com.nielcode.kupass.ui.screens.data.ImportPasswordDialog
import com.nielcode.kupass.ui.screens.home.HomeScreen
import com.nielcode.kupass.ui.screens.home.HomeViewModel
import com.nielcode.kupass.ui.screens.lock.SensitiveActionGuidance
import com.nielcode.kupass.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object HomeBase

@Serializable data class PasswordEditor(val passwordId: Long = -1L)

@Serializable data class PasswordDetail(val passwordId: Long)

private const val BACKUP_MIME_TYPE = "application/json"
private const val BACKUP_FILE_NAME = "kupass-backup.json"

/** Root navigation: the tabbed home pager plus full-screen detail and editor routes. */
@Composable
// One activity-retained controller owns native requests across destinations and rotation.
@Suppress("ViewModelForwarding")
fun MainAppScreen(
    authentication: EntryAuthenticationViewModel,
    authState: EntryAuthenticationViewModel.State,
    onOpenSecuritySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    DisposableEffect(navController, authentication) {
        val listener = NavController.OnDestinationChangedListener { controller, _, _ ->
            controller.currentBackStackEntry?.let { entry ->
                authentication.setDestination(entry.id, entry.protectedAccountId())
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    SideEffect(authState.approvedOpen) {
        authState.approvedOpen?.let { request ->
            authentication.openApprovedAccount(request.token) { id ->
                navController.navigate(PasswordDetail(id))
                navController.currentBackStackEntry?.id
            }
        }
    }
    SensitiveActionGuidance(
        visible =
            authState.pending != null &&
                authState.pending.action != Action.UnlockDestination &&
                !authState.deviceSecure,
        onOpenSecuritySettings = onOpenSecuritySettings,
        onDismiss = authentication::cancelPending,
    )
    NavHost(navController = navController, startDestination = HomeBase, modifier = modifier) {
        composable<HomeBase> { entry ->
            MainPagerScreen(
                entryId = entry.id,
                authentication = authentication,
                onNavigateToEditor = { navController.navigate(PasswordEditor()) },
                onNavigateToDetail = { id ->
                    authentication.request(Action.OpenAccount, entry.id, id)
                },
            )
        }
        accountRoutes(navController, authentication, onOpenSecuritySettings)
    }
}

/** Home, Data, and Settings pages with the floating bottom navigation and backup flows. */
@Composable
fun MainPagerScreen(
    entryId: String,
    authentication: EntryAuthenticationViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    backupViewModel: BackupViewModel = viewModel(factory = BackupViewModel.factory(authentication)),
) {
    val authState by authentication.state.collectAsStateWithLifecycle()
    val passwords by homeViewModel.passwords.collectAsStateWithLifecycle()
    val searchQuery by homeViewModel.searchQuery.collectAsStateWithLifecycle()
    val importPrompt by backupViewModel.importPrompt.collectAsStateWithLifecycle()
    val backupBusy by backupViewModel.backupBusy.collectAsStateWithLifecycle()
    val siteIcons = rememberSiteIcons()

    val filePickers =
        rememberBackupFilePickers(
            onAllowBackground = authentication::allowNextBackground,
            onExportFilePick = { uri ->
                finishExportPicker(uri, authentication, backupViewModel)
            },
            onImportFilePick = backupViewModel::importPasswords,
        )
    val consent = authState.export?.takeIf { it.entryId == entryId }
    BackupDialogs(
        showExportDialog = consent?.phase == ExportConsent.Phase.Password,
        importWrongPassword = importPrompt?.wrongPassword,
        busy = backupBusy,
        onExportConfirm = { password ->
            confirmExport(password, consent, authentication, backupViewModel, filePickers)
        },
        onExportDismiss = authentication::cancelPending,
        onImportPassword = backupViewModel::submitImportPassword,
        onImportDismiss = backupViewModel::cancelImport,
    )
    VaultEventToasts(events = homeViewModel.events)
    VaultEventToasts(events = backupViewModel.events)

    MainPager(
        onAddClick = onNavigateToEditor,
        onTabChange = authentication::cancelPending,
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
                    onExportClick = { authentication.request(Action.Export, entryId) },
                    onImportClick = filePickers::pickImportFile,
                    contentPadding = contentPadding,
                )
            MainTab.Settings ->
                SettingsScreen(
                    contentPadding = contentPadding,
                    onAllowBackground = authentication::allowNextBackground,
                )
        }
    }
}

private fun finishExportPicker(
    uri: Uri?,
    authentication: EntryAuthenticationViewModel,
    backupViewModel: BackupViewModel,
) {
    if (authentication.completeExportPicker(uri != null) && uri != null) {
        backupViewModel.exportPasswords(uri)
    } else {
        backupViewModel.cancelExport()
    }
}

private fun confirmExport(
    password: CharArray,
    consent: ExportConsent?,
    authentication: EntryAuthenticationViewModel,
    backupViewModel: BackupViewModel,
    filePickers: BackupFilePickers,
) {
    if (consent != null && authentication.startExportPicker(consent.token)) {
        backupViewModel.prepareExport(password)
        if (!filePickers.pickExportFile()) {
            authentication.completeExportPicker(false)
            backupViewModel.cancelExport()
        }
    } else {
        password.fill('\u0000')
        authentication.cancelPending()
        backupViewModel.cancelExport()
    }
}

/** The process-wide site icon loader. */
@Composable
private fun rememberSiteIcons(): SiteIcons {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { (appContext as App).container.siteIcons }
}

/** Storage Access Framework pickers; trips have the existing bounded background grace. */
private class BackupFilePickers(
    private val onAllowBackground: () -> Unit,
    private val exportLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val importLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
) {
    fun pickExportFile(): Boolean {
        onAllowBackground()
        return try {
            exportLauncher.launch(BACKUP_FILE_NAME)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    fun pickImportFile() {
        onAllowBackground()
        importLauncher.launch(arrayOf(BACKUP_MIME_TYPE))
    }
}

/**
 * @param onExportFilePick receives null when the user backs out of the picker.
 * @param onImportFilePick only called when a file was chosen.
 */
@Composable
private fun rememberBackupFilePickers(
    onAllowBackground: () -> Unit,
    onExportFilePick: (Uri?) -> Unit,
    onImportFilePick: (Uri) -> Unit,
): BackupFilePickers {
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
            onExportFilePick,
        )
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(onImportFilePick)
        }
    return remember(onAllowBackground, exportLauncher, importLauncher) {
        BackupFilePickers(onAllowBackground, exportLauncher, importLauncher)
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
    onTabChange: () -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (tab: MainTab, contentPadding: PaddingValues) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
    val currentOnTabChange by androidx.compose.runtime.rememberUpdatedState(onTabChange)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.drop(1).collect { currentOnTabChange() }
    }
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
