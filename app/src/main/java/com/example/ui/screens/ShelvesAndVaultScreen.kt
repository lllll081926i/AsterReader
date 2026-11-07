package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookEntity
import com.example.data.local.RepositoryEntity
import com.example.data.local.ShelfEntity
import com.example.data.local.SystemBiometrics
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ShelvesViewMode
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.AsterBookCover
import com.example.ui.components.AsterSettingsGroup
import com.example.ui.components.AsterSettingsRow
import com.example.ui.components.AsterSwitchRow
import com.example.ui.components.AsterSubScreenHeader
import com.example.ui.components.BotanicalGrimoireIllustration
import com.example.ui.components.SafeProtectedBookCard
import com.example.ui.components.TranquilDeskEmptyIllustration
import com.example.ui.components.VaultGeometricShieldIllustration
import com.example.ui.theme.RobotoMonoFontFamily

/**
 * 1. Shelves & Collections Screen
 */
@Composable
fun ShelvesScreen(
    shelves: List<ShelfEntity>,
    allBooks: List<BookEntity>,
    annotationsCount: Int,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newShelfName by remember { mutableStateOf("") }
    var newShelfSubtitle by remember { mutableStateOf("") }
    var newShelfPrivate by remember { mutableStateOf(false) }

    var activeQuickFilter by remember { mutableStateOf<String?>(null) }
    var showShelfOrderDialog by remember { mutableStateOf(false) }

    val shelvesGridColumns = when (uiState.shelvesViewMode) {
        ShelvesViewMode.COLLAGE_GRID -> if (isExpandedTablet) GridCells.Adaptive(minSize = 175.dp) else GridCells.Fixed(2)
        ShelvesViewMode.COMPACT_GRID -> if (isExpandedTablet) GridCells.Adaptive(minSize = 120.dp) else GridCells.Fixed(3)
        ShelvesViewMode.HORIZONTAL_LIST, ShelvesViewMode.BOOKSHELF_ROW -> if (isExpandedTablet) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("shelves_screen")
    ) {
        // Top Header with Title on Left and Actions + Settings Button at Far Top-Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = com.asterreader.core.designsystem.AsterLayout.HeaderHeight)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "书架",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 排序入口。
                Surface(
                    onClick = { showShelfOrderDialog = true },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "排序与删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 新建书架。
                Surface(
                    onClick = { showCreateDialog = true },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                        .testTag("create_shelf_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 设置入口。
                IconButton(
                    onClick = { viewModel.pushSubScreen(SubScreen.Settings) },
                    modifier = Modifier
                        .size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                        .testTag("shelves_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = shelvesGridColumns,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section 1: Quick Access Grid
            item(span = { GridItemSpan(maxLineSpan) }) {
                val totalCount = allBooks.size
                val favCount = allBooks.count { it.isFavorite && (!it.isVaultProtected || uiState.isVaultUnlocked) }
                val historyCount = allBooks.count { it.progressPercent > 0 && (!it.isVaultProtected || uiState.isVaultUnlocked) }

                val quickTiles = listOf(
                    QuickTileData("ALL", "全部馆藏", "$totalCount 本", Icons.AutoMirrored.Outlined.LibraryBooks),
                    QuickTileData("FAV", "我的收藏", "$favCount 本", Icons.Default.Favorite),
                    QuickTileData("HISTORY", "最近阅读", "$historyCount 本", Icons.Default.History),
                    QuickTileData("BOOKMARKS", "书签札记", "$annotationsCount 条", Icons.Default.Bookmark)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "快捷分类",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        quickTiles.take(2).forEach { tile ->
                            QuickCategoryTile(
                                tile = tile,
                                isSelected = activeQuickFilter == tile.key,
                                onClick = {
                                    if (tile.key == "BOOKMARKS") viewModel.openSearch(initialQuery = "", initialScope = "书签札记")
                                    else activeQuickFilter = if (activeQuickFilter == tile.key) null else tile.key
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        quickTiles.drop(2).forEach { tile ->
                            QuickCategoryTile(
                                tile = tile,
                                isSelected = activeQuickFilter == tile.key,
                                onClick = {
                                    if (tile.key == "BOOKMARKS") viewModel.openSearch(initialQuery = "", initialScope = "书签札记")
                                    else activeQuickFilter = if (activeQuickFilter == tile.key) null else tile.key
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Optional Expanded Quick Filter Preview Row
            if (activeQuickFilter != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val filteredBooks = when (activeQuickFilter) {
                        "FAV" -> allBooks.filter { it.isFavorite && (!it.isVaultProtected || uiState.isVaultUnlocked) }
                        "HISTORY" -> allBooks.filter { it.progressPercent > 0 && (!it.isVaultProtected || uiState.isVaultUnlocked) }
                        else -> allBooks.filter { !it.isVaultProtected || uiState.isVaultUnlocked }
                    }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "筛选结果 (${filteredBooks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                TextButton(onClick = { activeQuickFilter = null }) {
                                    Text("收起", maxLines = 1, softWrap = false)
                                }
                            }
                            filteredBooks.take(4).forEach { b ->
                                Surface(
                                    onClick = { viewModel.openBookDetail(b.id) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsterBookCover(
                                            coverUrl = b.coverUrl,
                                            title = b.title,
                                            format = b.format,
                                            modifier = Modifier
                                                .width(42.dp)
                                                .aspectRatio(0.71f)
                                                .clip(MaterialTheme.shapes.extraSmall)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(b.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${b.author} · ${b.updatedTimeText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2 Header + Single Icon Layout Switcher
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = com.asterreader.core.designsystem.AsterLayout.HeaderHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的书架 (${shelves.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )

                    // 单个纯图标切换按钮，无多余文字，统一 36dp 尺寸
                    IconButton(
                        onClick = { viewModel.cycleShelvesViewMode() },
                        modifier = Modifier
                            .size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                            .testTag("shelves_layout_switcher")
                    ) {
                        val icon = when (uiState.shelvesViewMode) {
                            ShelvesViewMode.COLLAGE_GRID -> Icons.Outlined.GridView
                            ShelvesViewMode.COMPACT_GRID -> Icons.Outlined.Apps
                            ShelvesViewMode.HORIZONTAL_LIST -> Icons.AutoMirrored.Outlined.ViewList
                            ShelvesViewMode.BOOKSHELF_ROW -> Icons.Outlined.ViewCarousel
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "切换视图",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            items(shelves, key = { it.id }) { shelf ->
                val isLockedPrivateShelf = shelf.isProtected && !uiState.isVaultUnlocked
                val shelfBooks = allBooks.filter { book ->
                    book.shelfIdsCsv.split(",").map { it.trim() }.contains(shelf.id)
                }
                // 书架封面优先用手工配置，缺省时回落到成员作品封面
                val collageUrls = shelf.coverUrlsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    .ifEmpty { shelfBooks.map { it.coverUrl }.filter { it.isNotBlank() } }
                val shelfBookCount = shelfBooks.size.coerceAtLeast(shelf.itemCountDisplay)

                val openShelfAction = {
                    if (isLockedPrivateShelf) {
                        viewModel.requestVaultUnlock {
                            viewModel.pushSubScreen(
                                SubScreen.ShelfDetail(
                                    shelfId = shelf.id,
                                    title = shelf.name,
                                    subtitle = shelf.subtitle
                                )
                            )
                        }
                    } else {
                        viewModel.pushSubScreen(
                            SubScreen.ShelfDetail(
                                shelfId = shelf.id,
                                title = shelf.name,
                                subtitle = shelf.subtitle
                            )
                        )
                    }
                }

                when (uiState.shelvesViewMode) {
                    ShelvesViewMode.COLLAGE_GRID -> {
                        ShelfCollageGridCard(
                            shelf = shelf,
                            collageUrls = collageUrls,
                            shelfBookCount = shelfBookCount,
                            isLockedPrivateShelf = isLockedPrivateShelf,
                            isVaultUnlocked = uiState.isVaultUnlocked,
                            isReorderMode = false,
                            onOpenShelf = openShelfAction,
                            onDeleteShelf = { viewModel.deleteCustomShelf(shelf.id) }
                        )
                    }

                    ShelvesViewMode.COMPACT_GRID -> {
                        ShelfCompactGridCard(
                            shelf = shelf,
                            collageUrls = collageUrls,
                            shelfBookCount = shelfBookCount,
                            isLockedPrivateShelf = isLockedPrivateShelf,
                            isReorderMode = false,
                            onOpenShelf = openShelfAction,
                            onDeleteShelf = { viewModel.deleteCustomShelf(shelf.id) }
                        )
                    }

                    ShelvesViewMode.HORIZONTAL_LIST -> {
                        ShelfHorizontalListCard(
                            shelf = shelf,
                            collageUrls = collageUrls,
                            shelfBookCount = shelfBookCount,
                            isLockedPrivateShelf = isLockedPrivateShelf,
                            isVaultUnlocked = uiState.isVaultUnlocked,
                            isReorderMode = false,
                            onOpenShelf = openShelfAction,
                            onDeleteShelf = { viewModel.deleteCustomShelf(shelf.id) }
                        )
                    }

                    ShelvesViewMode.BOOKSHELF_ROW -> {
                        ShelfShowcasePlankCard(
                            shelf = shelf,
                            shelfBooks = shelfBooks,
                            collageUrls = collageUrls,
                            shelfBookCount = shelfBookCount,
                            isLockedPrivateShelf = isLockedPrivateShelf,
                            isVaultUnlocked = uiState.isVaultUnlocked,
                            onOpenShelf = openShelfAction,
                            onOpenBook = { bookId -> viewModel.openBookDetail(bookId) }
                        )
                    }
                }
            }
        }
    }

    // Shelf Order & Delete Dialog
    if (showShelfOrderDialog) {
        AlertDialog(
            onDismissRequest = { showShelfOrderDialog = false },
            title = { Text("排序与删除", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                if (shelves.isEmpty()) {
                    Text("暂无书架", style = MaterialTheme.typography.bodyMedium)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        itemsIndexed(shelves) { index, shelf ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = shelf.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.moveShelfUp(shelf) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveShelfDown(shelf) },
                                    enabled = index < shelves.lastIndex,
                                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCustomShelf(shelf.id) },
                                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShelfOrderDialog = false }) { Text("完成", maxLines = 1, softWrap = false) }
            }
        )
    }

    // Create Shelf Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建书架", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newShelfName,
                        onValueChange = { newShelfName = it },
                        label = { Text("书架名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newShelfSubtitle,
                        onValueChange = { newShelfSubtitle = it },
                        label = { Text("备注") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        onClick = { newShelfPrivate = !newShelfPrivate },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "锁定书架入口（仅隐藏，不改动文件）",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Switch(
                                checked = newShelfPrivate,
                                onCheckedChange = { newShelfPrivate = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newShelfName.isNotBlank()) {
                            viewModel.createCustomShelf(
                                name = newShelfName.trim(),
                                subtitle = newShelfSubtitle.ifBlank { "收藏" },
                                isProtected = newShelfPrivate
                            )
                            newShelfName = ""
                            newShelfSubtitle = ""
                            newShelfPrivate = false
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("创建", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}

@Composable
private fun QuickCategoryTile(
    tile: QuickTileData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .height(58.dp)
            .testTag("quick_tile_${tile.key}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = tile.countText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = RobotoMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ShelfCollageGridCard(
    shelf: ShelfEntity,
    collageUrls: List<String>,
    shelfBookCount: Int,
    isLockedPrivateShelf: Boolean,
    isVaultUnlocked: Boolean,
    isReorderMode: Boolean,
    onOpenShelf: () -> Unit,
    onDeleteShelf: () -> Unit
) {
    Surface(
        onClick = onOpenShelf,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (shelf.isProtected) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shelf_card_${shelf.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (isLockedPrivateShelf) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "受保护",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "受保护",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (row in 0..1) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0..1) {
                                    val idx = row * 2 + col
                                    val url = collageUrls.getOrNull(idx).orEmpty()
                                    AsterBookCover(
                                        coverUrl = url,
                                        title = shelf.name,
                                        format = "CBZ",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(MaterialTheme.shapes.small)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isReorderMode && !shelf.isProtected) {
                    IconButton(
                        onClick = onDeleteShelf,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = shelf.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shelf.isProtected) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (isLockedPrivateShelf) "访问锁" else "$shelfBookCount 本",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfCompactGridCard(
    shelf: ShelfEntity,
    collageUrls: List<String>,
    shelfBookCount: Int,
    isLockedPrivateShelf: Boolean,
    isReorderMode: Boolean,
    onOpenShelf: () -> Unit,
    onDeleteShelf: () -> Unit
) {
    Surface(
        onClick = onOpenShelf,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shelf_card_${shelf.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (isLockedPrivateShelf) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "受保护",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    AsterBookCover(
                        coverUrl = collageUrls.firstOrNull().orEmpty(),
                        title = shelf.name,
                        format = "CBZ",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isLockedPrivateShelf) "访问锁" else "${shelfBookCount}本",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isReorderMode && !shelf.isProtected) {
                    IconButton(
                        onClick = onDeleteShelf,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = shelf.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShelfHorizontalListCard(
    shelf: ShelfEntity,
    collageUrls: List<String>,
    shelfBookCount: Int,
    isLockedPrivateShelf: Boolean,
    isVaultUnlocked: Boolean,
    isReorderMode: Boolean,
    onOpenShelf: () -> Unit,
    onDeleteShelf: () -> Unit
) {
    Surface(
        onClick = onOpenShelf,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shelf_card_${shelf.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isLockedPrivateShelf) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "受保护",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    collageUrls.take(3).forEach { url ->
                        AsterBookCover(
                            coverUrl = url,
                            title = shelf.name,
                            format = "CBZ",
                            modifier = Modifier
                                .width(44.dp)
                                .aspectRatio(0.71f)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = shelf.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shelf.isProtected) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = if (isLockedPrivateShelf) "入口已锁定，验证身份后可见" else "${shelf.subtitle} · 共 $shelfBookCount 本",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isReorderMode && !shelf.isProtected) {
                IconButton(onClick = onDeleteShelf) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShelfShowcasePlankCard(
    shelf: ShelfEntity,
    shelfBooks: List<BookEntity>,
    collageUrls: List<String>,
    shelfBookCount: Int,
    isLockedPrivateShelf: Boolean,
    isVaultUnlocked: Boolean,
    onOpenShelf: () -> Unit,
    onOpenBook: (String) -> Unit
) {
    Surface(
        onClick = onOpenShelf,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shelf_card_${shelf.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text(
                        text = shelf.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (shelf.isProtected) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (isLockedPrivateShelf) "访问锁" else "$shelfBookCount 本 · 展开",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (isLockedPrivateShelf) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "验证系统锁屏凭据以解锁书架",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        if (shelfBooks.isNotEmpty()) {
                            items(shelfBooks, key = { it.id }) { book ->
                                Column(
                                    modifier = Modifier
                                        .width(82.dp)
                                        .clickable { onOpenBook(book.id) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsterBookCover(
                                        coverUrl = book.coverUrl,
                                        title = book.title,
                                        format = book.format,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.71f)
                                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    )
                                }
                            }
                        } else {
                            items(collageUrls) { url ->
                                AsterBookCover(
                                    coverUrl = url,
                                    title = shelf.name,
                                    format = "CBZ",
                                    modifier = Modifier
                                        .width(82.dp)
                                        .aspectRatio(0.71f)
                                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                )
                            }
                        }
                    }
                    // Shelf Plank Ledge
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                }
            }
        }
    }
}

private data class QuickTileData(
    val key: String,
    val title: String,
    val countText: String,
    val icon: ImageVector
)

/**
 * Shelf Detail Sub-Screen (with interactive layout switching)
 */
@Composable
fun ShelfDetailScreen(
    shelfDetail: SubScreen.ShelfDetail,
    allBooks: List<BookEntity>,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val booksInShelf = remember(allBooks, shelfDetail) {
        allBooks.filter { shelfDetail.shelfId in it.shelfIdsCsv.split(',').map(String::trim) }
    }

    val gridColumns = when (uiState.libraryLayoutMode) {
        LibraryLayoutMode.GRID_2 -> if (isExpandedTablet) 3 else 2
        LibraryLayoutMode.GRID_3 -> if (isExpandedTablet) 4 else 3
        LibraryLayoutMode.LIST, LibraryLayoutMode.GALLERY -> if (isExpandedTablet) 2 else 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        AsterSubScreenHeader("${shelfDetail.title} (${booksInShelf.size})", onBack,
            trailing = {
                IconButton(onClick = { viewModel.cycleLibraryLayoutMode() },
                    modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)) {
                    Icon(Icons.Outlined.GridView, contentDescription = "切换视图")
                }
            })
        if (booksInShelf.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TranquilDeskEmptyIllustration(modifier = Modifier.size(140.dp))
                    Text("暂无书籍", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(booksInShelf, key = { it.id }) { book ->
                    if (book.isVaultProtected && !uiState.isVaultUnlocked) {
                        SafeProtectedBookCard(
                            onClick = {
                                viewModel.requestVaultUnlock {
                                    viewModel.openBookDetail(book.id)
                                }
                            }
                        )
                    } else if (uiState.libraryLayoutMode == LibraryLayoutMode.LIST) {
                        Surface(
                            onClick = { viewModel.openBookDetail(book.id) },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AsterBookCover(
                                    coverUrl = book.coverUrl,
                                    title = book.title,
                                    format = book.format,
                                    modifier = Modifier
                                        .width(56.dp)
                                        .aspectRatio(0.71f)
                                        .clip(MaterialTheme.shapes.small)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(book.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    LinearProgressIndicator(
                                        progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(4.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.openBookReader(
                                            bookId = book.id,
                                            preferDualPageOnTablet = isExpandedTablet
                                        )
                                    },
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("阅读", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    } else {
                        val isCompact3Col = uiState.libraryLayoutMode == LibraryLayoutMode.GRID_3
                        Surface(
                            onClick = { viewModel.openBookDetail(book.id) },
                            shape = RoundedCornerShape(if (isCompact3Col) 14.dp else 18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(if (isCompact3Col) 8.dp else 12.dp)) {
                                AsterBookCover(
                                    coverUrl = book.coverUrl,
                                    title = book.title,
                                    format = book.format,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.71f)
                                        .clip(RoundedCornerShape(if (isCompact3Col) 10.dp else 12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = book.title,
                                    style = if (isCompact3Col) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isCompact3Col) {
                                    Text(
                                        text = book.author,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.openBookReader(
                                                bookId = book.id,
                                                preferDualPageOnTablet = isExpandedTablet
                                            )
                                        },
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "阅读 ${book.progressPercent}%",
                                            style = MaterialTheme.typography.labelMedium,
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
    }
}
