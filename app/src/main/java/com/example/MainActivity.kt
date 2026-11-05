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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asterreader.core.designsystem.AsterMotion
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.AppleFloatingBottomDock
import com.example.ui.components.MainNavDestination
import com.example.ui.screens.LibraryHomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrivacySecuritySettingsScreen
import com.example.ui.screens.PublicationDetailScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SearchAndFilterScreen
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
    if (!uiState.settingsLoaded || uiState.isAppLockedNow || uiState.showQuickAuthSheet) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (uiState.settingsLoaded) "Aster Reader 已锁定" else "正在加载设置")
                    if (uiState.settingsLoaded) Button(onClick = { viewModel.requestVaultUnlock() }) { Text("解锁") }
                }
            }
        }
        if (uiState.settingsLoaded && uiState.showQuickAuthSheet) SystemSecurityAuthPromptSheet(
            onAuthenticated = viewModel::unlockAppAndVault,
            onDismiss = viewModel::dismissQuickAuthSheet,
            onAuthenticationStarted = viewModel::onSystemAuthenticationStarted,
            onAuthenticationFailed = viewModel::onSystemAuthenticationFailed
        )
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
            viewModel.importLocalFileUri(uri, "本地导入图书")
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
            filePickerLauncher.launch(
                arrayOf(
                    "application/pdf",
                    "application/vnd.comicbook+zip",
                    "application/x-cbz",
                    "application/zip",
                    "application/octet-stream"
                )
            )
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

    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage != null) {
            delay(2600)
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
                        // 主 Tab 底部导航切换动画：精致流体淡入淡出与微缩放，杜绝左右生硬乱滑
                        if (reduceMotion) {
                            androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                        } else {
                            (androidx.compose.animation.fadeIn(
                                animationSpec = androidx.compose.animation.core.tween(180, easing = AsterMotion.EmphasizedDecelerateEasing)
                            ) + androidx.compose.animation.scaleIn(
                                initialScale = 0.98f,
                                animationSpec = androidx.compose.animation.core.tween(180, easing = AsterMotion.EmphasizedDecelerateEasing)
                            )) togetherWith (androidx.compose.animation.fadeOut(
                                animationSpec = androidx.compose.animation.core.tween(140, easing = AsterMotion.EmphasizedAccelerateEasing)
                            ) + androidx.compose.animation.scaleOut(
                                targetScale = 1.01f,
                                animationSpec = androidx.compose.animation.core.tween(140, easing = AsterMotion.EmphasizedAccelerateEasing)
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

                        SubScreen.PrivacySecuritySettings -> {
                            PrivacySecuritySettingsScreen(
                                uiState = uiState,
                                protectedBooksCount = allBooks.count { it.isVaultProtected },
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        SubScreen.Onboarding -> {
                            OnboardingScreen(
                                onImportFileClick = triggerSystemFilePicker,
                                onCreateRepoClick = {
                                    viewModel.addRepositoryAndScan("本地漫画库", "每个文件作为作品")
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

                        MainNavDestination.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                repositories = allRepositories,
                                booksCount = allBooks.size,
                                viewModel = viewModel,
                                onTriggerImportFile = triggerSystemFilePicker,
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
                visible = uiState.snackbarMessage != null,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else fadeIn() + slideInVertically { it / 2 },
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (showAppleBottomDock) 78.dp else 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.widthIn(max = 480.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inversePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.snackbarMessage.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
