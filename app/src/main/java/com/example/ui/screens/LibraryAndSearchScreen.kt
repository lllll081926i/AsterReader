package com.example.ui.screens
import com.asterreader.core.designsystem.AsterHeaderRow
import com.asterreader.core.designsystem.AsterHeaderTitleStyle
import com.asterreader.core.designsystem.asterHeaderAction


import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material.icons.outlined.Tune

import com.asterreader.core.designsystem.AsterLayout
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import com.example.ui.components.AsterPairedActions

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.asterreader.core.designsystem.AsterModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import com.asterreader.core.designsystem.rememberAsterSheetState as rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import com.example.data.local.AnnotationEntity
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.ShelfEntity
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.LibraryLayoutMode
import com.example.ui.SortOption
import com.example.ui.SubScreen
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.asterreader.core.designsystem.asterCardInteraction
import com.example.ui.components.AsterBookCover
import com.example.ui.components.AsterEmptyState
import com.example.ui.components.SafeProtectedBookCard
import com.example.ui.components.TranquilDeskEmptyIllustration
import com.example.ui.theme.LocalAsterColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryHomeScreen(
    uiState: AsterUiState,
    books: List<BookEntity>,
    shelves: List<ShelfEntity>,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel,
    onTriggerSystemFilePicker: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddContentSheet by remember { mutableStateOf(false) }
    var showRepoWizardDialog by remember { mutableStateOf(false) }
    var showBatchShelfPicker by remember { mutableStateOf(false) }

    val filteredBooks = remember(books, uiState.libraryQuickStatusFilter) {
        when (uiState.libraryQuickStatusFilter) {
            "未读" -> books.filter { it.progressPercent == 0 && !it.isVaultProtected }
            "在读" -> books.filter { it.progressPercent in 1..99 }
            "已完结" -> books.filter { it.progressPercent >= 100 }
            "PDF" -> books.filter { it.format == "PDF" }
            "CBZ" -> books.filter { it.format == "CBZ" }
            else -> books
        }
    }

    val continueReadingBook = remember(books) {
        books.filter { !it.isVaultProtected && it.progressPercent > 0 }.maxByOrNull { it.lastReadTimestamp }
    }

    val inspectedBook = remember(books, uiState.tabletInspectedBookId) {
        books.find { it.id == uiState.tabletInspectedBookId } ?: books.firstOrNull()
    }
    val inspectedChapters by remember(inspectedBook?.id, isExpandedTablet) {
        if (isExpandedTablet && inspectedBook != null) viewModel.observeChapters(inspectedBook.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val gridColumns = when (uiState.libraryLayoutMode) {
        LibraryLayoutMode.GRID_2 -> GridCells.Adaptive(minSize = 148.dp)
        LibraryLayoutMode.GRID_3 -> GridCells.Adaptive(minSize = 100.dp)
        LibraryLayoutMode.LIST -> GridCells.Fixed(1)
        LibraryLayoutMode.GALLERY -> GridCells.Adaptive(minSize = 260.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
    ) {
        // ====================================================================
        // CENTER / PRIMARY LIBRARY PANE
        // ====================================================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Batch Selection Toolbar (Only shows when in multi-select mode)
                if (uiState.isBatchSelectionMode) {
                    Surface(
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = { viewModel.exitBatchSelection() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "退出多选")
                                }
                                Text(
                                    text = "已选 ${uiState.selectedBookIds.size}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectAllVisibleBooks(filteredBooks.map { it.id }) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("全选", maxLines = 1, softWrap = false)
                                }
                                TextButton(
                                    onClick = { showBatchShelfPicker = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("入架", maxLines = 1, softWrap = false)
                                }
                                IconButton(onClick = { viewModel.batchRemoveSelectedFromLibrary() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "移除",
                                        tint = colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                AsterHeaderRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AsterLayout.MainHeaderHeight)
                        .padding(horizontal = AsterLayout.PagePadding)
                        .testTag("library_main_header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "馆藏",
                            style = AsterHeaderTitleStyle.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${filteredBooks.size} 部",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.outline,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // 切换视图。
                        IconButton(
                            onClick = { viewModel.cycleLibraryLayoutMode() },
                            modifier = Modifier
                                .asterHeaderAction()
                                .testTag("library_layout_cycle_btn")
                        ) {
                            Icon(
                                imageVector = when (uiState.libraryLayoutMode) {
                                    LibraryLayoutMode.GRID_2 -> Icons.Filled.GridView
                                    LibraryLayoutMode.GRID_3 -> Icons.Outlined.ViewModule
                                    LibraryLayoutMode.LIST -> Icons.AutoMirrored.Outlined.ViewList
                                    LibraryLayoutMode.GALLERY -> Icons.Outlined.ViewAgenda
                                },
                                contentDescription = "切换视图",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(AsterLayout.HeaderIconSize)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .asterHeaderAction()
                                    .testTag("scope_overflow_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "更多选项",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(AsterLayout.HeaderIconSize)
                                )
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (uiState.isScanning) "取消扫描" else "刷新书库", maxLines = 1, softWrap = false) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.rescanRepositories()
                                    }
                                )
                                if (uiState.showReadingStats) {
                                    DropdownMenuItem(
                                        text = { Text("阅读统计", maxLines = 1, softWrap = false) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.pushSubScreen(SubScreen.ReadingStats)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("系列与卷", maxLines = 1, softWrap = false) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.pushSubScreen(SubScreen.SeriesList)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("批量管理", maxLines = 1, softWrap = false) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.enterBatchSelection(null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("添加目录", maxLines = 1, softWrap = false) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showRepoWizardDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("WebDAV") },
                                    leadingIcon = { Icon(Icons.Outlined.Dns, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.pushSubScreen(SubScreen.SettingsSection("webdav"))
                                    }
                                )
                            }
                        }

                        // 设置按钮直接置于馆藏行最右侧
                        IconButton(
                            onClick = { viewModel.pushSubScreen(SubScreen.Settings) },
                            modifier = Modifier
                                .asterHeaderAction()
                                .testTag("header_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "设置",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(AsterLayout.HeaderIconSize)
                            )
                        }
                    }
                }

                // Main Scrollable Grid/List Canvas
                LazyVerticalGrid(
                    columns = gridColumns,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (uiState.libraryLayoutMode == LibraryLayoutMode.GRID_3) 10.dp else 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        if (uiState.libraryLayoutMode == LibraryLayoutMode.LIST) 10.dp else 8.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_publication_grid")
                ) {
                    // 扫描进行中：一行细进度条，可点击取消
                    if (uiState.isScanning) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            val total = scanProgress.discovered
                            val progress = if (total > 0) {
                                (scanProgress.processed.toFloat() / total).coerceIn(0f, 1f)
                            } else 0f
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(MaterialTheme.shapes.extraSmall),
                                    color = colorScheme.primary,
                                    trackColor = colorScheme.surfaceContainerHighest
                                )
                                Text(
                                    text = if (total > 0) "${scanProgress.processed}/$total" else "扫描中",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                TextButton(
                                    onClick = { viewModel.cancelScanning() },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) { Text("取消", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                            }
                        }
                    }

                    // 1.5 最近阅读一小行胶囊条 (直接位于标题行下方、书库内容上方)
                    if (continueReadingBook != null && uiState.showRecentReading && (!continueReadingBook.isVaultProtected || uiState.isVaultUnlocked)) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        viewModel.openBookReader(
                                            bookId = continueReadingBook.id,
                                            preferDualPageOnTablet = isExpandedTablet
                                        )
                                    }
                                    .testTag("continue_reading_inline_pill")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "续读: ${continueReadingBook.title}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = if (continueReadingBook.totalPages > 0) "${continueReadingBook.currentPage}/${continueReadingBook.totalPages}页 (${continueReadingBook.progressPercent}%)" else "第 ${continueReadingBook.currentPage} 页 (${continueReadingBook.progressPercent}%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = com.example.ui.theme.RobotoMonoFontFamily,
                                        color = colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // 2. Filter Chips Row (精简为全部/未读/在读)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val inProgressCount = books.count { it.progressPercent in 1..99 }
                        val chips = listOf(
                            "全部格式" to "全部",
                            "未读" to "未读",
                            "在读" to "在读 ($inProgressCount)"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chips.forEach { (key, label) ->
                                val selected = uiState.libraryQuickStatusFilter == key
                                val chipColor by animateColorAsState(
                                    targetValue = if (selected) colorScheme.secondaryContainer else asterColors.surfaceContainerLow,
                                    animationSpec = com.asterreader.core.designsystem.AsterMotion.effectsSpec(LocalAsterReduceMotion.current),
                                    label = "library_chip_color"
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = chipColor,
                                    modifier = Modifier
                                        .heightIn(min = AsterLayout.TouchTarget)
                                        .clip(CircleShape)
                                        .selectable(selected = selected, role = Role.RadioButton, onClick = { viewModel.setLibraryQuickStatusFilter(key) })
                                        .testTag("library_chip_$key")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. All Works Grid / List Items
                    if (filteredBooks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (books.isEmpty()) {
                                AsterEmptyState(
                                    title = "书库还是空的",
                                    description = "导入本地漫画，或添加一个书库目录，书卷就会出现在这里。",
                                    actionLabel = "导入书籍",
                                    onAction = onTriggerSystemFilePicker,
                                    secondaryLabel = "添加目录",
                                    onSecondaryAction = { showRepoWizardDialog = true }
                                )
                            } else {
                                AsterEmptyState(
                                    title = "没有符合条件的作品",
                                    description = "当前筛选条件下没有书卷，换个条件或查看全部。",
                                    actionLabel = "显示全部",
                                    onAction = { viewModel.setLibraryQuickStatusFilter("全部格式") },
                                    actionIcon = Icons.Outlined.RestartAlt
                                )
                            }
                        }
                    } else {
                        items(filteredBooks, key = { it.id }) { book ->
                            val onBookTap = {
                                if (book.isVaultProtected && !uiState.isVaultUnlocked) {
                                    viewModel.requestVaultUnlock {
                                        viewModel.openBookDetail(book.id)
                                    }
                                } else if (uiState.isBatchSelectionMode) {
                                    viewModel.toggleBookBatchSelection(book.id)
                                } else if (isExpandedTablet) {
                                    viewModel.inspectBookOnTablet(book.id)
                                } else {
                                    viewModel.openBookDetail(book.id)
                                }
                            }

                            if (book.isVaultProtected && !uiState.isVaultUnlocked) {
                                when (uiState.libraryLayoutMode) {
                                    LibraryLayoutMode.LIST -> {
                                        HiddenBookListRow(onClick = onBookTap)
                                    }
                                    else -> {
                                        SafeProtectedBookCard(onClick = onBookTap)
                                    }
                                }
                            } else {
                                val isTabletSelected = isExpandedTablet && uiState.tabletInspectedBookId == book.id
                                val isBatchSelected = book.id in uiState.selectedBookIds
                                when (uiState.libraryLayoutMode) {
                                    LibraryLayoutMode.GRID_2 -> {
                                        PublicationGridCard(
                                            book = book,
                                            isCompact = false,
                                            isSelectedRing = isTabletSelected || isBatchSelected,
                                            showBatchCheck = uiState.isBatchSelectionMode,
                                            isBatchChecked = isBatchSelected,
                                            onClick = onBookTap,
                                            onLongClick = { viewModel.enterBatchSelection(book.id) }
                                        )
                                    }
                                    LibraryLayoutMode.GRID_3 -> {
                                        PublicationGridCard(
                                            book = book,
                                            isCompact = true,
                                            isSelectedRing = isTabletSelected || isBatchSelected,
                                            showBatchCheck = uiState.isBatchSelectionMode,
                                            isBatchChecked = isBatchSelected,
                                            onClick = onBookTap,
                                            onLongClick = { viewModel.enterBatchSelection(book.id) }
                                        )
                                    }
                                    LibraryLayoutMode.LIST -> {
                                        PublicationListRowCard(
                                            book = book,
                                            isSelectedRing = isTabletSelected || isBatchSelected,
                                            onClick = onBookTap,
                                            onReadClick = {
                                                viewModel.openBookReader(
                                                    bookId = book.id,
                                                    preferDualPageOnTablet = isExpandedTablet
                                                )
                                            }
                                        )
                                    }
                                    LibraryLayoutMode.GALLERY -> {
                                        PublicationGalleryCard(
                                            book = book,
                                            isSelectedRing = isTabletSelected || isBatchSelected,
                                            onClick = onBookTap,
                                            onReadClick = {
                                                viewModel.openBookReader(
                                                    bookId = book.id,
                                                    preferDualPageOnTablet = isExpandedTablet
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. End of list padding spacer
                }
            }

            // Floating Action Button (+)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { showAddContentSheet = true }
                    .testTag("library_add_fab")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "导入书籍",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // ====================================================================
        // RIGHT PANE: TABLET SUPPORTING PANE
        // ====================================================================
        if (isExpandedTablet && inspectedBook != null && (!inspectedBook.isVaultProtected || uiState.isVaultUnlocked)) {
            VerticalDivider(
                thickness = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
            TabletSupportingDetailPane(
                book = inspectedBook,
                chapters = inspectedChapters,
                isVaultUnlocked = uiState.isVaultUnlocked,
                onToggleFavorite = { viewModel.toggleFavorite(inspectedBook.id) },
                onOpenFullDetail = { viewModel.openBookDetail(inspectedBook.id) },
                onStartReading = { page ->
                    viewModel.openBookReader(
                        bookId = inspectedBook.id,
                        initialPage = page,
                        preferDualPageOnTablet = true
                    )
                },
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
            )
        }
    }

    // Add Content Bottom Sheet
    if (showAddContentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddContentSheet = false },
            containerColor = asterColors.surfaceBright
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "导入书籍",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable {
                            showAddContentSheet = false
                            onTriggerSystemFilePicker()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = colorScheme.primary)
                        Column {
                            Text(
                                text = "导入单本",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                            Text(
                                text = "漫画压缩包 / PDF / 图片",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = asterColors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .clickable {
                            showAddContentSheet = false
                            // 直接触发系统文件管理器选择目录，拒绝手动填写
                            viewModel.addRepositoryAndScan("本地书库", com.example.data.local.LibraryRepository.DEFAULT_SCAN_RULE)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, tint = colorScheme.primary)
                        Column {
                            Text(
                                text = "添加目录",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "在文件管理器中选择书库目录",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        showAddContentSheet = false
                        onTriggerSystemFilePicker()
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("导入本地文件", maxLines = 1, softWrap = false)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Repository Creation Wizard Dialog
    if (showRepoWizardDialog) {
        RepositoryCreationWizardDialog(
            onDismiss = { showRepoWizardDialog = false },
            onConfirmCreate = { name, rule ->
                showRepoWizardDialog = false
                viewModel.addRepositoryAndScan(name, rule)
            }
        )
    }

    // Batch Shelf Picker Dialog
    if (showBatchShelfPicker) {
        AlertDialog(
            onDismissRequest = { showBatchShelfPicker = false },
            title = { Text("加入书架", maxLines = 1) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (shelves.isEmpty()) item { Text("暂无书架，请先在书架页新建书架") }
                    items(shelves, key = { it.id }) { shelf ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = asterColors.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    showBatchShelfPicker = false
                                    viewModel.batchAddSelectedToShelf(shelf.id, shelf.name)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(shelf.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Text(shelf.subtitle, style = MaterialTheme.typography.labelSmall, color = colorScheme.outline, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBatchShelfPicker = false }) {
                    Text("取消", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}

@Composable
private fun ContinueReadingCompactCard(
    book: BookEntity,
    onCardClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = asterColors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .asterCardInteraction(onClick = onCardClick)
            .testTag("continue_reading_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(asterColors.surfaceContainer)
            ) {
                AsterBookCover(
                    coverUrl = book.coverUrl,
                    title = book.title,
                    format = book.format,
                    modifier = Modifier.fillMaxSize()
                )
                LinearProgressIndicator(
                    progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "最近阅读",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (book.totalPages > 0) "第 ${book.currentPage} / ${book.totalPages} 页 · ${book.progressPercent}%" else "第 ${book.currentPage} 页 · ${book.progressPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .heightIn(min = AsterLayout.TouchTarget)
                        .clip(CircleShape)
                        .clickable { onContinueClick() }
                        .testTag("continue_reading_cta_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "继续阅读",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PublicationGridCard(
    book: BookEntity,
    isCompact: Boolean,
    isSelectedRing: Boolean,
    showBatchCheck: Boolean,
    isBatchChecked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (isSelectedRing) {
                    Modifier
                        .background(asterColors.surfaceContainerLow, MaterialTheme.shapes.medium)
                        .border(2.dp, colorScheme.primary, MaterialTheme.shapes.medium)
                        .padding(4.dp)
                } else {
                    Modifier
                }
            )
            .asterCardInteraction(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("book_card_${book.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .background(asterColors.surfaceContainerLow)
        ) {
            AsterBookCover(
                coverUrl = book.coverUrl,
                title = book.title,
                format = book.format,
                modifier = Modifier.fillMaxSize()
            )

            if (book.isVaultProtected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(asterColors.surfaceContainerHighest.copy(alpha = 0.9f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "已隐藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (showBatchCheck) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isBatchChecked) colorScheme.primary else colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBatchChecked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已选中",
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Bottom Overlay: Progress Bar or "未读" pill
            if (book.progressPercent > 0) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = asterColors.primaryFixed,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                    )
                    if (!isCompact) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${book.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = book.currentChapterTitle.take(6),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            } else if (!isCompact) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surface.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "未读",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.outline,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = book.title,
            style = if (isCompact) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold) else MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        if (!isCompact) {
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun PublicationListRowCard(
    book: BookEntity,
    isSelectedRing: Boolean,
    onClick: () -> Unit,
    onReadClick: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = asterColors.surfaceContainerLow,
        border = if (isSelectedRing) BorderStroke(2.dp, colorScheme.primary)
        else BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .asterCardInteraction(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsterBookCover(
                coverUrl = book.coverUrl,
                title = book.title,
                format = book.format,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (book.totalPages > 0) "${book.author} · 第 ${book.currentPage}/${book.totalPages} 页" else "${book.author} · 第 ${book.currentPage} 页",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = colorScheme.primary,
                        trackColor = asterColors.surfaceContainerHighest,
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "${book.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .heightIn(min = AsterLayout.TouchTarget)
                    .clip(CircleShape)
                    .clickable { onReadClick() }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "阅读",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PublicationGalleryCard(
    book: BookEntity,
    isSelectedRing: Boolean,
    onClick: () -> Unit,
    onReadClick: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.large,
        color = asterColors.surfaceContainerLow,
        border = if (isSelectedRing) BorderStroke(2.dp, colorScheme.primary)
        else BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsterBookCover(
                coverUrl = book.coverUrl,
                title = book.title,
                format = book.format,
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.medium)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.synopsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "进度 ${book.progressPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.outline,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .heightIn(min = AsterLayout.TouchTarget)
                            .clip(CircleShape)
                            .clickable { onReadClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "阅读",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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

@Composable
private fun HiddenBookListRow(onClick: () -> Unit) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = asterColors.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .asterCardInteraction(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "已隐藏",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "已隐藏作品",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = "验证后可见",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Right-hand Supporting Inspection Pane on Tablet
 * (Comic type badges removed, progress bar merged into the bottom action bar)
 */
@Composable
private fun TabletSupportingDetailPane(
    book: BookEntity,
    chapters: List<ChapterEntity>,
    isVaultUnlocked: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenFullDetail: () -> Unit,
    onStartReading: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .background(asterColors.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Action Row inside Inspection Pane
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "详情",
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "收藏",
                            tint = if (book.isFavorite) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenFullDetail) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "展开详情",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Scrollable Detail Body (without comic type badges)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = AsterLayout.BottomSpacing),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .width(104.dp)
                            .aspectRatio(2f / 3f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(asterColors.surfaceContainerHighest)
                    ) {
                        AsterBookCover(
                            coverUrl = book.altCoverUrl.ifBlank { book.coverUrl },
                            title = book.title,
                            format = book.format,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.author.isNotBlank()) Text(
                            text = book.author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.fileSizeText} · ${book.totalPages} 页",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Publication Synopsis
                if (book.synopsis.isNotBlank()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "简介",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = book.synopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Quick Chapter List
                if (chapters.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "目录",
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "共 ${chapters.size} 话",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.outline
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = asterColors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            chapters.take(5).forEachIndexed { idx, ch ->
                                if (idx > 0) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (ch.isCurrent) colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            else Color.Transparent
                                        )
                                        .clickable { onStartReading(ch.startPage) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (ch.isCurrent) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (ch.isCurrent) FontWeight.Medium else FontWeight.Normal
                                            ),
                                            color = if (ch.isCurrent) colorScheme.primary else colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "P.${ch.startPage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ch.isCurrent) colorScheme.primary else colorScheme.outline,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // 阅读操作参与列布局，随大字体增高，不覆盖末尾章节。
            Surface(
                shape = MaterialTheme.shapes.large,
                color = asterColors.surfaceContainerHigh,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .testTag("tablet_detail_actions")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = AsterLayout.BottomSpacing)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 ${book.currentPage} / ${book.totalPages} 页",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${book.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .testTag("tablet_start_reading")
                                .weight(1f)
                                .heightIn(min = AsterLayout.TouchTarget)
                                .clip(CircleShape)
                                .clickable { onStartReading(book.currentPage) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().heightIn(min = AsterLayout.TouchTarget).padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (book.progressPercent > 0) "继续阅读" else "开始阅读",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        IconButton(onClick = onOpenFullDetail) {
                            Icon(
                                imageVector = Icons.Outlined.DisplaySettings,
                                contentDescription = "详情",
                                tint = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Search and Multi-Dimensional Filter Screen
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchAndFilterScreen(
    uiState: AsterUiState,
    books: List<BookEntity>,
    annotations: List<AnnotationEntity> = emptyList(),
    openFilterInitially: Boolean,
    initialScope: String = "全部",
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    var showFilterSheet by rememberSaveable { mutableStateOf(openFilterInitially) }
    var searchScopeTab by rememberSaveable(initialScope) { mutableStateOf(initialScope) } // 全部, 漫画, 书签札记
    val keyboard = LocalSoftwareKeyboardController.current

    val request = com.example.ui.LibrarySearchRequest(uiState.searchQuery, searchScopeTab,
        uiState.sortOption, uiState.searchStatusFilter, uiState.searchFormatFilters,
        uiState.searchTagFilters, uiState.searchStorageFilters, uiState.isVaultUnlocked)
    val searchOwner = LocalLifecycleOwner.current
    val results by androidx.compose.runtime.produceState<Pair<Pair<com.example.ui.LibrarySearchRequest,
        Pair<List<BookEntity>, List<AnnotationEntity>>>, com.example.ui.LibrarySearchResult>?>(null, books, annotations, request, searchOwner) {
        searchOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                kotlinx.coroutines.delay(120)
                val result = com.example.ui.searchLibrary(books, annotations, request)
                value = (request to (books to annotations)) to result
                kotlinx.coroutines.awaitCancellation()
            } finally { value = null }
        }
    }
    val currentResult = results?.takeIf {
        it.first.first == request && it.first.second.first === books && it.first.second.second === annotations
    }?.second
    val matchingBooks = currentResult?.books.orEmpty()
    val matchingAnnotations = currentResult?.annotations.orEmpty()

    // 筛选项全部由真实馆藏聚合，避免出现永远命中 0 条的硬编码选项
    val availableTags = remember(books) {
        books.flatMap { it.tagsCsv.split(',', '，') }
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    val availableStorages = remember(books) {
        books.map { it.repositoryName }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableFormats = remember(books) {
        books.map { it.format }
            .filter { it in com.example.data.local.LibraryRepository.READABLE_FORMATS }
            .distinct().sorted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
    ) {
        // Top Search Input Bar
        Column(
            modifier = Modifier
                .widthIn(max = AsterLayout.SearchContentMaxWidth)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .testTag("search_content_header")
                .padding(horizontal = AsterLayout.PagePadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AsterLayout.HeaderHeight)
            ) {
                AsterHeaderRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AsterLayout.HeaderHeight)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "搜索",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.commitSearchHistory(uiState.searchQuery)
                            keyboard?.hide()
                        }),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
                        cursorBrush = SolidColor(colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .semantics { contentDescription = "搜索书名、作者、系列、标签或批注" }
                            .testTag("search_query_input"),
                        decorationBox = { innerTextField ->
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "搜索书名、作者、系列、书签札记...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery("") },
                            modifier = Modifier
                                .asterHeaderAction()
                                .testTag("search_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "清空",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                }
            }
        }

        Row(Modifier.widthIn(max = AsterLayout.SearchContentMaxWidth).fillMaxWidth()
            .align(Alignment.CenterHorizontally).padding(horizontal = AsterLayout.PagePadding),
            verticalAlignment = Alignment.CenterVertically) {
            val scopes = listOf("全部", "漫画", "书签札记")
            com.asterreader.core.designsystem.AsterScopeTabs(listOf("全部", "漫画", "札记"),
                scopes.indexOf(searchScopeTab).coerceAtLeast(0), { searchScopeTab = scopes[it] }, Modifier.weight(1f))
            IconButton(onClick = { showFilterSheet = true }, modifier = Modifier.size(AsterLayout.TouchTarget).testTag("toggle_filter_sheet_btn")) {
                BadgedBox(badge = {
                    if (uiState.activeFilterCount > 0) Badge { Text(uiState.activeFilterCount.toString()) }
                }) {
                    Icon(Icons.Outlined.Tune, "筛选与排序", Modifier.size(AsterLayout.HeaderIconSize), colorScheme.primary)
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .widthIn(max = AsterLayout.SearchContentMaxWidth)
                .fillMaxSize()
                .align(Alignment.CenterHorizontally)
                .testTag("search_results"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Discovery block: Recent Searches & Popular Tags when query is empty
            if (uiState.searchQuery.isBlank()) {
                if (uiState.recentSearches.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "最近搜索",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                IconButton(onClick = viewModel::clearSearchHistory,
                                    modifier = Modifier.size(AsterLayout.TouchTarget)) {
                                    Icon(Icons.Outlined.DeleteOutline, "清空搜索历史", Modifier.size(AsterLayout.HeaderIconSize))
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.recentSearches.forEach { historyItem ->
                                    Surface(
                                        shape = CircleShape,
                                        color = asterColors.surfaceContainerLow,
                                        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .heightIn(min = com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                                            .clip(CircleShape)
                                            .clickable { viewModel.updateSearchQuery(historyItem) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = historyItem,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .clickable { viewModel.removeSearchHistoryItem(historyItem) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "删除记录",
                                                    tint = colorScheme.outline,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 搜索界面精简：仅保留历史搜索，去掉冗余的热门标签展示
                // Recent Searches

            }

            // Books Section
            if (searchScopeTab != "书签札记") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isBlank() && uiState.activeFilterCount == 0) {
                                "全部馆藏 (${matchingBooks.size})"
                            } else {
                                "匹配作品 (${matchingBooks.size})"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (uiState.activeFilterCount > 0 || uiState.searchQuery.isNotBlank()) {
                            Text(
                                text = "重置筛选",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.updateSearchQuery("")
                                        viewModel.resetAllSearchFilters()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (currentResult == null) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    }
                } else if (matchingBooks.isEmpty()) {
                    item {
                        AsterEmptyState(
                            title = if (uiState.searchQuery.isBlank() && uiState.activeFilterCount == 0) "搜索书库" else "未找到匹配的作品",
                            description = if (uiState.searchQuery.isBlank()) "输入书名、作者或标签开始搜索。"
                            else "换个关键词，或放宽筛选条件试试。",
                            compact = true,
                            actionLabel = if (uiState.activeFilterCount > 0) "清除筛选" else null,
                            onAction = if (uiState.activeFilterCount > 0) {
                                { viewModel.resetAllSearchFilters() }
                            } else null,
                            actionIcon = Icons.Outlined.RestartAlt
                        )
                    }
                } else {
                    items(matchingBooks, key = { it.id }) { book ->
                        SearchBookResultCard(
                            book = book,
                            highlightQuery = uiState.searchQuery,
                            onClick = {
                                viewModel.commitSearchHistory(uiState.searchQuery)
                                viewModel.openBookDetail(book.id)
                            },
                            onQuickRead = {
                                viewModel.commitSearchHistory(uiState.searchQuery)
                                viewModel.openBookReader(book.id)
                            }
                        )
                    }
                }
            }

            // Annotations & Bookmarks Section
            if (searchScopeTab == "书签札记" && matchingAnnotations.isEmpty()) {
                item {
                    if (currentResult == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    } else {
                        AsterEmptyState("未找到匹配的书签与札记", "更换关键词，或在阅读时添加书签与札记。", compact = true)
                    }
                }
            }
            if ((searchScopeTab == "全部" || searchScopeTab == "书签札记") && matchingAnnotations.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "书签与札记 (${matchingAnnotations.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                items(matchingAnnotations, key = { it.id }) { annotation ->
                    val parentBook = books.firstOrNull { it.id == annotation.bookId }
                    SearchAnnotationResultCard(
                        annotation = annotation,
                        bookTitle = parentBook?.title ?: "馆藏作品",
                        onClick = {
                            viewModel.commitSearchHistory(uiState.searchQuery)
                            viewModel.openBookReader(
                                bookId = annotation.bookId,
                                initialPage = annotation.pageNumber
                            )
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = asterColors.surfaceBright,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "高级筛选与排序",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                        maxLines = 1
                    )
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    FilterPartitionHeader(
                        icon = Icons.AutoMirrored.Outlined.Sort,
                        title = "排序方式",
                        modeLabel = "单选"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOption.entries.forEach { option ->
                            val selected = uiState.sortOption == option
                            FilterSheetPill(
                                label = option.label,
                                selected = selected,
                                onClick = { viewModel.setSortOption(option) }
                            )
                        }
                    }

                    FilterPartitionHeader(
                        icon = Icons.Outlined.AutoStories,
                        title = "阅读状态",
                        modeLabel = "单选"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("全部", "未读", "在读", "已读完").forEach { status ->
                            val selected = uiState.searchStatusFilter == status
                            FilterSheetPill(
                                label = status,
                                selected = selected,
                                onClick = { viewModel.setSearchStatusFilter(status) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    FilterPartitionHeader(
                        icon = Icons.Outlined.Description,
                        title = "文件格式",
                        modeLabel = "多选"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableFormats.forEach { fmt ->
                            val selected = fmt in uiState.searchFormatFilters
                            FilterSheetPill(
                                label = fmt,
                                selected = selected,
                                onClick = { viewModel.toggleSearchFormatFilter(fmt) }
                            )
                        }
                    }

                    if (availableTags.isNotEmpty()) {
                        FilterPartitionHeader(
                            icon = Icons.Outlined.Tag,
                            title = "题材标签",
                            modeLabel = "多选"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableTags.forEach { tag ->
                                val selected = tag in uiState.searchTagFilters
                                FilterSheetPill(
                                    label = tag,
                                    selected = selected,
                                    onClick = { viewModel.toggleSearchTagFilter(tag) }
                                )
                            }
                        }
                    }

                    if (availableStorages.size > 1) {
                        FilterPartitionHeader(
                            icon = Icons.Outlined.SdCard,
                            title = "存储目录",
                            modeLabel = "多选"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableStorages.forEach { storage ->
                                val selected = storage in uiState.searchStorageFilters
                                FilterSheetPill(
                                    label = storage,
                                    selected = selected,
                                    onClick = { viewModel.toggleSearchStorageFilter(storage) }
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = asterColors.surfaceContainer.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    AsterPairedActions(
                        leftLabel = "重置", leftIcon = Icons.Outlined.RestartAlt,
                        onLeft = viewModel::resetAllSearchFilters,
                        rightLabel = "查看 ${matchingBooks.size} 部", rightIcon = Icons.Outlined.FilterAlt,
                        onRight = { showFilterSheet = false }, primaryOnRight = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("apply_filter_sheet_btn")
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPartitionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modeLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Text(
            text = modeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun FilterSheetPill(
    label: String,
    selected: Boolean,
    usePrimaryHighlight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    val bg = when {
        selected && usePrimaryHighlight -> colorScheme.primaryContainer
        selected -> colorScheme.secondaryContainer
        else -> asterColors.surfaceContainer
    }
    val fg = when {
        selected && usePrimaryHighlight -> colorScheme.onPrimaryContainer
        selected -> colorScheme.onSecondaryContainer
        else -> colorScheme.onSurfaceVariant
    }

    Surface(
        shape = CircleShape,
        color = bg,
        border = if (selected && !usePrimaryHighlight) BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.25f)) else null,
        modifier = modifier
            .heightIn(min = AsterLayout.TouchTarget)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.Checkbox, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun SearchBookResultCard(
    book: BookEntity,
    highlightQuery: String,
    onClick: () -> Unit,
    onQuickRead: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.large,
        color = asterColors.surfaceContainerLow,
        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .asterCardInteraction(onClick = onClick)
            .testTag("search_result_${book.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(104.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(asterColors.surfaceContainerHigh)
            ) {
                AsterBookCover(
                    coverUrl = book.coverUrl,
                    title = book.title,
                    format = book.format,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(104.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = book.format,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        val annotatedTitle = buildAnnotatedString {
                            val q = highlightQuery.trim()
                            val idx = if (q.isNotBlank()) book.title.indexOf(q, ignoreCase = true) else -1
                            if (idx >= 0) {
                                append(book.title.substring(0, idx))
                                withStyle(SpanStyle(color = colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                    append(book.title.substring(idx, idx + q.length))
                                }
                                append(book.title.substring(idx + q.length))
                            } else {
                                append(book.title)
                            }
                        }
                        Text(
                            text = annotatedTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "${book.author} · ${book.series}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when {
                                book.progressPercent >= 100 -> "已读完"
                                book.progressPercent > 0 -> "第 ${book.currentPage}/${book.totalPages} 页"
                                else -> "未读"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (book.progressPercent > 0) colorScheme.primary else colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${book.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = if (book.progressPercent >= 100) colorScheme.secondary else colorScheme.primary,
                        trackColor = asterColors.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .heightIn(min = AsterLayout.TouchTarget)
                    .clip(CircleShape)
                    .clickable { onQuickRead() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "阅读",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchAnnotationResultCard(
    annotation: AnnotationEntity,
    bookTitle: String,
    onClick: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = asterColors.surfaceContainerLow,
        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .asterCardInteraction(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmarks,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = bookTitle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${annotation.chapterTitle} · P.${annotation.pageNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = "“${annotation.quoteText}”",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (annotation.noteText.isNotBlank()) {
                Text(
                    text = annotation.noteText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RepositoryCreationWizardDialog(
    onDismiss: () -> Unit,
    onConfirmCreate: (String, String) -> Unit
) {
    var repoName by remember { mutableStateOf("本地书库") }
    val scanRule = com.example.data.local.LibraryRepository.DEFAULT_SCAN_RULE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "添加目录", fontWeight = FontWeight.Bold, maxLines = 1)
        },
        text = {
            OutlinedTextField(
                value = repoName,
                onValueChange = { repoName = it },
                label = { Text("目录名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirmCreate(repoName, scanRule) }) {
                Text("添加", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", maxLines = 1, softWrap = false)
            }
        }
    )
}
