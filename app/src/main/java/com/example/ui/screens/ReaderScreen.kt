package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.asterreader.core.designsystem.AsterModalBottomSheet as ModalBottomSheet
import com.asterreader.core.designsystem.AsterReaderPalette.ComicPaper
import com.asterreader.core.designsystem.AsterReaderPalette.StatusIcon
import com.asterreader.core.designsystem.AsterReaderPalette.StatusText
import com.asterreader.core.designsystem.AsterReaderPalette.StatusSurface
import com.asterreader.core.designsystem.AsterReaderPalette.ChromeBorderColor
import com.asterreader.core.designsystem.AsterReaderPalette.ChromeAccentMint
import com.asterreader.core.designsystem.AsterReaderPalette.ChromeTextSecondary
import com.asterreader.core.designsystem.AsterReaderPalette.ChromeTextPrimary
import com.asterreader.core.designsystem.AsterReaderPalette.ChromePillColor
import com.asterreader.core.designsystem.AsterReaderPalette.ChromeSurfaceColor

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.example.ui.components.PublicationPageCanvas
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
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.ReaderLayoutMode
import com.example.ui.theme.RobotoMonoFontFamily
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
    BackHandler(onBack = onBack)
    if (book.isVaultProtected || book.isVaultEncrypted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("此版本无法安全读取保险库内容") }
        return
    }

    val context = LocalContext.current
    DisposableEffect(book.id) { onDispose { viewModel.releaseReaderResources() } }
    val annotations by remember(book.id) {
        viewModel.observeBookAnnotations(book.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val totalPages = book.totalPages.coerceAtLeast(1)
    val chapters = remember(book.id, totalPages) {
        object : AbstractList<ChapterEntity>() {
            override val size = totalPages
            override fun get(index: Int) = ChapterEntity("${book.id}_$index", book.id, index + 1,
                "第 ${index + 1} 页", index + 1, index + 1, false, false)
        }
    }
    var currentPage by remember(book.id, initialPage) {
        mutableIntStateOf((initialPage ?: book.currentPage).coerceIn(1, totalPages))
    }
    var currentChapterIndex by remember(book.id, chapters) {
        val idx = currentPage - 1
        mutableIntStateOf(idx)
    }

    var showChrome by remember { mutableStateOf(true) }
    var showMiniStatusPill by remember { mutableStateOf(true) }
    var showModeSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var tabletSideDrawerOpen by remember(isExpandedTablet) { mutableStateOf(false) }
    var tabletSideTab by remember { mutableIntStateOf(0) }

    var modeSwitchToastText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.readerLayoutMode) {
        val label = when (uiState.readerLayoutMode) {
            ReaderLayoutMode.VERTICAL_SCROLL -> "上下滚动"
            ReaderLayoutMode.RTL_PAGER -> "右向左翻页"
            ReaderLayoutMode.LTR_PAGER -> "左向右翻页"
            ReaderLayoutMode.DUAL_PAGE -> "双页展开"
        }
        modeSwitchToastText = "已切换至 $label"
        delay(1800)
        modeSwitchToastText = null
    }

    var readerBrightness by remember { mutableFloatStateOf(0.95f) }
    var dualPageCoverOffset by remember { mutableStateOf(uiState.smartSpreadStitch) }
    val bookmarkedPages = annotations.filter { it.type == "BOOKMARK" }.map { it.pageNumber }.toSet()

    var sliderPreview by remember(book.id) { mutableStateOf<Float?>(null) }
    val activeChapterTitle = "第 $currentPage 页"
    LaunchedEffect(currentPage) {
        currentChapterIndex = currentPage - 1
        viewModel.updateReadingProgress(book.id, currentPage, activeChapterTitle)
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

                PublicationPageCanvas(
                    publicationId = book.id,
                    page = currentPage,
                    totalPages = totalPages,
                    mode = uiState.readerLayoutMode,
                    expanded = isExpandedTablet,
                    coverSingle = uiState.smartSpreadStitch,
                    fitMode = uiState.fitModeLabel,
                    loadPage = { viewModel.renderPage(book.id, it) },
                    onPageChanged = { currentPage = it },
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

            // High-Contrast Transient Top Bar Chrome (Always legible on both dark & light manga pages)
            androidx.compose.animation.AnimatedVisibility(
                visible = showChrome,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else slideInVertically { -it } + fadeIn(),
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = ChromeSurfaceColor,
                    contentColor = ChromeTextPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("reader_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    tint = ChromeTextPrimary
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
                                        color = ChromeTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (book.isVaultProtected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lock,
                                            contentDescription = "受保护",
                                            tint = ChromeAccentMint,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = activeChapterTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChromeTextSecondary,
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
                                modifier = Modifier.testTag("reader_bookmark_btn")
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "书签",
                                    tint = if (isBookmarked) ChromeAccentMint else ChromeTextPrimary
                                )
                            }

                            if (isExpandedTablet) {
                                IconButton(
                                    onClick = { tabletSideDrawerOpen = !tabletSideDrawerOpen },
                                    modifier = Modifier.testTag("reader_tablet_drawer_toggle")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = "侧栏",
                                        tint = if (tabletSideDrawerOpen) ChromeAccentMint else ChromeTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mode Switch Floating Pill Toast
            androidx.compose.animation.AnimatedVisibility(
                visible = modeSwitchToastText != null,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else fadeIn() + slideInVertically { -it / 2 },
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = ChromeSurfaceColor,
                    border = BorderStroke(0.8.dp, ChromeBorderColor),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ChromeAccentMint,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = modeSwitchToastText.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChromeTextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = ChromeSurfaceColor,
                    contentColor = ChromeTextPrimary,
                    shadowElevation = 12.dp,
                    border = BorderStroke(0.8.dp, ChromeBorderColor),
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .testTag("reader_unified_bottom_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Half: Progress Scrub Bar + Chapter Step
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (currentChapterIndex > 0) {
                                        currentChapterIndex--
                                        currentPage = chapters[currentChapterIndex].startPage
                                    } else {
                                        currentPage = (currentPage - 10).coerceAtLeast(1)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = null,
                                    tint = ChromeTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "上一话",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChromeTextPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Text(
                                text = "$currentPage / $totalPages · ${((currentPage.toFloat() / totalPages) * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = RobotoMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ChromeAccentMint,
                                maxLines = 1,
                                softWrap = false
                            )

                            TextButton(
                                onClick = {
                                    if (currentChapterIndex < chapters.lastIndex) {
                                        currentChapterIndex++
                                        currentPage = chapters[currentChapterIndex].startPage
                                    } else {
                                        currentPage = (currentPage + 10).coerceAtMost(totalPages)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "下一话",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChromeTextPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = null,
                                    tint = ChromeTextPrimary,
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
                                thumbColor = ChromeAccentMint,
                                activeTrackColor = ChromeAccentMint,
                                inactiveTrackColor = ChromePillColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .testTag("reader_progress_slider")
                        )

                        HorizontalDivider(color = ChromeBorderColor)

                        // 漫画控制区。
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
                                    icon = Icons.Outlined.SwapHoriz,
                                    label = uiState.readerLayoutMode.shortBadge,
                                    isActive = false,
                                    onClick = {
                                        val nextMode = when (uiState.readerLayoutMode) {
                                            ReaderLayoutMode.VERTICAL_SCROLL -> ReaderLayoutMode.RTL_PAGER
                                            ReaderLayoutMode.RTL_PAGER -> ReaderLayoutMode.LTR_PAGER
                                            ReaderLayoutMode.LTR_PAGER -> ReaderLayoutMode.DUAL_PAGE
                                            ReaderLayoutMode.DUAL_PAGE -> ReaderLayoutMode.VERTICAL_SCROLL
                                        }
                                        viewModel.setReaderLayoutMode(nextMode)
                                    }
                                )
                                ReaderIslandButton(
                                    icon = Icons.Outlined.Tune,
                                    label = "漫画设置",
                                    isActive = showModeSheet,
                                    onClick = { showModeSheet = true }
                                )
                                val isBookmarked = currentPage in bookmarkedPages
                                ReaderIslandButton(
                                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    label = if (isBookmarked) "已存书签" else "书签",
                                    isActive = isBookmarked,
                                    onClick = {
                                        viewModel.toggleBookmark(book.id, currentPage, activeChapterTitle)
                                    }
                                )

                        }
                    }
                }
            }

            // Exquisite Closable Floating Mini Status Bar at Bottom-Right Corner
            androidx.compose.animation.AnimatedVisibility(
                visible = showMiniStatusPill,
                enter = if (LocalAsterReduceMotion.current) androidx.compose.animation.EnterTransition.None else fadeIn(),
                exit = if (LocalAsterReduceMotion.current) androidx.compose.animation.ExitTransition.None else fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = 10.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = StatusSurface,
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.16f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .height(24.dp)
                        .testTag("reader_mini_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${activeChapterTitle.take(6)} · $currentPage/$totalPages · ${((currentPage.toFloat() / totalPages) * 100).roundToInt()}%",
                            fontFamily = RobotoMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusText,
                            maxLines = 1,
                            softWrap = false
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { showMiniStatusPill = false }
                                .testTag("reader_mini_status_close_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭状态条",
                                tint = StatusIcon,
                                modifier = Modifier.size(11.dp)
                            )
                        }
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
                selectedTab = tabletSideTab,
                dualPageCoverOffset = dualPageCoverOffset,
                viewModel = viewModel,
                onSelectTab = { tabletSideTab = it },
                onClose = { tabletSideDrawerOpen = false },
                onSelectLayoutMode = { viewModel.setReaderLayoutMode(it) },
                onToggleCoverOffset = {
                    dualPageCoverOffset = it
                    viewModel.setSmartSpreadStitch(it)
                },

                onSelectChapter = { idx, chapter ->
                    currentChapterIndex = idx
                    currentPage = chapter.startPage
                }
            )
        }
    }

    // 漫画阅读设置。
    if (showModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModeSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "漫画阅读设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    IconButton(onClick = { showModeSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }


                    // ================= COMIC-ONLY SETTINGS =================
                    Text(
                        text = "阅读方向与翻页",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            ReaderLayoutMode.VERTICAL_SCROLL to "上下滚动",
                            ReaderLayoutMode.RTL_PAGER to "右向左翻",
                            ReaderLayoutMode.LTR_PAGER to "左向右翻",
                            ReaderLayoutMode.DUAL_PAGE to "双页展开"
                        )
                        modes.forEach { (mode, title) ->
                            val selected = uiState.readerLayoutMode == mode
                            Surface(
                                onClick = { viewModel.setReaderLayoutMode(mode) },
                                shape = MaterialTheme.shapes.medium,
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            ReaderLayoutMode.VERTICAL_SCROLL -> Icons.Outlined.ViewDay
                                            ReaderLayoutMode.DUAL_PAGE -> Icons.Outlined.ViewColumn
                                            else -> Icons.Outlined.ViewArray
                                        },
                                        contentDescription = title,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "画面缩放模式",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("适合屏幕", "原始比例").forEach { fitMode ->
                            val selected = uiState.fitModeLabel == fitMode
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setFitModeLabel(fitMode) },
                                label = { Text(fitMode, maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("封面单页", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = dualPageCoverOffset,
                            onCheckedChange = {
                                dualPageCoverOffset = it
                                viewModel.setSmartSpreadStitch(it)
                            }
                        )
                    }


                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Common Hardware & Display Controls
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("屏幕亮度", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${(readerBrightness * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = RobotoMonoFontFamily,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.BrightnessLow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Slider(
                            value = readerBrightness,
                            onValueChange = { readerBrightness = it },
                            valueRange = 0.25f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Outlined.BrightnessHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("音量键翻页", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = uiState.volumeKeyPageTurn,
                        onCheckedChange = { viewModel.setVolumeKeyPageTurn(it) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("页面状态条", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = showMiniStatusPill,
                        onCheckedChange = { showMiniStatusPill = it }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
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
                    text = "目录 (${chapters.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.height(340.dp),
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
                                    text = "P.${ch.startPage}",
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
        color = if (isActive) ChromePillColor else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) ChromeAccentMint else ChromeTextPrimary,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) ChromeAccentMint else ChromeTextPrimary,
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
    selectedTab: Int,
    dualPageCoverOffset: Boolean,
    viewModel: AsterViewModel,
    onSelectTab: (Int) -> Unit,
    onClose: () -> Unit,
    onSelectLayoutMode: (ReaderLayoutMode) -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        text = "漫画控制面板",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "收起", modifier = Modifier.size(18.dp))
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf("漫画设置", "目录").forEachIndexed { idx, title ->
                        val active = selectedTab == idx
                        Surface(
                            onClick = { onSelectTab(idx) },
                            shape = MaterialTheme.shapes.small,
                            color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (active) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                        Text(
                            text = "翻页布局",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SideModeButton(
                                    title = "双页展开",
                                    selected = uiState.readerLayoutMode == ReaderLayoutMode.DUAL_PAGE,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectLayoutMode(ReaderLayoutMode.DUAL_PAGE) }
                                )
                                SideModeButton(
                                    title = "左向右翻",
                                    selected = uiState.readerLayoutMode == ReaderLayoutMode.LTR_PAGER,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectLayoutMode(ReaderLayoutMode.LTR_PAGER) }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SideModeButton(
                                    title = "上下滚动",
                                    selected = uiState.readerLayoutMode == ReaderLayoutMode.VERTICAL_SCROLL,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectLayoutMode(ReaderLayoutMode.VERTICAL_SCROLL) }
                                )
                                SideModeButton(
                                    title = "右向左翻",
                                    selected = uiState.readerLayoutMode == ReaderLayoutMode.RTL_PAGER,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectLayoutMode(ReaderLayoutMode.RTL_PAGER) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Text(
                            text = "画面缩放",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("适合屏幕", "原始比例").forEach { fitMode ->
                                SideModeButton(
                                    title = fitMode,
                                    selected = uiState.fitModeLabel == fitMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setFitModeLabel(fitMode) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("首页单页偏移", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Switch(checked = dualPageCoverOffset, onCheckedChange = onToggleCoverOffset)
                        }


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
                                    text = "P.${ch.startPage}",
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

@Composable
private fun SideModeButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        )
    }
}
