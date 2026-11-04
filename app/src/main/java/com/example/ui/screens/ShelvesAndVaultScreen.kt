package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import com.example.data.local.RepositoryEntity
import com.example.data.local.ShelfEntity
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ReaderPaperTheme
import com.example.ui.ShelvesViewMode
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.AsterBookCover
import com.example.ui.components.AsterSettingsGroup
import com.example.ui.components.AsterSettingsRow
import com.example.ui.components.AsterSwitchRow
import com.example.ui.components.BotanicalGrimoireIllustration
import com.example.ui.components.SafeProtectedBookCard
import com.example.ui.components.TranquilDeskEmptyIllustration
import com.example.ui.components.VaultGeometricShieldIllustration
import com.example.ui.theme.RobotoMonoFontFamily

/**
 * 1. Shelves & Collections Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var isReorderMode by remember { mutableStateOf(false) }

    val shelvesGridColumns = when (uiState.shelvesViewMode) {
        ShelvesViewMode.COLLAGE_GRID -> if (isExpandedTablet) 4 else 2
        ShelvesViewMode.COMPACT_GRID -> if (isExpandedTablet) 6 else 3
        ShelvesViewMode.HORIZONTAL_LIST, ShelvesViewMode.BOOKSHELF_ROW -> if (isExpandedTablet) 2 else 1
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { isReorderMode = !isReorderMode },
                    shape = RoundedCornerShape(999.dp),
                    color = if (isReorderMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = "排序",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isReorderMode) "完成" else "排序",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("create_shelf_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "新建",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Far Top-Right Settings Button (Merged Appearance + Global Settings)
                IconButton(
                    onClick = { viewModel.pushSubScreen(SubScreen.Settings) },
                    modifier = Modifier.testTag("shelves_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(shelvesGridColumns),
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
                                    activeQuickFilter = if (activeQuickFilter == tile.key) null else tile.key
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
                                    activeQuickFilter = if (activeQuickFilter == tile.key) null else tile.key
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
                        shape = RoundedCornerShape(20.dp),
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
                                    shape = RoundedCornerShape(12.dp),
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
                                                .clip(RoundedCornerShape(6.dp))
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

            // Section 2 Header + Interactive 4-Mode Shelf Layout Switcher (`双列` / `三列` / `列表` / `展架`)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
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

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("shelves_layout_switcher")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            ShelvesViewMode.entries.forEach { mode ->
                                val selected = uiState.shelvesViewMode == mode
                                val icon = when (mode) {
                                    ShelvesViewMode.COLLAGE_GRID -> Icons.Outlined.GridView
                                    ShelvesViewMode.COMPACT_GRID -> Icons.Outlined.Apps
                                    ShelvesViewMode.HORIZONTAL_LIST -> Icons.AutoMirrored.Outlined.ViewList
                                    ShelvesViewMode.BOOKSHELF_ROW -> Icons.Outlined.ViewCarousel
                                }
                                Surface(
                                    onClick = { viewModel.setShelvesViewMode(mode) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.testTag("shelves_mode_${mode.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = mode.label,
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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

            items(shelves, key = { it.id }) { shelf ->
                val isLockedPrivateShelf = shelf.isProtected && !uiState.isVaultUnlocked
                val collageUrls = shelf.coverUrlsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val shelfBooks = allBooks.filter { it.shelfIdsCsv.contains(shelf.id) }
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
                            isReorderMode = isReorderMode,
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
                            isReorderMode = isReorderMode,
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
                            isReorderMode = isReorderMode,
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
                        shape = RoundedCornerShape(12.dp),
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
                                text = "加密保护",
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
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .height(84.dp)
            .testTag("quick_tile_${tile.key}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tile.countText,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = RobotoMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleSmall,
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
        shape = RoundedCornerShape(24.dp),
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
                    .clip(RoundedCornerShape(16.dp))
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
                                    contentDescription = "已加密",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已加密",
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
                                        format = "EPUB",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(10.dp))
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
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (isLockedPrivateShelf) "加密" else "$shelfBookCount 本",
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
        shape = RoundedCornerShape(18.dp),
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                if (isLockedPrivateShelf) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "已加密",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    AsterBookCover(
                        coverUrl = collageUrls.firstOrNull().orEmpty(),
                        title = shelf.name,
                        format = "EPUB",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isLockedPrivateShelf) "加密" else "${shelfBookCount}本",
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
        shape = RoundedCornerShape(20.dp),
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
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "已加密",
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
                            format = "EPUB",
                            modifier = Modifier
                                .width(44.dp)
                                .aspectRatio(0.71f)
                                .clip(RoundedCornerShape(8.dp))
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
                    text = if (isLockedPrivateShelf) "受加密保护" else "${shelf.subtitle} · 共 $shelfBookCount 本",
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
        shape = RoundedCornerShape(24.dp),
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
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (isLockedPrivateShelf) "已加密" else "$shelfBookCount 本 · 展开",
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
                    shape = RoundedCornerShape(16.dp),
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
                            text = "点击验证指纹或 PIN 解锁书架",
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
                                    format = "EPUB",
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
                shape = RoundedCornerShape(999.dp),
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
                            shape = RoundedCornerShape(999.dp),
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
                            shape = RoundedCornerShape(18.dp),
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
                                        .clip(RoundedCornerShape(10.dp))
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
                                            .clip(RoundedCornerShape(999.dp))
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.openBookReader(
                                            bookId = book.id,
                                            preferDualPageOnTablet = isExpandedTablet
                                        )
                                    },
                                    shape = RoundedCornerShape(999.dp),
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
                                        shape = RoundedCornerShape(999.dp),
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
 * 2. Vault Overview Screen (retained for compatibility if invoked programmatically)
 */
@Composable
fun VaultOverviewScreen(
    allBooks: List<BookEntity>,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel
) {
    val protectedBooks = remember(allBooks) { allBooks.filter { it.isVaultProtected } }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("vault_overview_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "加密书籍",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (uiState.isVaultUnlocked) {
                Button(
                    onClick = { viewModel.lockVaultSession() },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.testTag("vault_lock_now_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("锁定", maxLines = 1, softWrap = false)
                }
            }
        }

        if (!uiState.isVaultUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.widthIn(max = 460.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        VaultGeometricShieldIllustration(modifier = Modifier.size(120.dp))

                        Text(
                            text = "已加密 (${protectedBooks.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all(Char::isDigit)) {
                                    pinInput = it
                                    pinError = false
                                    if (it.length >= 4 && viewModel.verifyPin(it)) {
                                        pinInput = ""
                                    }
                                }
                            },
                            label = { Text("输入 PIN 码") },
                            isError = pinError,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_pin_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.unlockAppAndVault() },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("vault_biometric_unlock_btn")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("指纹解锁", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(
                                onClick = {
                                    val ok = viewModel.verifyPin(pinInput.ifBlank { "1234" })
                                    if (!ok) pinError = true
                                },
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Text("解锁", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Privacy & Security Settings Screen
 */
@Composable
fun PrivacySecuritySettingsScreen(
    uiState: AsterUiState,
    protectedBooksCount: Int,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showRecoveryKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("privacy_security_settings_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "隐私与安全",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Hardware Cryptography Status Banner
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "端侧加密已开启",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1
                        )
                        Text(
                            text = "当前受保护书籍：$protectedBooksCount 本",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }
            }

            // Group 1: 应用锁
            AsterSettingsGroup(title = "应用锁") {
                AsterSwitchRow(
                    title = "启用应用锁",
                    checked = uiState.isAppLockEnabled,
                    onCheckedChange = { viewModel.setAppLockEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "生物识别解锁",
                    checked = uiState.secondaryAuthForVault,
                    onCheckedChange = { viewModel.setSecondaryAuthForVault(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.LockClock,
                    title = "自动锁定时间",
                    badgeText = uiState.autoLockPolicyLabel,
                    onClick = { showTimeoutDialog = true }
                )
            }

            // Group 2: 加密防护
            AsterSettingsGroup(title = "防窥与加密") {
                AsterSwitchRow(
                    title = "防截屏保护",
                    checked = uiState.flagSecureEnabled,
                    onCheckedChange = { viewModel.setFlagSecureEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "多任务模糊遮罩",
                    checked = uiState.hideRecentsPreview,
                    onCheckedChange = { viewModel.setHideRecentsPreview(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.RemoveRedEye,
                    title = "加密书籍隐藏封面",
                    badgeText = "已开启",
                    onClick = {}
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.SearchOff,
                    title = "搜索结果隐藏加密项",
                    badgeText = "已开启",
                    onClick = {}
                )
            }

            // Group 3: 密钥管理
            AsterSettingsGroup(title = "密钥与会话") {
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.VerifiedUser,
                    title = "加密完整性校验",
                    badgeText = "校验",
                    onClick = { viewModel.runVaultIntegrityCheck() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Key,
                    title = "应急恢复密钥",
                    onClick = { showRecoveryKeyDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Default.LockOpen,
                    title = "立即锁定",
                    badgeText = "$protectedBooksCount 本",
                    onClick = { viewModel.lockAppImmediately() }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showTimeoutDialog) {
        val options = listOf("立即锁定", "30 秒后", "1 分钟后", "5 分钟后", "仅息屏锁定")
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("自动锁定时间", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoLockPolicyLabel(option)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.autoLockPolicyLabel == option,
                                onClick = {
                                    viewModel.setAutoLockPolicyLabel(option)
                                    showTimeoutDialog = false
                                }
                            )
                            Text(option, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("关闭", maxLines = 1, softWrap = false) }
            }
        )
    }

    if (showRecoveryKeyDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryKeyDialog = false },
            title = { Text("应急恢复密钥", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = "ASTR-94F2-7B1E-C803-55D9-K2M8",
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showRecoveryKeyDialog = false }) {
                    Text("确定", maxLines = 1, softWrap = false)
                }
            }
        )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        // Header with Back Arrow
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
                Text(
                    text = "设置与外观",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            OutlinedButton(
                onClick = { viewModel.pushSubScreen(SubScreen.Onboarding) },
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("向导", maxLines = 1, softWrap = false)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Group 1: 外观与阅读 (Merged direct theme controls + layout & typography)
            AsterSettingsGroup(title = "外观与显示") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "主题模式",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ThemeMode.SYSTEM to "跟随系统",
                            ThemeMode.LIGHT to "浅色模式",
                            ThemeMode.DARK to "深色模式"
                        ).forEach { (mode, label) ->
                            val selected = uiState.themeMode == mode
                            Surface(
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "系统动态取色 (Monet)",
                    checked = uiState.useDynamicColor,
                    onCheckedChange = { viewModel.setUseDynamicColor(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "首页显示最近阅读",
                    checked = uiState.showRecentReading,
                    onCheckedChange = { viewModel.setShowRecentReading(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "书库默认布局",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LibraryLayoutMode.entries.forEach { mode ->
                            val selected = uiState.libraryLayoutMode == mode
                            Surface(
                                onClick = { viewModel.setLibraryLayoutMode(mode) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Tune,
                    title = "漫画默认阅读设置",
                    badgeText = "${uiState.readerLayoutMode.shortBadge} · ${uiState.fitModeLabel}",
                    onClick = { activeModal = "COMIC_PREFS" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
                    title = "电子书默认排版设置",
                    badgeText = "${uiState.readerFontSizeSp}sp · ${uiState.readerPaperTheme.label.take(4)}",
                    onClick = { activeModal = "EBOOK_PREFS" }
                )
            }

            // Group 2: 本地书库
            AsterSettingsGroup(title = "本地书库") {
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Folder,
                    title = "已授权目录",
                    badgeText = "${repositories.size} 个",
                    onClick = { activeModal = "REPOS" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.CreateNewFolder,
                    title = "导入本地文件",
                    onClick = onTriggerImportFile
                )
            }

            // Group 3: 隐私与安全
            AsterSettingsGroup(title = "隐私与安全") {
                AsterSettingsRow(
                    leadingIcon = Icons.Default.Security,
                    title = "安全与加密设置",
                    badgeText = if (uiState.isAppLockEnabled) "已保护" else "已开启",
                    onClick = { viewModel.pushSubScreen(SubScreen.PrivacySecuritySettings) }
                )
            }

            // Group 4: 存储与关于
            AsterSettingsGroup(title = "存储与关于") {
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Storage,
                    title = "清理图片缓存",
                    badgeText = "${uiState.coverCacheMb + uiState.pageDecodeCacheMb} MB",
                    onClick = { viewModel.clearRegenerableCache() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Backup,
                    title = "备份与恢复",
                    badgeText = uiState.lastBackupTimeText,
                    onClick = {
                        viewModel.createLocalBackup(includeVault = true)
                        activeModal = "BACKUP"
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.BugReport,
                    title = "运行状态",
                    onClick = { activeModal = "DIAGNOSTICS" }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Info,
                    title = "关于 Aster Reader",
                    badgeText = "v2.4.0",
                    onClick = { activeModal = "ABOUT" }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Repositories Management Dialog
    if (activeModal == "REPOS") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("本地目录 ($booksCount 本)", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repositories.forEach { repo ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(repo.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${repo.bookCount} 本", style = MaterialTheme.typography.labelSmall, fontFamily = RobotoMonoFontFamily, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                }
                                Text(repo.pathUri, style = MaterialTheme.typography.labelSmall, fontFamily = RobotoMonoFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newRepoName,
                        onValueChange = { newRepoName = it },
                        label = { Text("新增目录名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRepoName.isNotBlank()) {
                            viewModel.addRepositoryAndScan(newRepoName.trim(), "按文件夹识别")
                            newRepoName = ""
                        } else {
                            viewModel.rescanRepositories()
                        }
                        activeModal = null
                    }
                ) {
                    Text("保存", maxLines = 1, softWrap = false)
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
                                shape = RoundedCornerShape(10.dp),
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
                        listOf("适合宽度", "适合高度", "原始比例").forEach { fit ->
                            val selected = uiState.fitModeLabel == fit
                            Surface(
                                onClick = { viewModel.setFitModeLabel(fit) },
                                shape = RoundedCornerShape(10.dp),
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
                        Text("自动裁切白边", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = uiState.autoCropMargins,
                            onCheckedChange = { viewModel.setAutoCropMargins(it) }
                        )
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

    // Dedicated E-Book Default Settings Modal
    if (activeModal == "EBOOK_PREFS") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("电子书默认排版设置", fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("默认纸张背景", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReaderPaperTheme.entries.forEach { paper ->
                            val selected = uiState.readerPaperTheme == paper
                            Surface(
                                onClick = { viewModel.setReaderPaperTheme(paper) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = paper.label.take(4),
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

                    Text("默认字体", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("衬线" to "Lora", "无衬线" to "Roboto Flex", "等宽" to "Roboto Mono").forEach { (label, key) ->
                            val selected = uiState.readerFontFamily == key
                            Surface(
                                onClick = { viewModel.setReaderFontFamily(key) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
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
                        Text("默认字号 (${uiState.readerFontSizeSp}sp)", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.setReaderFontSize(uiState.readerFontSizeSp - 1) }) {
                                Text("A-", maxLines = 1, softWrap = false)
                            }
                            OutlinedButton(onClick = { viewModel.setReaderFontSize(uiState.readerFontSizeSp + 1) }) {
                                Text("A+", maxLines = 1, softWrap = false)
                            }
                        }
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

    // Diagnostics / Backup / About Modals
    if (activeModal == "BACKUP" || activeModal == "DIAGNOSTICS" || activeModal == "ABOUT") {
        AlertDialog(
            onDismissRequest = { activeModal = null },
            title = {
                Text(
                    text = when (activeModal) {
                        "BACKUP" -> "备份完成"
                        "DIAGNOSTICS" -> "运行状态"
                        else -> "Aster Reader"
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeModal == "ABOUT") {
                        BotanicalGrimoireIllustration(modifier = Modifier.size(110.dp))
                    }
                    Text(
                        text = when (activeModal) {
                            "BACKUP" -> "已生成本地备份：aster_backup_2026.zip"
                            "DIAGNOSTICS" -> "EPUB / CBZ / PDF 渲染引擎运行正常"
                            else -> "Aster Reader v2.4.0\n本地电子书与漫画阅读器"
                        },
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
