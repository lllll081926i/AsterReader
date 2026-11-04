package com.example.ui.screens

import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewArray
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AnnotationEntity
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.SeedData
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.ReaderLayoutMode
import com.example.ui.ReaderPaperTheme
import com.example.ui.components.MangaPageCanvas
import com.example.ui.theme.AnnotationBlue
import com.example.ui.theme.AnnotationGreen
import com.example.ui.theme.AnnotationPink
import com.example.ui.theme.AnnotationYellow
import com.example.ui.theme.InkDark
import com.example.ui.theme.InkLight
import com.example.ui.theme.InkWarm
import com.example.ui.theme.LoraFontFamily
import com.example.ui.theme.PaperDarkGray
import com.example.ui.theme.PaperLight
import com.example.ui.theme.PaperOledBlack
import com.example.ui.theme.PaperWarm
import com.example.ui.theme.RobotoMonoFontFamily
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

// Dedicated high-contrast reader chrome palette so titles & controls are 100% readable
// regardless of whether the manga page or system theme is light or dark.
private val ChromeSurfaceColor = Color(0xF2141B18)
private val ChromePillColor = Color(0xFF23302A)
private val ChromeTextPrimary = Color(0xFFF4F8F6)
private val ChromeTextSecondary = Color(0xFFB7CBC1)
private val ChromeAccentMint = Color(0xFF9CD4B8)
private val ChromeBorderColor = Color(0x408AA899)

@OptIn(ExperimentalMaterial3Api::class)
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

    val context = LocalContext.current
    val chapters by remember(book.id) {
        viewModel.observeChapters(book.id)
    }.collectAsState(initial = emptyList())

    val annotations by remember(book.id) {
        viewModel.observeBookAnnotations(book.id)
    }.collectAsState(initial = emptyList())

    val isComic = book.format.equals("CBZ", ignoreCase = true)
    val totalPages = book.totalPages.coerceAtLeast(12)
    var currentPage by remember(book.id, initialPage) {
        mutableIntStateOf((initialPage ?: book.currentPage).coerceIn(1, totalPages))
    }
    var currentChapterIndex by remember(book.id, chapters) {
        val idx = chapters.indexOfLast { it.startPage <= currentPage }.coerceAtLeast(0)
        mutableIntStateOf(idx)
    }

    var showChrome by remember { mutableStateOf(true) }
    var showMiniStatusPill by remember { mutableStateOf(true) }
    var showModeSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showTtsBar by remember { mutableStateOf(false) }
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
    var cropWhiteBorders by remember { mutableStateOf(uiState.autoCropMargins) }
    var dualPageCoverOffset by remember { mutableStateOf(uiState.smartSpreadStitch) }
    var bookmarkedPages by remember { mutableStateOf(setOf(currentPage)) }

    var selectedParagraphForHighlight by remember { mutableStateOf<String?>(null) }
    var highlightColorChoice by remember { mutableStateOf("YELLOW") }
    var highlightNoteInput by remember { mutableStateOf("") }
    var inBookSearchQuery by remember { mutableStateOf("") }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }
    var ttsCurrentParagraphIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.CHINESE
            }
        }
        ttsEngine = engine
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    val activeChapterTitle = chapters.getOrNull(currentChapterIndex)?.title
        ?: book.currentChapterTitle.ifBlank { if (isComic) "第 104 话" else "第一章" }

    LaunchedEffect(currentPage) {
        val chIdx = chapters.indexOfLast { it.startPage <= currentPage }
        if (chIdx >= 0) currentChapterIndex = chIdx
        val chTitle = chapters.getOrNull(currentChapterIndex)?.title
        viewModel.updateReadingProgress(book.id, currentPage, chTitle)
    }

    // Decoupled Reader Paper & Guaranteed High-Contrast Ink Colors
    val isLightPaper = uiState.readerPaperTheme == ReaderPaperTheme.LIGHT_PAPER ||
        uiState.readerPaperTheme == ReaderPaperTheme.WARM_PAPER

    val paperBgColor = when {
        isComic && uiState.readerLayoutMode != ReaderLayoutMode.DUAL_PAGE -> Color(0xFF0E1311)
        else -> when (uiState.readerPaperTheme) {
            ReaderPaperTheme.LIGHT_PAPER -> PaperLight
            ReaderPaperTheme.WARM_PAPER -> PaperWarm
            ReaderPaperTheme.DARK_GRAY -> PaperDarkGray
            ReaderPaperTheme.OLED_BLACK -> PaperOledBlack
        }
    }
    val inkTextColor = when {
        isComic && uiState.readerLayoutMode != ReaderLayoutMode.DUAL_PAGE -> Color(0xFFF2F6F4)
        isLightPaper -> if (uiState.readerPaperTheme == ReaderPaperTheme.WARM_PAPER) InkWarm else InkLight
        else -> InkDark
    }

    val mangaUrls = remember(book.id) {
        listOf(
            SeedData.URL_MANGA_PAGE_MAIN,
            SeedData.URL_MANGA_SPREAD_LEFT,
            SeedData.URL_MANGA_SPREAD_RIGHT,
            SeedData.URL_FRIEREN_11_DETAIL,
            SeedData.URL_DUNGEON_MESHI_12,
            SeedData.URL_MANGA_PAGE_MAIN,
            SeedData.URL_MANGA_SPREAD_LEFT,
            SeedData.URL_MANGA_SPREAD_RIGHT
        )
    }

    val currentChapterParagraphs = remember(book.id, currentChapterIndex, chapters) {
        val rawContent = chapters.getOrNull(currentChapterIndex)?.textContent?.takeIf { it.isNotBlank() }
            ?: """
                在寂静的北方高原边缘，晨雾如同半透明的羊皮纸缓缓铺展。芙莉莲合上厚重的魔法典籍，指尖拂过封面上那枚已经磨损的星辰纹章。
                “所谓的时间，只有在被人铭记的瞬间才具备重量。”她低声说道，目光越过寂静的森林与远方耸立的遗迹。对于精灵而言，百年的岁月不过是一场短暂的午后小憩，但那些与同伴并肩走过的零碎日常，却像深深刻在石碑上的铭文，历经风雨依然清晰可辨。
                当夜幕降临，篝火在废墟中央跳跃出温暖的琥珀色光芒。菲伦整理着行囊中的草药与卷轴，修塔尔克则在擦拭那柄陪伴他度过无数次危机的战斧。旅途并未结束，每一页翻过的篇章都是通往灵魂安眠之地的见证。
            """.trimIndent()
        rawContent.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(paperBgColor)
            .testTag("reader_screen_root")
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (isComic) {
                when (uiState.readerLayoutMode) {
                    ReaderLayoutMode.VERTICAL_SCROLL -> {
                        WebtoonVerticalReader(
                            mangaUrls = mangaUrls,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            activeChapterTitle = activeChapterTitle,
                            cropBorders = cropWhiteBorders,
                            onToggleChrome = { showChrome = !showChrome },
                            onNextChapter = {
                                if (currentChapterIndex < chapters.lastIndex) {
                                    currentChapterIndex++
                                    currentPage = chapters[currentChapterIndex].startPage
                                }
                            }
                        )
                    }

                    ReaderLayoutMode.RTL_PAGER, ReaderLayoutMode.LTR_PAGER -> {
                        SinglePageComicPager(
                            mangaUrls = mangaUrls,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            isRtl = uiState.readerLayoutMode == ReaderLayoutMode.RTL_PAGER,
                            onPageChanged = { currentPage = it },
                            onToggleChrome = { showChrome = !showChrome }
                        )
                    }

                    ReaderLayoutMode.DUAL_PAGE -> {
                        TabletDualPageSpreadCanvas(
                            book = book,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            activeChapterTitle = activeChapterTitle,
                            paperBgColor = paperBgColor,
                            inkTextColor = inkTextColor,
                            coverOffset = dualPageCoverOffset,
                            onStepSpread = { delta ->
                                currentPage = (currentPage + delta).coerceIn(1, totalPages)
                            },
                            onToggleChrome = { showChrome = !showChrome }
                        )
                    }
                }
            } else {
                ReflowableEbookCanvas(
                    book = book,
                    chapterTitle = activeChapterTitle,
                    paragraphs = currentChapterParagraphs,
                    annotations = annotations,
                    paperBgColor = paperBgColor,
                    inkTextColor = inkTextColor,
                    fontSizeSp = uiState.readerFontSizeSp.toFloat(),
                    lineHeightMultiplier = uiState.readerLineHeightMultiplier,
                    fontFamilyName = uiState.readerFontFamily,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    ttsPlayingIndex = if (isTtsPlaying) ttsCurrentParagraphIndex else -1,
                    onToggleChrome = { showChrome = !showChrome },
                    onStepPage = { delta ->
                        currentPage = (currentPage + delta).coerceIn(1, totalPages)
                    },
                    onParagraphClick = { paragraphText ->
                        selectedParagraphForHighlight = paragraphText
                        highlightNoteInput = ""
                    }
                )
            }

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
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
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
                                            contentDescription = "已加密",
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
                            if (!isComic) {
                                IconButton(
                                    onClick = { showSearchSheet = true },
                                    modifier = Modifier.testTag("reader_in_book_search_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "搜索",
                                        tint = ChromeTextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        showTtsBar = !showTtsBar
                                        if (showTtsBar && !isTtsPlaying) {
                                            isTtsPlaying = true
                                            val textToRead = currentChapterParagraphs.getOrNull(ttsCurrentParagraphIndex) ?: book.title
                                            ttsEngine?.setSpeechRate(ttsSpeed)
                                            ttsEngine?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "aster_tts")
                                        }
                                    },
                                    modifier = Modifier.testTag("reader_tts_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.GraphicEq,
                                        contentDescription = "朗读",
                                        tint = if (showTtsBar) ChromeAccentMint else ChromeTextPrimary
                                    )
                                }
                            }

                            val isBookmarked = currentPage in bookmarkedPages
                            IconButton(
                                onClick = {
                                    bookmarkedPages = if (isBookmarked) {
                                        bookmarkedPages - currentPage
                                    } else {
                                        bookmarkedPages + currentPage
                                    }
                                    if (!isBookmarked) {
                                        viewModel.addAnnotation(
                                            bookId = book.id,
                                            chapterTitle = activeChapterTitle,
                                            pageNumber = currentPage,
                                            quoteText = "P.$currentPage",
                                            noteText = "书签",
                                            colorName = "GREEN",
                                            type = "BOOKMARK"
                                        )
                                    }
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
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
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

            // TTS Playback Floating Controller
            androidx.compose.animation.AnimatedVisibility(
                visible = showTtsBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (showChrome) 158.dp else 44.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ChromeSurfaceColor,
                    contentColor = ChromeTextPrimary,
                    shadowElevation = 10.dp,
                    border = BorderStroke(0.8.dp, ChromeBorderColor),
                    modifier = Modifier.widthIn(max = 440.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = ChromeAccentMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = currentChapterParagraphs.getOrNull(ttsCurrentParagraphIndex) ?: activeChapterTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = ChromeTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    ttsSpeed = when (ttsSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 0.85f
                                        else -> 1.0f
                                    }
                                    ttsEngine?.setSpeechRate(ttsSpeed)
                                }
                            ) {
                                Text(
                                    text = "${ttsSpeed}x",
                                    fontFamily = RobotoMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ChromeAccentMint,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isTtsPlaying) {
                                        ttsEngine?.stop()
                                        isTtsPlaying = false
                                    } else {
                                        isTtsPlaying = true
                                        val txt = currentChapterParagraphs.getOrNull(ttsCurrentParagraphIndex) ?: book.title
                                        ttsEngine?.speak(txt, TextToSpeech.QUEUE_FLUSH, null, "aster_tts")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isTtsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isTtsPlaying) "暂停" else "朗读",
                                    tint = ChromeTextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    ttsEngine?.stop()
                                    isTtsPlaying = false
                                    showTtsBar = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = ChromeTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Unified Bottom Floating Island (Merged Progress Scrub Bar + Bottom Option Buttons)
            androidx.compose.animation.AnimatedVisibility(
                visible = showChrome,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
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
                            value = currentPage.toFloat(),
                            onValueChange = { currentPage = it.roundToInt().coerceIn(1, totalPages) },
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

                        // Bottom Half: Dedicated Comic vs E-Book Reader Option Buttons
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
                            if (isComic) {
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
                                        bookmarkedPages = if (isBookmarked) {
                                            bookmarkedPages - currentPage
                                        } else {
                                            bookmarkedPages + currentPage
                                        }
                                    }
                                )
                            } else {
                                ReaderIslandButton(
                                    icon = Icons.Outlined.FormatSize,
                                    label = "排版设置",
                                    isActive = showModeSheet,
                                    onClick = { showModeSheet = true }
                                )
                                ReaderIslandButton(
                                    icon = Icons.Default.EditNote,
                                    label = "批注",
                                    onClick = {
                                        selectedParagraphForHighlight =
                                            currentChapterParagraphs.firstOrNull() ?: book.title
                                        highlightNoteInput = ""
                                    }
                                )
                                ReaderIslandButton(
                                    icon = Icons.Outlined.GraphicEq,
                                    label = if (showTtsBar) "朗读中" else "朗读",
                                    isActive = showTtsBar,
                                    onClick = {
                                        showTtsBar = !showTtsBar
                                        if (showTtsBar && !isTtsPlaying) {
                                            isTtsPlaying = true
                                            val textToRead = currentChapterParagraphs.getOrNull(ttsCurrentParagraphIndex) ?: book.title
                                            ttsEngine?.setSpeechRate(ttsSpeed)
                                            ttsEngine?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "aster_tts")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Exquisite Closable Floating Mini Status Bar at Bottom-Right Corner
            androidx.compose.animation.AnimatedVisibility(
                visible = showMiniStatusPill,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 12.dp, bottom = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xD9121815),
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
                            color = Color(0xFFEAF2EE),
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
                                tint = Color(0xFFA7B8B0),
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
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut()
        ) {
            TabletReaderSidePanel(
                isComic = isComic,
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                uiState = uiState,
                selectedTab = tabletSideTab,
                dualPageCoverOffset = dualPageCoverOffset,
                cropWhiteBorders = cropWhiteBorders,
                viewModel = viewModel,
                onSelectTab = { tabletSideTab = it },
                onClose = { tabletSideDrawerOpen = false },
                onSelectLayoutMode = { viewModel.setReaderLayoutMode(it) },
                onSelectPaperTheme = { viewModel.setReaderPaperTheme(it) },
                onToggleCoverOffset = {
                    dualPageCoverOffset = it
                    viewModel.setSmartSpreadStitch(it)
                },
                onToggleCropBorders = {
                    cropWhiteBorders = it
                    viewModel.setAutoCropMargins(it)
                },
                onSelectChapter = { idx, chapter ->
                    currentChapterIndex = idx
                    currentPage = chapter.startPage
                }
            )
        }
    }

    // Strictly Separated Comic Settings vs. E-Book Typography Settings Bottom Sheet
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
                        text = if (isComic) "漫画阅读设置" else "电子书排版设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                    IconButton(onClick = { showModeSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                if (isComic) {
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
                                shape = RoundedCornerShape(14.dp),
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
                        listOf("适合宽度", "适合高度", "原始比例").forEach { fitMode ->
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
                        Text("裁切白边", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = cropWhiteBorders,
                            onCheckedChange = {
                                cropWhiteBorders = it
                                viewModel.setAutoCropMargins(it)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("跨页对齐", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = dualPageCoverOffset,
                            onCheckedChange = {
                                dualPageCoverOffset = it
                                viewModel.setSmartSpreadStitch(it)
                            }
                        )
                    }
                } else {
                    // ================= E-BOOK-ONLY SETTINGS =================
                    Text(
                        text = "纸张背景主题",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val papers = listOf(
                            Triple(ReaderPaperTheme.LIGHT_PAPER, "明纸", PaperLight to InkLight),
                            Triple(ReaderPaperTheme.WARM_PAPER, "羊皮", PaperWarm to InkWarm),
                            Triple(ReaderPaperTheme.DARK_GRAY, "深灰", PaperDarkGray to InkDark),
                            Triple(ReaderPaperTheme.OLED_BLACK, "纯黑", PaperOledBlack to InkDark)
                        )
                        papers.forEach { (theme, label, pair) ->
                            val selected = uiState.readerPaperTheme == theme
                            Surface(
                                onClick = { viewModel.setReaderPaperTheme(theme) },
                                shape = RoundedCornerShape(14.dp),
                                color = pair.first,
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Aa",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = LoraFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = pair.second,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = pair.second,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "字体与排版",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "衬线宋体" to "Lora",
                            "无衬线体" to "Roboto Flex",
                            "等宽字体" to "Roboto Mono"
                        ).forEach { (displayName, familyKey) ->
                            val selected = uiState.readerFontFamily == familyKey
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setReaderFontFamily(familyKey) },
                                label = { Text(displayName, maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("字号大小 (${uiState.readerFontSizeSp} sp)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.setReaderFontSize(uiState.readerFontSizeSp - 1) }) {
                                Text("A-", maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(onClick = { viewModel.setReaderFontSize(uiState.readerFontSizeSp + 1) }) {
                                Text("A+", maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "紧凑 1.3x" to 1.3f,
                            "标准 1.65x" to 1.65f,
                            "舒展 1.9x" to 1.9f
                        ).forEach { (label, mult) ->
                            val selected = kotlin.math.abs(uiState.readerLineHeightMultiplier - mult) < 0.05f
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setReaderLineHeight(mult) },
                                label = { Text(label, maxLines = 1, softWrap = false) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
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
                            shape = RoundedCornerShape(14.dp),
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

    // In-Book Full-Text Search Sheet
    if (showSearchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "书内检索",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = inBookSearchQuery,
                    onValueChange = { inBookSearchQuery = it },
                    placeholder = { Text("输入关键词...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                val matches = currentChapterParagraphs.filter {
                    inBookSearchQuery.isBlank() || it.contains(inBookSearchQuery, ignoreCase = true)
                }
                LazyColumn(
                    modifier = Modifier.height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(matches) { idx, paragraph ->
                        Surface(
                            onClick = { showSearchSheet = false },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$activeChapterTitle · #${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = paragraph,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Highlight & Annotation Creation Dialog
    selectedParagraphForHighlight?.let { paragraphText ->
        AlertDialog(
            onDismissRequest = { selectedParagraphForHighlight = null },
            title = { Text("添加批注", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = "“${paragraphText.take(80)}${if (paragraphText.length > 80) "..." else ""}”",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            "YELLOW" to AnnotationYellow,
                            "GREEN" to AnnotationGreen,
                            "BLUE" to AnnotationBlue,
                            "PINK" to AnnotationPink
                        ).forEach { (key, colorVal) ->
                            val isSelected = highlightColorChoice == key
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { highlightColorChoice = key },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF1F2021),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = highlightNoteInput,
                        onValueChange = { highlightNoteInput = it },
                        label = { Text("笔记内容") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addAnnotation(
                            bookId = book.id,
                            chapterTitle = activeChapterTitle,
                            pageNumber = currentPage,
                            quoteText = paragraphText.take(120),
                            noteText = highlightNoteInput.ifBlank { "高亮" },
                            colorName = highlightColorChoice,
                            type = "HIGHLIGHT"
                        )
                        selectedParagraphForHighlight = null
                    }
                ) {
                    Text("保存", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedParagraphForHighlight = null }) {
                    Text("取消", maxLines = 1, softWrap = false)
                }
            }
        )
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
        shape = RoundedCornerShape(999.dp),
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
private fun WebtoonVerticalReader(
    mangaUrls: List<String>,
    currentPage: Int,
    totalPages: Int,
    activeChapterTitle: String,
    cropBorders: Boolean,
    onToggleChrome: () -> Unit,
    onNextChapter: () -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleChrome
            )
            .testTag("webtoon_vertical_list"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(count = 8) { index ->
            val pageNumber = ((currentPage - 1 + index) % totalPages) + 1
            val imageUrl = mangaUrls[index % mangaUrls.size]

            Box(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (cropBorders) 0.dp else 12.dp,
                        vertical = if (cropBorders) 0.dp else 6.dp
                    )
                    .aspectRatio(0.72f)
            ) {
                MangaPageCanvas(
                    imageUrl = imageUrl,
                    pageNumber = pageNumber,
                    chapterTitle = activeChapterTitle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Clean Chapter Transition Card
        item {
            Surface(
                color = Color(0xFF151C19),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFF2B4D40)),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$activeChapterTitle · 完",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF2F6F4)
                    )
                    Button(
                        onClick = onNextChapter,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("下一话", maxLines = 1, softWrap = false)
                    }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

/**
 * 2. Single-Page RTL / LTR Horizontal Pager — Clean images with zero patch number overlays
 */
@Composable
private fun SinglePageComicPager(
    mangaUrls: List<String>,
    currentPage: Int,
    totalPages: Int,
    isRtl: Boolean,
    onPageChanged: (Int) -> Unit,
    onToggleChrome: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = (currentPage - 1).coerceIn(0, totalPages - 1),
        pageCount = { totalPages }
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage + 1)
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }

    HorizontalPager(
        state = pagerState,
        reverseLayout = isRtl,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleChrome() },
                    onDoubleTap = {
                        zoomScale = if (zoomScale > 1.2f) 1f else 1.85f
                    }
                )
            }
            .testTag("single_page_comic_pager")
    ) { pageIdx ->
        val pageNum = pageIdx + 1
        val url = mangaUrls[pageIdx % mangaUrls.size]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            MangaPageCanvas(
                imageUrl = url,
                pageNumber = pageNum,
                chapterTitle = "",
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .aspectRatio(0.71f)
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale
                    )
            )
        }
    }
}

/**
 * 3. Tablet Dual-Page Spread Canvas — High-contrast headers and unobstructed manga pages
 */
@Composable
private fun TabletDualPageSpreadCanvas(
    book: BookEntity,
    currentPage: Int,
    totalPages: Int,
    activeChapterTitle: String,
    paperBgColor: Color,
    inkTextColor: Color,
    coverOffset: Boolean,
    onStepSpread: (Int) -> Unit,
    onToggleChrome: () -> Unit
) {
    val leftPageNum = if (coverOffset) currentPage else (currentPage + 1).coerceAtMost(totalPages)
    val rightPageNum = if (coverOffset) (currentPage + 1).coerceAtMost(totalPages) else currentPage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141A17))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleChrome
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = paperBgColor,
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.16f)),
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxWidth()
                .aspectRatio(1.42f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // LEFT PAGE
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = inkTextColor.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$leftPageNum",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = RobotoMonoFontFamily,
                                color = inkTextColor.copy(alpha = 0.75f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, inkTextColor.copy(alpha = 0.25f))
                        ) {
                            MangaPageCanvas(
                                imageUrl = SeedData.URL_MANGA_SPREAD_LEFT,
                                pageNumber = leftPageNum,
                                chapterTitle = activeChapterTitle,
                                isDarkCanvas = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // RIGHT PAGE
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = activeChapterTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = inkTextColor.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$rightPageNum",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = RobotoMonoFontFamily,
                                color = inkTextColor.copy(alpha = 0.75f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, inkTextColor.copy(alpha = 0.25f))
                        ) {
                            MangaPageCanvas(
                                imageUrl = SeedData.URL_MANGA_SPREAD_RIGHT,
                                pageNumber = rightPageNum,
                                chapterTitle = activeChapterTitle,
                                isDarkCanvas = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Center Binding Spine Crease
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(32.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.24f),
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        // Bottom-left Stepper Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { onStepSpread(-2) },
                shape = CircleShape,
                color = ChromeSurfaceColor,
                contentColor = ChromeTextPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上一页")
                }
            }
            Surface(
                onClick = { onStepSpread(2) },
                shape = CircleShape,
                color = ChromeSurfaceColor,
                contentColor = ChromeTextPrimary,
                shadowElevation = 4.dp,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下一页")
                }
            }
        }
    }
}

/**
 * 4. Tablet Right-Hand Collapsible Supporting Drawer (Separated by Comic vs E-Book)
 */
@Composable
private fun TabletReaderSidePanel(
    isComic: Boolean,
    chapters: List<ChapterEntity>,
    currentChapterIndex: Int,
    uiState: AsterUiState,
    selectedTab: Int,
    dualPageCoverOffset: Boolean,
    cropWhiteBorders: Boolean,
    viewModel: AsterViewModel,
    onSelectTab: (Int) -> Unit,
    onClose: () -> Unit,
    onSelectLayoutMode: (ReaderLayoutMode) -> Unit,
    onSelectPaperTheme: (ReaderPaperTheme) -> Unit,
    onToggleCoverOffset: (Boolean) -> Unit,
    onToggleCropBorders: (Boolean) -> Unit,
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
                        text = if (isComic) "漫画控制面板" else "电子书排版面板",
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
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf(if (isComic) "漫画设置" else "排版设置", "目录").forEachIndexed { idx, title ->
                        val active = selectedTab == idx
                        Surface(
                            onClick = { onSelectTab(idx) },
                            shape = RoundedCornerShape(8.dp),
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
                    if (isComic) {
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
                            listOf("适合宽度", "适合高度", "原始比例").forEach { fitMode ->
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("自动裁切白边", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Switch(checked = cropWhiteBorders, onCheckedChange = onToggleCropBorders)
                        }
                    } else {
                        Text(
                            text = "纸张底色",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple(ReaderPaperTheme.LIGHT_PAPER, "明纸", PaperLight),
                                Triple(ReaderPaperTheme.WARM_PAPER, "羊皮", PaperWarm),
                                Triple(ReaderPaperTheme.DARK_GRAY, "深灰", PaperDarkGray),
                                Triple(ReaderPaperTheme.OLED_BLACK, "纯黑", PaperOledBlack)
                            ).forEach { (theme, label, bg) ->
                                val selected = uiState.readerPaperTheme == theme
                                Surface(
                                    onClick = { onSelectPaperTheme(theme) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = bg,
                                    border = BorderStroke(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (theme == ReaderPaperTheme.DARK_GRAY || theme == ReaderPaperTheme.OLED_BLACK) Color.White else Color.Black,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Text(
                            text = "字体与字号",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("衬线" to "Lora", "无衬线" to "Roboto Flex", "等宽" to "Roboto Mono").forEach { (name, key) ->
                                SideModeButton(
                                    title = name,
                                    selected = uiState.readerFontFamily == key,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setReaderFontFamily(key) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("字号 (${uiState.readerFontSizeSp}sp)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { viewModel.setReaderFontSize(uiState.readerFontSizeSp - 1) }) {
                                    Text("A-", maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(onClick = { viewModel.setReaderFontSize(uiState.readerFontSizeSp + 1) }) {
                                    Text("A+", maxLines = 1, softWrap = false)
                                }
                            }
                        }
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
                            shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(12.dp),
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

/**
 * 5. Reflowable E-Book & PDF Reader Canvas
 */
@Composable
private fun ReflowableEbookCanvas(
    book: BookEntity,
    chapterTitle: String,
    paragraphs: List<String>,
    annotations: List<AnnotationEntity>,
    paperBgColor: Color,
    inkTextColor: Color,
    fontSizeSp: Float,
    lineHeightMultiplier: Float,
    fontFamilyName: String,
    currentPage: Int,
    totalPages: Int,
    ttsPlayingIndex: Int,
    onToggleChrome: () -> Unit,
    onStepPage: (Int) -> Unit,
    onParagraphClick: (String) -> Unit
) {
    val activeFontFamily = when (fontFamilyName) {
        "Lora" -> LoraFontFamily
        "Roboto Mono" -> RobotoMonoFontFamily
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paperBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleChrome
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 70.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = inkTextColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = RobotoMonoFontFamily,
                    color = inkTextColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    softWrap = false
                )
            }

            HorizontalDivider(color = inkTextColor.copy(alpha = 0.14f))

            Text(
                text = chapterTitle,
                fontFamily = LoraFontFamily,
                fontSize = (fontSizeSp + 5f).sp,
                fontWeight = FontWeight.Bold,
                color = inkTextColor,
                lineHeight = ((fontSizeSp + 5f) * 1.3f).sp
            )

            paragraphs.forEachIndexed { idx, paragraph ->
                val matchedAnnotation = annotations.firstOrNull {
                    val sample = it.quoteText.take(12).trim()
                    sample.isNotEmpty() && paragraph.contains(sample)
                }
                val isTtsReading = idx == ttsPlayingIndex

                val highlightBg = when (matchedAnnotation?.colorName) {
                    "YELLOW" -> AnnotationYellow.copy(alpha = 0.55f)
                    "GREEN" -> AnnotationGreen.copy(alpha = 0.55f)
                    "BLUE" -> AnnotationBlue.copy(alpha = 0.55f)
                    "PINK" -> AnnotationPink.copy(alpha = 0.55f)
                    else -> if (isTtsReading) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else Color.Transparent
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(highlightBg)
                        .clickable { onParagraphClick(paragraph) }
                        .padding(vertical = 4.dp, horizontal = if (highlightBg != Color.Transparent) 10.dp else 0.dp)
                ) {
                    Text(
                        text = "　　$paragraph",
                        fontFamily = activeFontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * lineHeightMultiplier).sp,
                        color = inkTextColor
                    )
                    if (matchedAnnotation != null && matchedAnnotation.noteText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = inkTextColor.copy(alpha = 0.75f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = matchedAnnotation.noteText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = inkTextColor.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onStepPage(-1) },
                    enabled = currentPage > 1
                ) {
                    Text("上一页", maxLines = 1, softWrap = false)
                }
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = RobotoMonoFontFamily,
                    color = inkTextColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    softWrap = false
                )
                OutlinedButton(
                    onClick = { onStepPage(1) },
                    enabled = currentPage < totalPages
                ) {
                    Text("下一页", maxLines = 1, softWrap = false)
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
