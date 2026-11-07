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
                .height(56.dp)
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
                // 统一 36dp 尺寸的排序按钮
                Surface(
                    onClick = { showShelfOrderDialog = true },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp)
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

                // 统一 36dp 尺寸的新建书架按钮
                Surface(
                    onClick = { showCreateDialog = true },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
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

                // 统一 36dp 尺寸的设置按钮
                IconButton(
                    onClick = { viewModel.pushSubScreen(SubScreen.Settings) },
                    modifier = Modifier
                        .size(36.dp)
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
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
                        .height(44.dp)
                        .padding(top = 8.dp, bottom = 2.dp),
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
                            .size(36.dp)
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
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.moveShelfDown(shelf) },
                                    enabled = index < shelves.lastIndex,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteCustomShelf(shelf.id) },
                                    modifier = Modifier.size(32.dp)
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
                            .size(32.dp)
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
                            .size(26.dp)
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
        allBooks.filter { it.shelfIdsCsv.contains(shelfDetail.shelfId) }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "${shelfDetail.title} (${booksInShelf.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Layout Switcher inside Shelf Detail
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LibraryLayoutMode.entries.forEach { mode ->
                        val selected = uiState.libraryLayoutMode == mode
                        Surface(
                            onClick = { viewModel.setLibraryLayoutMode(mode) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

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
                contentPadding = PaddingValues(20.dp),
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

/**
 * 4. Global Settings Screen (Merged Appearance + Global Settings, accessible from Top-Right)
 */
@Composable
fun SettingsScreen(
    uiState: AsterUiState,
    repositories: List<RepositoryEntity>,
    booksCount: Int,
    viewModel: AsterViewModel,
    onTriggerImportFile: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }

    var activeModal by remember { mutableStateOf<String?>(null) }
    var newRepoName by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportLibrary(it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importLibrary(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        AsterSubScreenHeader(
            title = "设置",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Group 0: 外观
            AsterSettingsGroup(title = "外观") {
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.DarkMode,
                    title = "主题",
                    badgeText = uiState.themeMode.label,
                    onClick = { activeModal = "THEME" }
                )
            }

            // Group 1: 阅读与排版配置
            AsterSettingsGroup(title = "阅读与排版") {
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Tune,
                    title = "排版管理",
                    badgeText = "${uiState.readerLayoutMode.shortBadge} · ${uiState.fitModeLabel}",
                    onClick = { activeModal = "COMIC_PREFS" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "减弱动效",
                    checked = uiState.reduceMotion,
                    onCheckedChange = { viewModel.setReduceMotion(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "音量键翻页",
                    checked = uiState.volumeKeyPageTurn,
                    onCheckedChange = { viewModel.setVolumeKeyPageTurn(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "跨页对齐拼接",
                    checked = uiState.smartSpreadStitch,
                    onCheckedChange = { viewModel.setSmartSpreadStitch(it) }
                )
            }

            // Group 2: 书架与标签管理
            AsterSettingsGroup(title = "书架与标签") {
                AsterSettingsRow(
                    leadingIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
                    title = "书架管理",
                    badgeText = "管理分组",
                    onClick = { activeModal = "SHELF_MGR" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Tag,
                    title = "标签管理",
                    badgeText = "作品分类",
                    onClick = { activeModal = "TAG_MGR" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "首页继续阅读栏",
                    checked = uiState.showRecentReading,
                    onCheckedChange = { viewModel.setShowRecentReading(it) }
                )
            }

            // Group 3: 本地书库与存储
            AsterSettingsGroup(title = "阅读统计") {
                AsterSwitchRow(
                    title = "记录阅读时长",
                    checked = uiState.showReadingStats,
                    onCheckedChange = { viewModel.setShowReadingStats(it) }
                )
                if (uiState.showReadingStats) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    AsterSettingsRow(
                        leadingIcon = Icons.Outlined.Schedule,
                        title = "查看统计",
                        onClick = { viewModel.pushSubScreen(SubScreen.ReadingStats) }
                    )
                }
            }

            AsterSettingsGroup(title = "本地书库") {
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Folder,
                    title = "书库目录",
                    badgeText = "${repositories.size} 个",
                    onClick = { activeModal = "REPOS" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.CreateNewFolder,
                    title = "导入书籍",
                    onClick = onTriggerImportFile
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Storage,
                    title = "清理图片缓存",
                    badgeText = "释放空间",
                    onClick = { viewModel.clearRegenerableCache() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Image,
                    title = "重建封面",
                    subtitle = "按封面页重新截取，可修复显示不出来的封面",
                    onClick = { viewModel.rebuildLibraryCovers() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Backup,
                    title = "导出书库清单",
                    subtitle = "备份进度、标签、书架与批注（不含漫画文件）",
                    onClick = { exportLauncher.launch("aster-library.json") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Folder,
                    title = "恢复书库清单",
                    subtitle = "从备份恢复元数据，来源文件需重新授权",
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }

            // Group 4: 隐私与关于
            AsterSettingsGroup(title = "安全与关于") {
                AsterSettingsRow(
                    leadingIcon = Icons.Default.Security,
                    title = "安全设置",
                    badgeText = if (uiState.isAppLockEnabled) "已开启" else "未开启",
                    onClick = { viewModel.pushSubScreen(SubScreen.PrivacySecuritySettings) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Info,
                    title = "关于 Aster Reader",
                    badgeText = "v2.4.0",
                    onClick = { activeModal = "ABOUT" }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "本机共 $booksCount 部作品 · 数据只留在这台设备",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            )
        }
    }

    // Theme Picker
    if (activeModal == "THEME") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("主题", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    activeModal = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    activeModal = null
                                }
                            )
                            Text(mode.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeModal = null }) { Text("关闭", maxLines = 1, softWrap = false) }
            }
        )
    }

    // Repositories Management Dialog (在文件管理器中直接选择目录，绝无手动填写)
    if (activeModal == "REPOS") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("书库目录 (${repositories.size})", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (repositories.isEmpty()) {
                        Text(
                            text = "暂无已挂载的书库目录。点击下方按钮直接在系统文件管理器中选取。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        repositories.forEach { repo ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(repo.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(repo.pathUri, style = MaterialTheme.typography.labelSmall, fontFamily = RobotoMonoFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { viewModel.deleteRepository(repo.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "删除目录", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        activeModal = null
                        // 直接启动系统文件管理器选取目录
                        viewModel.addRepositoryAndScan("本地书库", com.example.data.local.LibraryRepository.DEFAULT_SCAN_RULE)
                    }
                ) {
                    Text("选择目录", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeModal = null }) { Text("关闭", maxLines = 1, softWrap = false) }
            }
        )
    }

    // Dedicated Comic Default Settings Modal
    if (activeModal == "COMIC_PREFS") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("漫画默认阅读设置", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("默认翻页方向", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReaderLayoutMode.entries.forEach { mode ->
                            val selected = uiState.readerLayoutMode == mode
                            Surface(
                                onClick = { viewModel.setReaderLayoutMode(mode) },
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mode.shortBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Text("画面缩放模式", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("适合屏幕", "原始比例").forEach { fit ->
                            val selected = uiState.fitModeLabel == fit
                            Surface(
                                onClick = { viewModel.setFitModeLabel(fit) },
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = fit,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("双页跨页智能拼接", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.smartSpreadStitch,
                            onCheckedChange = { viewModel.setSmartSpreadStitch(it) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeModal = null }) {
                    Text("完成", maxLines = 1, softWrap = false)
                }
            }
        )
    }


    // Shelf Manager Modal
    if (activeModal == "SHELF_MGR") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("书架管理", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "书架用于整理与聚合不同来源的漫画。可直接在首页或详情页快速入架。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            activeModal = null
                            viewModel.createCustomShelf("新书架", "自定义收藏", false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("新建自定义书架", maxLines = 1, softWrap = false)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("完成", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // Tag Manager Modal
    if (activeModal == "TAG_MGR") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("标签管理", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "标签来自作品详情的「编辑信息」，检索筛选会直接使用这些标签。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            activeModal = null
                            viewModel.openSearch(openFilterInitially = false, initialQuery = "", initialScope = "全部")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("前往标签检索", maxLines = 1, softWrap = false)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeModal = null }) {
                    Text("完成", maxLines = 1, softWrap = false)
                }
            }
        )
    }

    // About Modal
    if (activeModal == "ABOUT") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = {
                Text(
                    text = "Aster Reader",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BotanicalGrimoireIllustration(modifier = Modifier.size(110.dp))
                    Text(
                        text = "Aster Reader v2.4.0\n轻量、安全、纯粹的本地漫画阅读器",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(onClick = { activeModal = null }) {
                    Text("确定", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}
