package com.example

import android.os.Bundle
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asterreader.core.designsystem.AsterMotion
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.SnackbarTone
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.AppleFloatingBottomDock
import com.example.ui.components.MainNavDestination
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.LibraryHomeScreen
import com.example.ui.screens.HiddenBooksScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrivacySecuritySettingsScreen
import com.example.ui.screens.PublicationDetailScreen
import com.example.ui.screens.ReadingStatsScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SearchAndFilterScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShelfDetailScreen
import com.example.ui.screens.ShelvesScreen
import com.example.ui.screens.SystemSecurityAuthPromptSheet
import com.example.ui.theme.AsterReaderTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: AsterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenOffReceiver, filter)
        enableEdgeToEdge()
        if (savedInstanceState == null) consumeOpenIntent(intent)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.flagSecureEnabled, uiState.hideRecentsPreview, uiState.isAppLockedNow) {
                if (uiState.flagSecureEnabled || uiState.hideRecentsPreview || uiState.isAppLockedNow)
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!uiState.hideRecentsPreview)
            }

            val darkTheme = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AsterReaderTheme(
                darkTheme = darkTheme,
                dynamicColor = uiState.useDynamicColor
            ) {
                CompositionLocalProvider(LocalAsterReduceMotion provides (uiState.reduceMotion || !android.animation.ValueAnimator.areAnimatorsEnabled())) {
                    AsterReaderAppRoot(viewModel = viewModel, uiState = uiState)
                }
            }
        }
    }
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) viewModel.onScreenOff()
        }
    }

    override fun onStart() {
        super.onStart()
        val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        viewModel.onForeground(manager.isDeviceLocked)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeOpenIntent(intent)
    }

    /** 接收「用其他应用打开 / 分享」传来的漫画文件，直接导入并开始阅读。 */
    private fun consumeOpenIntent(intent: Intent?) {
        intent ?: return
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? android.net.Uri
            }
            else -> null
        } ?: return
        if (uri.scheme == "content") {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.openExternalDocument(uri)
    }

    override fun onStop() {
        viewModel.onBackground()
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) viewModel.releaseReaderResources()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewModel.releaseReaderResources()
    }

}

@Composable
fun AsterReaderAppRoot(
    viewModel: AsterViewModel,
    uiState: AsterUiState
) {
    val reduceMotion = LocalAsterReduceMotion.current
    val snackbar by viewModel.snackbar.collectAsStateWithLifecycle()
    if (!uiState.settingsLoaded || uiState.isAppLockedNow || uiState.showQuickAuthSheet) {
        val colorScheme = MaterialTheme.colorScheme
        Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.surface) {
            when {
                !uiState.settingsLoaded -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = colorScheme.primary
                        )
                        Text(
                            text = "正在整理书库…",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState.isAppLockedNow -> AppLockScreen(viewModel = viewModel, uiState = uiState)
                else -> Box(modifier = Modifier.fillMaxSize())
            }
        }
        if (uiState.settingsLoaded && uiState.showQuickAuthSheet && !uiState.isAppLockedNow) {
            SystemSecurityAuthPromptSheet(
                viewModel = viewModel,
                onDismiss = viewModel::dismissQuickAuthSheet
            )
        }
        return
    }
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val allShelves by viewModel.allShelves.collectAsStateWithLifecycle()
    val allAnnotations by viewModel.allAnnotations.collectAsStateWithLifecycle()
    val allRepositories by viewModel.allRepositories.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLocalFileUri(uri)
        }
    }

    val relocateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.relocateBookSource(uri)
    }

    val relocateFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        viewModel.relocateBookSource(uri)
    }

    LaunchedEffect(uiState.pendingRelocateBookId, uiState.pendingRelocateIsFolder) {
        if (uiState.pendingRelocateBookId != null) {
            try {
                if (uiState.pendingRelocateIsFolder) relocateFolderLauncher.launch(null)
                else relocateLauncher.launch(arrayOf("*/*"))
            } catch (_: Exception) {
                viewModel.relocateBookSource(null)
                viewModel.showSnackbar("无法打开选择器")
            }
        }
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        viewModel.onDirectorySelected(it)
    }
    LaunchedEffect(uiState.requestDirectoryPicker) {
        if (uiState.requestDirectoryPicker) {
            try { directoryPickerLauncher.launch(null) }
            catch (_: Exception) { viewModel.onDirectorySelected(null); viewModel.showSnackbar("无法打开目录选择器") }
        }
    }

    val triggerSystemFilePicker = {
        try {
            // 不限制 MIME：CBZ/ZIP/CBR/RAR/CB7/7Z/CBT/TAR/PDF/EPUB/单图 都可选
            filePickerLauncher.launch(arrayOf("*/*"))
        } catch (_: Exception) {
            viewModel.showSnackbar("无法打开系统文件选择器，请重试")
        }
    }

    BackHandler(
        enabled = uiState.isBatchSelectionMode ||
            uiState.subScreenStack.isNotEmpty() ||
            uiState.currentTab != MainNavDestination.LIBRARY
    ) {
        viewModel.popSubScreen()
    }

    LaunchedEffect(snackbar?.id) {
        if (snackbar != null) {
            delay(if (snackbar?.tone == SnackbarTone.ERROR) 3400 else 2600)
            viewModel.consumeSnackbar()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 严格界定平板横屏/展开模式：不仅要求宽度 >= 720dp，还要求处于横屏比例（宽 > 高）
        // 杜绝竖屏大手机或折叠屏竖屏时被误判为平板双栏导致布局异常
        val isExpandedTablet = maxWidth >= 720.dp && maxWidth > maxHeight
        val activeSub = uiState.activeSubScreen

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = activeSub to uiState.currentTab,
                transitionSpec = {
                    val (targetSub, targetTab) = targetState
                    val (initialSub, initialTab) = initialState

                    if (targetSub != null || initialSub != null) {
                        // 子页面与主页面之间的推入/退出导航转场
                        AsterMotion.pageTransition(
                            reduceMotion = reduceMotion,
                            forward = targetSub != null && initialSub == null ||
                                    (targetSub is SubScreen.Reader && initialSub !is SubScreen.Reader)
                        )
                    } else {
                        // 主 Tab 底部导航切换动画：精致滑入与淡入淡出（向左/向右有序切换）
                        val targetIndex = MainNavDestination.entries.indexOf(targetTab)
                        val initialIndex = MainNavDestination.entries.indexOf(initialTab)
                        val direction = if (targetIndex >= initialIndex) 1 else -1

                        if (reduceMotion) {
                            androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                        } else {
                            (androidx.compose.animation.slideInHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(220, easing = AsterMotion.EmphasizedDecelerateEasing),
                                initialOffsetX = { (it * 0.15f * direction).toInt() }
                            ) + androidx.compose.animation.fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(200, easing = AsterMotion.EmphasizedDecelerateEasing)
                            )) togetherWith (androidx.compose.animation.slideOutHorizontally(
                                animationSpec = androidx.compose.animation.core.tween(180, easing = AsterMotion.EmphasizedAccelerateEasing),
                                targetOffsetX = { (-it * 0.12f * direction).toInt() }
                            ) + androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(160, easing = AsterMotion.EmphasizedAccelerateEasing)
                            ))
                        }
                    }
                },
                label = "aster_root_navigation"
            ) { (subScreen, currentTab) ->
                if (subScreen != null) {
                    when (subScreen) {
                        is SubScreen.Search -> {
                            SearchAndFilterScreen(
                                uiState = uiState,
                                books = allBooks,
                                annotations = allAnnotations,
                                openFilterInitially = subScreen.openFilterSheetInitially,
                                initialScope = subScreen.initialScope,
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        is SubScreen.PublicationDetail -> {
                            val targetBook = allBooks.find { it.id == subScreen.bookId }

                            if (targetBook != null) {
                                PublicationDetailScreen(
                                    book = targetBook,
                                    uiState = uiState,
                                    isExpandedTablet = isExpandedTablet,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popSubScreen() },
                                    onStartReading = { startPage ->
                                        viewModel.openBookReader(
                                            bookId = targetBook.id,
                                            initialPage = startPage,
                                            preferDualPageOnTablet = isExpandedTablet
                                        )
                                    }
                                )
                            } else {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("作品不可用，请返回资料库检查来源或访问权限")
                                    Button(onClick = { viewModel.popSubScreen() }) { Text("返回资料库") }
                                }
                            }
                        }

                        is SubScreen.Reader -> {
                            val targetBook = allBooks.find { it.id == subScreen.bookId }

                            if (targetBook != null) {
                                ReaderScreen(
                                    book = targetBook,
                                    initialPage = subScreen.initialPage,
                                    uiState = uiState,
                                    isExpandedTablet = isExpandedTablet,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popSubScreen() }
                                )
                            } else {
                                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("作品不可用，请返回资料库检查来源或访问权限")
                                    Button(onClick = { viewModel.popSubScreen() }) { Text("返回资料库") }
                                }
                            }
                        }

                        is SubScreen.ShelfDetail -> {
                            ShelfDetailScreen(
                                shelfDetail = subScreen,
                                allBooks = allBooks,
                                uiState = uiState,
                                isExpandedTablet = isExpandedTablet,
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        SubScreen.Settings -> {
                            SettingsScreen(
                                uiState = uiState,
                                repositories = allRepositories,
                                booksCount = allBooks.size,
                                viewModel = viewModel,
                                onTriggerImportFile = triggerSystemFilePicker,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        SubScreen.HiddenBooks -> {
                            HiddenBooksScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        SubScreen.ReadingStats -> {
                            if (uiState.showReadingStats) {
                                ReadingStatsScreen(
                                    viewModel = viewModel,
                                    finishedBooks = allBooks.count {
                                        it.totalPages > 0 && it.currentPage >= it.totalPages
                                    },
                                    totalBooks = allBooks.size,
                                    onBack = { viewModel.popSubScreen() }
                                )
                            } else {
                                LaunchedEffect(Unit) { viewModel.popSubScreen() }
                            }
                        }

                        SubScreen.SeriesList -> {
                            SeriesScreen(
                                allBooks = allBooks,
                                uiState = uiState,
                                isExpandedTablet = isExpandedTablet,
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        SubScreen.PrivacySecuritySettings -> {
                            PrivacySecuritySettingsScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        SubScreen.Onboarding -> {
                            OnboardingScreen(
                                onImportFileClick = triggerSystemFilePicker,
                                onCreateRepoClick = {
                                    viewModel.addRepositoryAndScan("本地漫画库", com.example.data.local.LibraryRepository.DEFAULT_SCAN_RULE)
                                },
                                onOpenSecuritySettings = {
                                    viewModel.popSubScreen()
                                    viewModel.pushSubScreen(SubScreen.PrivacySecuritySettings)
                                },
                                onFinish = { viewModel.popSubScreen() }
                            )
                        }
                    }
                } else {
                    // Primary Top-Level Destinations switched via compact AppleFloatingBottomDock
                    when (currentTab) {
                        MainNavDestination.LIBRARY -> {
                            LibraryHomeScreen(
                                uiState = uiState,
                                books = allBooks,
                                shelves = allShelves,
                                isExpandedTablet = isExpandedTablet,
                                viewModel = viewModel,
                                onTriggerSystemFilePicker = triggerSystemFilePicker
                            )
                        }

                        MainNavDestination.SHELVES -> {
                            ShelvesScreen(
                                shelves = allShelves,
                                allBooks = allBooks,
                                annotationsCount = allAnnotations.size,
                                uiState = uiState,
                                isExpandedTablet = isExpandedTablet,
                                viewModel = viewModel
                            )
                        }

                        MainNavDestination.SEARCH -> {
                            SearchAndFilterScreen(
                                uiState = uiState,
                                books = allBooks,
                                annotations = allAnnotations,
                                openFilterInitially = false,
                                viewModel = viewModel,
                                onBack = { viewModel.selectMainTab(MainNavDestination.LIBRARY) }
                            )
                        }
                    }
                }
            }

            // Compact Apple-style Floating Bottom Dock (Icon on Top, Title Below: 书库 / 书架 / 搜索)
            val showAppleBottomDock = activeSub == null && !uiState.isBatchSelectionMode
            AnimatedVisibility(
                visible = showAppleBottomDock,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else slideInVertically { it } + fadeIn(),
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val lastReadBook = allBooks.firstOrNull { !it.isVaultProtected && it.progressPercent > 0 }
                AppleFloatingBottomDock(
                    currentDestination = uiState.currentTab,
                    onSelectDestination = { dest -> viewModel.selectMainTab(dest) },
                    libraryCount = allBooks.size,
                    isVaultUnlocked = uiState.isVaultUnlocked,
                    isExpandedTablet = isExpandedTablet,
                    lastReadTitle = lastReadBook?.title,
                    onQuickResumeRead = lastReadBook?.let { b ->
                        {
                            viewModel.openBookReader(
                                bookId = b.id,
                                preferDualPageOnTablet = isExpandedTablet
                            )
                        }
                    }
                )
            }

            // Floating Snackbar Feedback Pill
            AnimatedVisibility(
                visible = snackbar != null,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else fadeIn() + slideInVertically { it / 2 },
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (showAppleBottomDock) 78.dp else 24.dp, start = 20.dp, end = 20.dp)
            ) {
                val tone = snackbar?.tone ?: SnackbarTone.INFO
                val feedbackColor = when (tone) {
                    SnackbarTone.ERROR -> MaterialTheme.colorScheme.errorContainer
                    SnackbarTone.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                    SnackbarTone.INFO -> MaterialTheme.colorScheme.inverseSurface
                }
                val feedbackContent = when (tone) {
                    SnackbarTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                    SnackbarTone.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                    SnackbarTone.INFO -> MaterialTheme.colorScheme.inverseOnSurface
                }
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = feedbackColor,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .clickable { viewModel.consumeSnackbar() }
                        .semantics { contentDescription = "关闭提示" }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (tone) {
                                SnackbarTone.ERROR -> Icons.Filled.ErrorOutline
                                SnackbarTone.SUCCESS -> Icons.Filled.CheckCircle
                                SnackbarTone.INFO -> Icons.Filled.Info
                            },
                            contentDescription = null,
                            tint = feedbackContent,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = snackbar?.message.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = feedbackContent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}
