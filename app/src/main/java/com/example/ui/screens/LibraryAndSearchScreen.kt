package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.ui.components.AsterBookCover
import com.example.ui.components.SafeProtectedBookCard
import com.example.ui.components.TranquilDeskEmptyIllustration
import com.example.ui.theme.LocalAsterColors

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddContentSheet by remember { mutableStateOf(false) }
    var showRepoWizardDialog by remember { mutableStateOf(false) }
    var showBatchShelfPicker by remember { mutableStateOf(false) }

    val filteredBooks = remember(books, uiState.libraryQuickStatusFilter) {
        when (uiState.libraryQuickStatusFilter) {
            "未读" -> books.filter { it.progressPercent == 0 && !it.isVaultProtected }
            "在读" -> books.filter { it.progressPercent in 1..99 }
            "已完结" -> books.filter { it.progressPercent >= 100 }
            "EPUB" -> books.filter { it.format == "EPUB" || it.format == "MOBI" }
            "CBZ" -> books.filter { it.format == "CBZ" }
            else -> books
        }
    }

    val continueReadingBook = remember(books) {
        books.firstOrNull { it.id == "dungeon_meshi_12" }
            ?: books.firstOrNull { !it.isVaultProtected && it.progressPercent > 0 }
    }

    val inspectedBook = remember(books, uiState.tabletInspectedBookId) {
        books.find { it.id == uiState.tabletInspectedBookId } ?: books.firstOrNull()
    }
    val inspectedChapters by remember(inspectedBook?.id) {
        viewModel.observeChapters(inspectedBook?.id ?: "frieren_11")
    }.collectAsState(initial = emptyList())

    val gridColumns = when (uiState.libraryLayoutMode) {
        LibraryLayoutMode.GRID_2 -> GridCells.Fixed(if (isExpandedTablet) 3 else 2)
        LibraryLayoutMode.GRID_3 -> GridCells.Fixed(if (isExpandedTablet) 4 else 3)
        LibraryLayoutMode.LIST -> GridCells.Fixed(1)
        LibraryLayoutMode.GALLERY -> GridCells.Fixed(if (isExpandedTablet) 2 else 1)
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
                // TopAppBar or Batch Selection Toolbar
                // Search & Appearance buttons removed; Settings placed at the far top-right
                if (uiState.isBatchSelectionMode) {
                    Surface(
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
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
                                TextButton(
                                    onClick = { viewModel.batchEncryptSelectedToVault() },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("加密", maxLines = 1, softWrap = false)
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
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Aster Reader",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )

                        IconButton(
                            onClick = { viewModel.pushSubScreen(SubScreen.Settings) },
                            modifier = Modifier.testTag("header_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "设置",
                                tint = colorScheme.onSurface
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
                        bottom = 136.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (uiState.libraryLayoutMode == LibraryLayoutMode.GRID_3) 10.dp else 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        if (uiState.libraryLayoutMode == LibraryLayoutMode.LIST) 10.dp else 14.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("library_publication_grid")
                ) {
                    // 1. Scope Header ("全部作品") + Single Small Icon Layout Switcher
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "全部作品",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
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
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Single Small Icon Button to Cycle Home Layout Mode
                                IconButton(
                                    onClick = { viewModel.cycleLibraryLayoutMode() },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("library_layout_cycle_btn")
                                ) {
                                    Icon(
                                        imageVector = when (uiState.libraryLayoutMode) {
                                            LibraryLayoutMode.GRID_2 -> Icons.Filled.GridView
                                            LibraryLayoutMode.GRID_3 -> Icons.Outlined.ViewModule
                                            LibraryLayoutMode.LIST -> Icons.AutoMirrored.Outlined.ViewList
                                            LibraryLayoutMode.GALLERY -> Icons.Outlined.ViewAgenda
                                        },
                                        contentDescription = "切换排列 (${uiState.libraryLayoutMode.label})",
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { showOverflowMenu = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("scope_overflow_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = "更多选项",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("刷新书库", maxLines = 1, softWrap = false) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.rescanRepositories()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("批量管理", maxLines = 1, softWrap = false) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.enterBatchSelection()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("隐私安全", maxLines = 1, softWrap = false) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.pushSubScreen(SubScreen.PrivacySecuritySettings)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("欢迎向导", maxLines = 1, softWrap = false) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.pushSubScreen(SubScreen.Onboarding)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Filter Chips Row
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val inProgressCount = books.count { it.progressPercent in 1..99 }
                        val chips = listOf(
                            "全部格式" to "全部",
                            "未读" to "未读",
                            "在读" to "在读 ($inProgressCount)",
                            "已完结" to "已完结",
                            "CBZ" to "漫画",
                            "EPUB" to "电子书"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chips.forEach { (key, label) ->
                                val selected = uiState.libraryQuickStatusFilter == key
                                Surface(
                                    shape = CircleShape,
                                    color = if (selected) colorScheme.secondaryContainer else asterColors.surfaceContainerLow,
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.setLibraryQuickStatusFilter(key) }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TranquilDeskEmptyIllustration(size = 140.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "暂无书卷",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(
                                    onClick = { viewModel.setLibraryQuickStatusFilter("全部格式") },
                                    shape = CircleShape
                                ) {
                                    Text("显示全部", maxLines = 1, softWrap = false)
                                }
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
                                        EncryptedBookListRow(onClick = onBookTap)
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

                    // 4. Recent Reading Section Placed Below "全部作品" (Can be toggled off in Settings)
                    if (uiState.showRecentReading && continueReadingBook != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "最近阅读",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "可在设置中关闭",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.outline,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                ContinueReadingCompactCard(
                                    book = continueReadingBook,
                                    onCardClick = { viewModel.openBookDetail(continueReadingBook.id) },
                                    onContinueClick = {
                                        viewModel.openBookReader(
                                            bookId = continueReadingBook.id,
                                            preferDualPageOnTablet = isExpandedTablet
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Action Button (+)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.primaryContainer,
                contentColor = colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 84.dp)
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
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
        if (isExpandedTablet && inspectedBook != null) {
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
                    text = "导入本地书籍",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
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
                                text = "选择本地文件",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSecondaryContainer,
                                maxLines = 1
                            )
                            Text(
                                text = "EPUB / CBZ / CBR / PDF / MOBI / TXT",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = asterColors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            showAddContentSheet = false
                            showRepoWizardDialog = true
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
                                text = "挂载本地目录",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "自动扫描目录并建立索引",
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
                        viewModel.importLocalFileUri(null, "星际漫游指南")
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("示例导入《星际漫游指南.epub》", maxLines = 1, softWrap = false)
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shelves.forEach { shelf ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = asterColors.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
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
                                Text(shelf.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
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
        shape = RoundedCornerShape(16.dp),
        color = asterColors.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
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
                    .clip(RoundedCornerShape(12.dp))
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
                    text = "第 ${book.currentPage} / ${book.totalPages} 页 · ${book.progressPercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimary,
                    modifier = Modifier
                        .height(34.dp)
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
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isSelectedRing) {
                    Modifier
                        .background(asterColors.surfaceContainerLow, RoundedCornerShape(14.dp))
                        .border(2.dp, colorScheme.primary, RoundedCornerShape(14.dp))
                        .padding(4.dp)
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("book_card_${book.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
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
                        text = "已加密",
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
        shape = RoundedCornerShape(16.dp),
        color = asterColors.surfaceContainerLow,
        border = if (isSelectedRing) BorderStroke(2.dp, colorScheme.primary)
        else BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
                    .clip(RoundedCornerShape(10.dp))
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
                    text = "${book.author} · 第 ${book.currentPage}/${book.totalPages} 页",
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
                    .height(34.dp)
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
        shape = RoundedCornerShape(20.dp),
        color = asterColors.surfaceContainerLow,
        border = if (isSelectedRing) BorderStroke(2.dp, colorScheme.primary)
        else BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
                    .clip(RoundedCornerShape(12.dp))
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
                        contentColor = colorScheme.onPrimary,
                        modifier = Modifier
                            .height(32.dp)
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
private fun EncryptedBookListRow(onClick: () -> Unit) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = asterColors.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
                            contentDescription = "已加密",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "已加密书籍",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = "点击解锁",
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
                    .padding(start = 20.dp, end = 20.dp, bottom = 164.dp),
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
                            .clip(RoundedCornerShape(12.dp))
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
                        Text(
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        shape = RoundedCornerShape(16.dp),
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
        }

        // Merged Progress Bar + Action Island inside Tablet Supporting Pane
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = asterColors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 80.dp)
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
                        contentColor = colorScheme.onPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(CircleShape)
                            .clickable { onStartReading(book.currentPage) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
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

/**
 * Search and Multi-Dimensional Filter Screen
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchAndFilterScreen(
    uiState: AsterUiState,
    books: List<BookEntity>,
    annotations: List<AnnotationEntity> = emptyList(),
    openFilterInitially: Boolean,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    var showFilterSheet by remember { mutableStateOf(openFilterInitially) }
    var searchScopeTab by remember { mutableStateOf("全部") } // 全部, 漫画, 电子书, 书签札记

    val matchingBooks = remember(
        books,
        uiState.searchQuery,
        uiState.sortOption,
        uiState.searchStatusFilter,
        uiState.searchFormatFilters,
        uiState.searchTagFilters,
        uiState.searchStorageFilters,
        uiState.isVaultUnlocked,
        searchScopeTab
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        books.filter { book ->
            if (book.isVaultProtected && !uiState.isVaultUnlocked) return@filter false

            val matchesScope = when (searchScopeTab) {
                "漫画" -> book.format.equals("CBZ", ignoreCase = true)
                "电子书" -> !book.format.equals("CBZ", ignoreCase = true)
                "书签札记" -> false
                else -> true
            }
            if (!matchesScope) return@filter false

            val matchesQuery = q.isBlank() ||
                book.title.lowercase().contains(q) ||
                book.author.lowercase().contains(q) ||
                book.series.lowercase().contains(q) ||
                book.tagsCsv.lowercase().contains(q) ||
                book.format.lowercase().contains(q)

            val matchesStatus = uiState.searchStatusFilter == "全部" ||
                book.statusBadge == uiState.searchStatusFilter

            val matchesFormat = uiState.searchFormatFilters.isEmpty() ||
                book.format in uiState.searchFormatFilters

            val matchesTag = uiState.searchTagFilters.isEmpty() ||
                uiState.searchTagFilters.any { tag -> book.tagsCsv.contains(tag) }

            val matchesStorage = uiState.searchStorageFilters.isEmpty() ||
                book.repositoryName in uiState.searchStorageFilters

            matchesQuery && matchesStatus && matchesFormat && matchesTag && matchesStorage
        }.let { list ->
            when (uiState.sortOption) {
                SortOption.RECENT_READ -> list.sortedByDescending { it.lastReadTimestamp }
                SortOption.RECENT_ADDED -> list.sortedByDescending { it.id }
                SortOption.TITLE_AZ -> list.sortedBy { it.title }
                SortOption.FILE_SIZE -> list.sortedByDescending { it.totalPages }
                SortOption.PROGRESS -> list.sortedByDescending { it.progressPercent }
            }
        }
    }

    val matchingAnnotations = remember(annotations, books, uiState.searchQuery, searchScopeTab, uiState.isVaultUnlocked) {
        if (searchScopeTab != "全部" && searchScopeTab != "书签札记") return@remember emptyList()
        val q = uiState.searchQuery.trim().lowercase()
        annotations.filter { item ->
            val parentBook = books.firstOrNull { it.id == item.bookId }
            if (parentBook?.isVaultProtected == true && !uiState.isVaultUnlocked) return@filter false
            q.isBlank() ||
                item.quoteText.lowercase().contains(q) ||
                item.noteText.lowercase().contains(q) ||
                item.chapterTitle.lowercase().contains(q) ||
                (parentBook?.title?.lowercase()?.contains(q) == true)
        }
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
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
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
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
                        cursorBrush = SolidColor(colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
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
                                .size(34.dp)
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

                    Surface(
                        shape = CircleShape,
                        color = colorScheme.secondaryContainer,
                        contentColor = colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { showFilterSheet = true }
                            .testTag("toggle_filter_sheet_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "筛选",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Scope Switcher Tabs + Active Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .align(Alignment.CenterHorizontally)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("全部", "漫画", "电子书", "书签札记").forEach { tab ->
                val isSelected = searchScopeTab == tab
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) colorScheme.primary else asterColors.surfaceContainer,
                    contentColor = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .clickable { searchScopeTab = tab }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            VerticalDivider(modifier = Modifier.height(18.dp), color = colorScheme.outlineVariant.copy(alpha = 0.45f))

            Surface(
                shape = CircleShape,
                color = colorScheme.secondaryContainer,
                contentColor = colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .clickable { showFilterSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text("筛选", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                    if (uiState.activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.activeFilterCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colorScheme.onPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainerLow,
                border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .clickable { showFilterSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = uiState.sortOption.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.CenterHorizontally),
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
                                Text(
                                    text = "清空",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { viewModel.clearSearchHistory() }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
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
                                            .height(32.dp)
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
                                                    .size(20.dp)
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

                // Hot Tag Discovery Pills
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tag,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "热门标签",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("奇幻与冒险", "科幻经典", "建筑空间", "日常与料理").forEach { tag ->
                                val isSelected = tag in uiState.searchTagFilters
                                FilterSheetPill(
                                    label = "# $tag",
                                    selected = isSelected,
                                    usePrimaryHighlight = true,
                                    onClick = { viewModel.toggleSearchTagFilter(tag) }
                                )
                            }
                        }
                    }
                }
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

                if (matchingBooks.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = asterColors.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TranquilDeskEmptyIllustration(modifier = Modifier.size(96.dp))
                                Text(
                                    text = "未找到匹配的作品",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
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

            item { Spacer(modifier = Modifier.height(100.dp)) }
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
                        icon = Icons.Outlined.Sort,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CBZ", "EPUB", "PDF").forEach { fmt ->
                            val selected = fmt in uiState.searchFormatFilters
                            FilterSheetPill(
                                label = fmt,
                                selected = selected,
                                onClick = { viewModel.toggleSearchFormatFilter(fmt) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    FilterPartitionHeader(
                        icon = Icons.Outlined.Tag,
                        title = "题材标签",
                        modeLabel = "多选"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("奇幻与冒险", "科幻经典", "建筑空间", "日常与料理").forEach { tag ->
                            val selected = tag in uiState.searchTagFilters
                            FilterSheetPill(
                                label = tag,
                                selected = selected,
                                onClick = { viewModel.toggleSearchTagFilter(tag) }
                            )
                        }
                    }

                    FilterPartitionHeader(
                        icon = Icons.Outlined.SdCard,
                        title = "存储目录",
                        modeLabel = "多选"
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("内部存储 / Books", "SD 扩展卡 / MangaArchive").forEach { storage ->
                            val selected = storage in uiState.searchStorageFilters
                            FilterSheetPill(
                                label = storage,
                                selected = selected,
                                onClick = { viewModel.toggleSearchStorageFilter(storage) }
                            )
                        }
                    }
                }

                Surface(
                    color = asterColors.surfaceContainer.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetAllSearchFilters() },
                            shape = CircleShape,
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重置", maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = { showFilterSheet = false },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("apply_filter_sheet_btn")
                        ) {
                            Icon(Icons.Outlined.FilterAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("查看 ${matchingBooks.size} 部", maxLines = 1, softWrap = false)
                        }
                    }
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
            .height(34.dp)
            .clip(CircleShape)
            .clickable { onClick() }
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
                maxLines = 1,
                softWrap = false
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
        shape = RoundedCornerShape(18.dp),
        color = asterColors.surfaceContainerLow,
        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
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
                    .clip(RoundedCornerShape(10.dp))
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
                            shape = RoundedCornerShape(5.dp),
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
                    .height(34.dp)
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
        shape = RoundedCornerShape(16.dp),
        color = asterColors.surfaceContainerLow,
        border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
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
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme
    var step by remember { mutableIntStateOf(1) }
    var repoName by remember { mutableStateOf("SD 扩展卡 / MangaArchive") }
    var scanRule by remember { mutableStateOf("按目录识别作品") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) {
                    1 -> "选择本地目录 (1/3)"
                    2 -> "扫描规则 (2/3)"
                    else -> "扫描预览 (3/3)"
                },
                maxLines = 1
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (step) {
                    1 -> {
                        OutlinedTextField(
                            value = repoName,
                            onValueChange = { repoName = it },
                            label = { Text("目录名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        listOf(
                            "按目录识别作品",
                            "单文件单作品",
                            "按卷号自动合并系列"
                        ).forEach { rule ->
                            val selected = scanRule == rule
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) colorScheme.secondaryContainer else asterColors.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { scanRule = rule }
                            ) {
                                Text(
                                    text = rule,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurface,
                                    maxLines = 1,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = asterColors.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "识别结果：12 部作品 · 96 话",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colorScheme.primary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "├─ 葬送的芙莉莲 (11 卷)\n├─ 迷宫饭 (12 卷)\n└─ 基地三部曲.epub",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step < 3) {
                Button(onClick = { step++ }) {
                    Text("下一步", maxLines = 1, softWrap = false)
                }
            } else {
                Button(onClick = { onConfirmCreate(repoName, scanRule) }) {
                    Text("完成", maxLines = 1, softWrap = false)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", maxLines = 1, softWrap = false)
            }
        }
    )
}
