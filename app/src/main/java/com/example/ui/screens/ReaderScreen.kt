package com.example.ui.screens
import com.asterreader.core.designsystem.AsterHeaderRow
import com.asterreader.core.designsystem.AsterHeaderTitleStyle
import com.asterreader.core.designsystem.asterHeaderAction
import com.example.ui.components.ReaderSettingsContent

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.displayCutoutPadding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.asterreader.core.designsystem.AsterModalBottomSheet as ModalBottomSheet
import com.asterreader.core.designsystem.AsterReaderPalette.ComicPaper
import com.asterreader.core.designsystem.AsterReaderPalette.StatusIcon

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.example.ui.components.PublicationPageCanvas
import com.asterreader.core.designsystem.AsterSettingsChoice
import com.asterreader.core.designsystem.AsterChoice
import com.example.ui.components.readerModeChoices
import com.example.ui.components.readerFitChoices
import com.example.ui.components.ReaderZoomCommand
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewArray
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.asterreader.core.designsystem.AsterReaderPalette
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.ReaderLayoutMode
import com.example.ui.theme.RobotoMonoFontFamily
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Dedicated high-contrast reader chrome palette so titles & controls are 100% readable
// regardless of whether the manga page or system theme is light or dark.

@Composable
fun ReaderScreen(
    book: BookEntity,
    initialPage: Int? = null,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    BackHandler(enabled = !com.example.ui.components.LocalRootHandlesBack.current, onBack = onBack)
    DisposableEffect(book.id) { onDispose { viewModel.releaseReaderResources(book.id) } }
    var attempt by remember(book.id) { mutableIntStateOf(0) }
    var indexed by remember(book.id, book.filePath, book.sizeBytes, book.sourceModifiedAt, book.contentFingerprint) {
        mutableStateOf<Result<BookEntity>?>(null)
    }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(book.id, book.totalPages, book.filePath, book.sizeBytes, book.sourceModifiedAt, book.contentFingerprint, attempt, owner) {
        if (book.totalPages == 0) owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            indexed = null
            indexed = try { Result.success(viewModel.prepareReadingIndex(book.id)) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { Result.failure(IllegalStateException("无法读取漫画，请检查文件与授权")) }
        }
    }
    val ready = if (book.totalPages > 0) book else indexed?.getOrNull()
    if (ready == null) {
        Column(Modifier.fillMaxSize().background(ComicPaper).padding(com.asterreader.core.designsystem.AsterLayout.PagePadding), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (indexed?.isFailure == true) {
                Text("无法读取漫画，请检查文件与授权", color = StatusIcon)
                TextButton(onClick = { attempt++ }, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = StatusIcon)) { Text("重试") }
            } else {
                androidx.compose.material3.CircularProgressIndicator(color = StatusIcon)
                Text("正在打开漫画", color = StatusIcon)
            }
            TextButton(onClick = onBack, colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = StatusIcon)) { Text("返回") }
        }
        return
    }
    ReaderContent(ready, initialPage, uiState, isExpandedTablet, viewModel, onBack)
}

@Composable
private fun ReaderContent(
    book: BookEntity,
    initialPage: Int?,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    // 安全来源由 Repository 授权并提供随机读取，阅读 UI 不直接接触密钥。

    val context = LocalContext.current
    // 阅读时长：进入开始计时，离开时结算一次
    DisposableEffect(book.id) {
        viewModel.beginReadingSession()
        onDispose { viewModel.endReadingSession() }
    }
    // 沉浸式全屏处理：进入阅读器时隐藏系统状态栏与导航栏，退出时恢复
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())

        onDispose {
            insetsController?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }
    val annotations by remember(book.id) {
        viewModel.observeBookAnnotations(book.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val totalPages = book.totalPages.coerceAtLeast(1)
    val storedChapters by remember(book.id) {
        viewModel.observeChapters(book.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // 有真实章节（按目录分组）时用真实章节；否则退化为页码目录
    val chapters: List<ChapterEntity> = remember(storedChapters, totalPages, book.id) {
        if (storedChapters.isNotEmpty()) storedChapters
        else object : AbstractList<ChapterEntity>() {
            override val size = totalPages
            override fun get(index: Int): ChapterEntity {
                if (index !in indices) throw IndexOutOfBoundsException()
                return ChapterEntity("${book.id}_p$index", book.id, index + 1, "第 ${index + 1} 页",
                    index + 1, index + 1, false, false)
            }
        }
    }

    var restored by remember(book.id) {
        mutableStateOf<com.asterreader.core.database.ReaderStateEntity?>(null)
    }
    var stateLoaded by remember(book.id) { mutableStateOf(false) }
    var modeOverride by remember(book.id) { mutableStateOf<ReaderLayoutMode?>(null) }
    var fitOverride by remember(book.id) { mutableStateOf<String?>(null) }
    var coverOverride by remember(book.id) { mutableStateOf<Boolean?>(null) }
    var rtlOverride by remember(book.id) { mutableStateOf<Boolean?>(null) }
    val readerMode = modeOverride ?: uiState.readerLayoutMode
    val readerFit = fitOverride ?: uiState.fitModeLabel
    val readerCover = coverOverride ?: uiState.smartSpreadStitch
    var currentPage by remember(book.id, initialPage) {
        mutableIntStateOf((initialPage ?: book.currentPage).coerceIn(1, totalPages))
    }
    LaunchedEffect(currentPage, stateLoaded) {
        if (stateLoaded && currentPage != restored?.page) restored = null
    }
    LaunchedEffect(book.id) {
        val pageBeforeRestore = currentPage
        restored = viewModel.loadReaderState(book.id)
        val saved = restored
        modeOverride = saved?.layoutMode?.let { name -> ReaderLayoutMode.entries.find { it.name == name } }
        fitOverride = saved?.fitMode
        coverOverride = saved?.coverSingle
        rtlOverride = saved?.rightToLeft
        if (saved != null && initialPage == null && currentPage == pageBeforeRestore) {
            currentPage = saved.page.coerceIn(1, totalPages)
        }
        stateLoaded = true
    }
    var currentChapterIndex by remember(book.id, chapters) { mutableIntStateOf(0) }

    var showChrome by remember(book.id) { mutableStateOf(false) }
    var showMiniStatusPill by remember { mutableStateOf(true) }
    var isAtEnd by remember(book.id) { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var tabletSideDrawerOpen by remember(isExpandedTablet) { mutableStateOf(false) }
    var tabletSideTab by remember { mutableIntStateOf(0) }

    val readerBrightness = uiState.readerBrightness

    val bookmarkedPages = remember(annotations) {
        annotations.filter { it.type == "BOOKMARK" }.mapTo(HashSet()) { it.pageNumber }
    }

    var sliderPreview by remember(book.id) { mutableStateOf<Float?>(null) }
    var zoomCommand by remember(book.id) { mutableStateOf(ReaderZoomCommand()) }
    val activeChapterTitle = remember(chapters, currentPage) {
        if (storedChapters.isEmpty()) "第 $currentPage 页"
        else chapters.firstOrNull { currentPage in it.startPage..it.endPage }?.title ?: "第 $currentPage 页"
    }
    LaunchedEffect(currentPage, chapters, stateLoaded) {
        currentChapterIndex = if (storedChapters.isEmpty()) currentPage - 1
        else chapters.indexOfFirst { currentPage in it.startPage..it.endPage }.coerceAtLeast(0)
        if (stateLoaded) viewModel.updateReadingProgress(book.id, currentPage, activeChapterTitle)
    }
    LaunchedEffect(stateLoaded, modeOverride, fitOverride, coverOverride, rtlOverride) {
        if (stateLoaded) viewModel.saveReaderOptionsFor(book.id, modeOverride, fitOverride, coverOverride, rtlOverride)
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(book.id) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(ComicPaper)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                val key = event.nativeKeyEvent
                if (uiState.volumeKeyPageTurn && key.keyCode in setOf(android.view.KeyEvent.KEYCODE_VOLUME_UP, android.view.KeyEvent.KEYCODE_VOLUME_DOWN)) {
                    if (key.action == android.view.KeyEvent.ACTION_UP) {
                        currentPage = (currentPage + if (key.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) 1 else -1).coerceIn(1, totalPages)
                    }
                    true
                } else false
            }
            .testTag("reader_screen_root")
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {

                if (!stateLoaded) {
                    Box(Modifier.fillMaxSize().background(ComicPaper))
                } else PublicationPageCanvas(
                    publicationId = book.id,
                    page = currentPage,
                    totalPages = totalPages,
                    mode = readerMode,
                    expanded = isExpandedTablet,
                    coverSingle = readerCover,
                    rightToLeft = rtlOverride ?: (readerMode == ReaderLayoutMode.RTL_PAGER),
                    fitMode = readerFit,
                    preloadBefore = uiState.preloadBefore,
                    preloadAfter = uiState.preloadAfter,
                    initialScale = if (restored?.page == currentPage && initialPage == null) restored?.scale ?: 1f else 1f,
                    initialPanX = if (restored?.page == currentPage && initialPage == null) restored?.panX ?: 0f else 0f,
                    initialPanY = if (restored?.page == currentPage && initialPage == null) restored?.panY ?: 0f else 0f,
                    initialOffsetFraction = if (restored?.page == currentPage && initialPage == null) restored?.offsetFraction ?: 0f else 0f,
                    nativeSize = viewModel::nativePageSize,
                    zoomCommand = zoomCommand,
                    loadPage = { targetPage, maxPixels -> viewModel.renderPage(book.id, targetPage, maxPixels) },
                    loadRegion = { targetPage, top, bottom, pixels ->
                        viewModel.renderPageRegion(book.id, targetPage, top, bottom, pixels)
                    },
                    onPrefetch = { pages -> viewModel.prefetchPages(book.id, pages) },
                    onPageChanged = { currentPage = it },
                    onReachedEnd = { end ->
                        if (end && !isAtEnd) viewModel.markReadingCompleted(book.id)
                        isAtEnd = end
                    },
                    onViewportChanged = { p, fraction, scale, panX, panY ->
                        viewModel.updateViewport(book.id, p, fraction, scale, panX, panY)
                    },
                    onNextPage = {
                        if (currentPage < totalPages) {
                            currentPage++
                        }
                    },
                    onPrevPage = {
                        if (currentPage > 1) {
                            currentPage--
                        }
                    },
                    onToggleChrome = { showChrome = !showChrome }
                )


            // Brightness Dimming Overlay
            val dimAlpha = ((1f - readerBrightness) * 0.65f).coerceIn(0f, 0.65f)
            if (dimAlpha > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                )
            }

            // High-Contrast Transient Top Bar Chrome (全屏模式不叠加系统栏边距，紧凑高度)
            androidx.compose.animation.AnimatedVisibility(
                visible = showChrome,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else slideInVertically { -it } + fadeIn(),
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsterHeaderRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .displayCutoutPadding()
                            .heightIn(min = com.asterreader.core.designsystem.AsterLayout.HeaderHeight)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.asterHeaderAction().testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.HeaderIconSize),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.padding(start = 2.dp, end = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (book.isVaultProtected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = "已锁定",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = activeChapterTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {


                            val isBookmarked = currentPage in bookmarkedPages
                            IconButton(
                                onClick = {
                                    viewModel.toggleBookmark(book.id, currentPage, activeChapterTitle)
                                },
                                modifier = Modifier.asterHeaderAction().testTag("reader_bookmark_btn")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "书签",
                                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.HeaderIconSize),
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isExpandedTablet) {
                                IconButton(
                                    onClick = { showModeSheet = true },
                                    modifier = Modifier.asterHeaderAction().testTag("reader_tablet_drawer_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "阅读设置",
                                        modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.HeaderIconSize),
                                        tint = if (tabletSideDrawerOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Unified Bottom Floating Island (Merged Progress Scrub Bar + Bottom Option Buttons)
            androidx.compose.animation.AnimatedVisibility(
                visible = showChrome,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else slideInVertically { it } + fadeIn(),
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 12.dp,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .testTag("reader_unified_bottom_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Top Half: Progress Scrub Bar + Chapter Step
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (storedChapters.isNotEmpty() && currentChapterIndex > 0) {
                                        currentChapterIndex--
                                        currentPage = chapters[currentChapterIndex].startPage
                                    } else {
                                        currentPage = (currentPage - 10).coerceAtLeast(1)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = currentPage > 1,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (storedChapters.isNotEmpty() && currentChapterIndex > 0) "上一话" else "前 10 页",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3
                                )                            }

                            Text(
                                text = "$currentPage / $totalPages · ${((currentPage.toFloat() / totalPages) * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1.4f),
                                textAlign = TextAlign.Center
                            )

                            TextButton(
                                onClick = {
                                    if (storedChapters.isNotEmpty() && currentChapterIndex < chapters.lastIndex) {
                                        currentChapterIndex++
                                        currentPage = chapters[currentChapterIndex].startPage
                                    } else {
                                        currentPage = (currentPage + 10).coerceAtMost(totalPages)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = currentPage < totalPages,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (storedChapters.isNotEmpty() && currentChapterIndex < chapters.lastIndex) "下一话" else "后 10 页",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Slider(
                            value = sliderPreview ?: currentPage.toFloat(),
                            onValueChange = { sliderPreview = it },
                            onValueChangeFinished = {
                                sliderPreview?.let { currentPage = it.roundToInt().coerceIn(1, totalPages) }
                                sliderPreview = null
                            },
                            valueRange = 1f..totalPages.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .testTag("reader_progress_slider")
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 漫画控制区：将滚动方式直接合并进设置中，改名为“设置”
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReaderIslandButton(
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                label = "目录",
                                onClick = { showTocSheet = true }
                            )

                            ReaderIslandButton(
                                icon = Icons.Outlined.Tune,
                                label = "设置",
                                isActive = showModeSheet,
                                onClick = { showModeSheet = true }
                            )

                            val isBookmarked = currentPage in bookmarkedPages
                            ReaderIslandButton(
                                icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                label = "书签",
                                isActive = isBookmarked,
                                onClick = {
                                    viewModel.toggleBookmark(book.id, currentPage, activeChapterTitle)
                                }
                            )
                        }
                    }
                }
            }

            // Compact Non-intrusive Floating Status Bar at Bottom-Right Corner (完美垂直水平居中，无偏移)
            androidx.compose.animation.AnimatedVisibility(
                visible = showMiniStatusPill,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else fadeIn(),
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .displayCutoutPadding().navigationBarsPadding().padding(end = 2.dp, bottom = 2.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = AsterReaderPalette.StatusOpacity),
                    modifier = Modifier.testTag("reader_mini_status_pill")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = AsterReaderPalette.StatusHorizontalPadding,
                            vertical = AsterReaderPalette.StatusVerticalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAtEnd) "已读完" else
                                "$currentPage / $totalPages · ${((currentPage.toFloat() / totalPages) * 100).toInt().coerceAtMost(99)}%",
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = AsterReaderPalette.StatusFontSize,
                            lineHeight = AsterReaderPalette.StatusFontSize,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }

        // Tablet Collapsible Right-Hand Supporting Drawer
        AnimatedVisibility(
            visible = isExpandedTablet && tabletSideDrawerOpen,
            enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else slideInHorizontally { it } + fadeIn(),
            exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else slideOutHorizontally { it } + fadeOut()
        ) {
            TabletReaderSidePanel(
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                uiState = uiState,
                currentMode = readerMode, currentFit = readerFit, currentCover = readerCover,
                selectedTab = tabletSideTab,
                viewModel = viewModel,
                onSelectTab = { tabletSideTab = it },
                onClose = { tabletSideDrawerOpen = false },
                onSelectLayoutMode = { modeOverride = it; if (it != ReaderLayoutMode.DUAL_PAGE) rtlOverride = null },
                onSelectFit = { fitOverride = it },
                rightToLeft = rtlOverride ?: (readerMode == ReaderLayoutMode.RTL_PAGER),
                onRightToLeft = { rtlOverride = it },
                onReset = { modeOverride = null; fitOverride = null; coverOverride = null; rtlOverride = null },
                showStatus = showMiniStatusPill,
                onShowStatus = { showMiniStatusPill = it },
                onToggleCoverOffset = {

                    coverOverride = it
                },

                onSelectChapter = { idx, chapter ->
                    currentChapterIndex = idx
                    currentPage = chapter.startPage
                }
            )
        }
    }

    // 独立设置页面；系统返回只关闭设置，保留当前阅读位置。
    if (showModeSheet) {
        Dialog(
            onDismissRequest = { showModeSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            com.asterreader.core.designsystem.AsterSettingsScale {
            Surface(Modifier.fillMaxSize().testTag("reader_settings_page"), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize().statusBarsPadding().navigationBarsPadding()
            ) {
                com.asterreader.core.designsystem.AsterPreferenceHeader("阅读设置", { showModeSheet = false }, "返回阅读")
                Column(Modifier.weight(1f).align(Alignment.CenterHorizontally)
                    .widthIn(max = com.asterreader.core.designsystem.AsterPreferenceLayout.ContentMaxWidth)
                    .fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = com.asterreader.core.designsystem.AsterLayout.PagePadding)
                    .padding(top = com.asterreader.core.designsystem.AsterLayout.ControlSpacing)) {
                ReaderSettingsContent(uiState, viewModel, readerMode, readerFit, readerCover,
                    rtlOverride ?: (readerMode == ReaderLayoutMode.RTL_PAGER),
                    onMode = { modeOverride = it; if (it != ReaderLayoutMode.DUAL_PAGE) rtlOverride = null },
                    onFit = { fitOverride = it }, onCover = { coverOverride = it },
                    onRightToLeft = { rtlOverride = it },
                    onReset = { modeOverride = null; fitOverride = null; coverOverride = null; rtlOverride = null },
                    showStatus = showMiniStatusPill, onShowStatus = { showMiniStatusPill = it })
                if (isAtEnd && book.series.isNotBlank()) {
                    TextButton(onClick = { viewModel.openNextVolume(book) }) { Text("下一卷") }
                }
                Spacer(modifier = Modifier.height(14.dp))
                }
            }
            }
            }
        }
    }

    // TOC Bottom Sheet
    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "目录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(chapters) { index, ch ->
                        val isCurrent = index == currentChapterIndex
                        Surface(
                            onClick = {
                                currentChapterIndex = index
                                currentPage = ch.startPage
                                showTocSheet = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ch.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "第 ${ch.startPage} 页",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

}

@Composable
private fun ReaderIslandButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * 1. Continuous Webtoon Vertical Scroll Reader — Clean images with zero patch number overlays
 */
@Composable
private fun TabletReaderSidePanel(
    chapters: List<ChapterEntity>,
    currentChapterIndex: Int,
    uiState: AsterUiState,
    currentMode: ReaderLayoutMode,
    currentFit: String,
    currentCover: Boolean,
    selectedTab: Int,
    viewModel: AsterViewModel,
    onSelectTab: (Int) -> Unit,
    onClose: () -> Unit,
    onSelectLayoutMode: (ReaderLayoutMode) -> Unit,
    onSelectFit: (String) -> Unit,
    rightToLeft: Boolean,
    onRightToLeft: (Boolean) -> Unit,
    onReset: () -> Unit,
    showStatus: Boolean,
    onShowStatus: (Boolean) -> Unit,
    onToggleCoverOffset: (Boolean) -> Unit,
    onSelectChapter: (Int, ChapterEntity) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight()
            .testTag("tablet_reader_side_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AsterHeaderRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = com.asterreader.core.designsystem.AsterLayout.HeaderHeight)
                    .padding(horizontal = com.asterreader.core.designsystem.AsterLayout.PagePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "阅读设置",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.asterHeaderAction()) {
                    Icon(Icons.Default.Close, contentDescription = "收起", modifier = Modifier.size(18.dp))
                }
            }

            AsterSettingsChoice(listOf(AsterChoice("设置", Icons.Outlined.Tune),
                AsterChoice("目录", Icons.AutoMirrored.Filled.FormatListBulleted)), selectedTab, onSelectTab,
                Modifier.padding(horizontal = 16.dp))

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    ReaderSettingsContent(uiState, viewModel, currentMode, currentFit, currentCover, rightToLeft,
                        onSelectLayoutMode, onSelectFit, onToggleCoverOffset, onRightToLeft, onReset,
                        showStatus, onShowStatus)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(chapters) { idx, ch ->
                        val active = idx == currentChapterIndex
                        Surface(
                            onClick = { onSelectChapter(idx, ch) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ch.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "第 ${ch.startPage} 页",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = RobotoMonoFontFamily,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
