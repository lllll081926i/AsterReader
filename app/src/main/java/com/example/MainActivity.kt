package com.example

import android.os.Bundle
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
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // Note: Do not set hardware WindowManager.LayoutParams.FLAG_SECURE on the window
            // because AI Studio streams the emulator display via virtual screen capture, and
            // SurfaceFlinger renders FLAG_SECURE windows as a solid black screen in the browser stream.
            LaunchedEffect(Unit) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                AsterReaderAppRoot(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@Composable
fun AsterReaderAppRoot(
    viewModel: AsterViewModel,
    uiState: AsterUiState
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val allShelves by viewModel.allShelves.collectAsState()
    val allAnnotations by viewModel.allAnnotations.collectAsState()
    val allRepositories by viewModel.allRepositories.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importLocalFileUri(uri, "本地导入图书")
        } else {
            viewModel.importLocalFileUri(null, "北极星建筑手稿集")
        }
    }

    val triggerSystemFilePicker = {
        try {
            filePickerLauncher.launch(
                arrayOf(
                    "application/epub+zip",
                    "application/pdf",
                    "application/vnd.comicbook+zip",
                    "application/x-cbz",
                    "text/plain",
                    "*/*"
                )
            )
        } catch (_: Exception) {
            viewModel.importLocalFileUri(null, "北极星建筑手稿集")
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
        val isExpandedTablet = maxWidth >= 700.dp
        val activeSub = uiState.activeSubScreen

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = activeSub to uiState.currentTab,
                transitionSpec = {
                    AsterMotion.pageTransition(
                        reduceMotion = uiState.reduceMotion,
                        forward = targetState.first != null
                    )
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
                                viewModel = viewModel,
                                onBack = { viewModel.popSubScreen() }
                            )
                        }

                        is SubScreen.PublicationDetail -> {
                            val targetBook = allBooks.find { it.id == subScreen.bookId }
                                ?: allBooks.firstOrNull()
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
                            }
                        }

                        is SubScreen.Reader -> {
                            val targetBook = allBooks.find { it.id == subScreen.bookId }
                                ?: allBooks.firstOrNull()
                            if (targetBook != null) {
                                ReaderScreen(
                                    book = targetBook,
                                    initialPage = subScreen.initialPage,
                                    uiState = uiState,
                                    isExpandedTablet = isExpandedTablet,
                                    viewModel = viewModel,
                                    onBack = { viewModel.popSubScreen() }
                                )
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
                                    viewModel.addRepositoryAndScan("本地漫画与轻小说库", "按文件夹识别系列")
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
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
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

            // System Security / Biometric Recognition Module Sheet (No Separate Vault Page)
            if (uiState.showQuickAuthSheet) {
                SystemSecurityAuthPromptSheet(
                    onAuthenticated = { viewModel.unlockAppAndVault() },
                    onVerifyPin = { pin -> viewModel.verifyPin(pin) },
                    onDismiss = { viewModel.dismissQuickAuthSheet() }
                )
            }

            // Floating Snackbar Feedback Pill
            AnimatedVisibility(
                visible = uiState.snackbarMessage != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (showAppleBottomDock) 78.dp else 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
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
