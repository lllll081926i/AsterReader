package com.example.ui.screens
import com.asterreader.core.designsystem.AsterHeaderRow
import com.asterreader.core.designsystem.AsterHeaderTitleStyle
import com.asterreader.core.designsystem.asterHeaderAction

import androidx.compose.foundation.layout.heightIn

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.asterreader.core.designsystem.AsterModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.ReaderLayoutMode
import com.example.ui.components.AsterBookCover
import com.example.ui.theme.AnnotationBlue
import com.example.ui.theme.AnnotationGreen
import com.example.ui.theme.AnnotationPink
import com.example.ui.theme.AnnotationYellow
import com.example.ui.theme.LocalAsterColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicationDetailScreen(
    book: BookEntity,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel,
    onBack: () -> Unit,
    onStartReading: (Int?) -> Unit,
    section: String? = null
) {
    if (section != null) {
        PublicationPropertiesScreen(book, section, viewModel, onBack)
        return
    }
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    val chapters by remember(book.id) {
        viewModel.observeChapters(book.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val annotations by remember(book.id) {
        viewModel.observeBookAnnotations(book.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var isSynopsisExpanded by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showFullTocSheet by remember { mutableStateOf(false) }
    var showAnnotationsSheet by remember { mutableStateOf(false) }
    val openSection: (String) -> Unit = { viewModel.pushSubScreen(com.example.ui.SubScreen.PublicationDetail(book.id, it)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AsterHeaderRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp)
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = com.asterreader.core.designsystem.AsterLayout.HeaderHeight)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.asterHeaderAction().testTag("detail_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.HeaderIconSize)
                    )
                }

                Text(
                    text = book.title,
                    style = AsterHeaderTitleStyle,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(book.id) },
                        modifier = Modifier.asterHeaderAction().testTag("detail_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "收藏",
                            modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.HeaderIconSize),
                            tint = if (book.isFavorite) colorScheme.primary else colorScheme.outline
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.asterHeaderAction().testTag("detail_more_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "更多操作",
                                modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.HeaderIconSize),
                                tint = colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                text = { Text("编辑信息", maxLines = 1, softWrap = false) },
                                onClick = {
                                    showOverflowMenu = false
                                    openSection("metadata")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                                text = { Text("文件属性", maxLines = 1, softWrap = false) },
                                onClick = {
                                    showOverflowMenu = false
                                    openSection("file")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Photo, contentDescription = null) },
                                text = { Text("选择封面页", maxLines = 1, softWrap = false) },
                                onClick = {
                                    showOverflowMenu = false
                                    openSection("cover")
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, contentDescription = null) },
                                text = { Text("系列与卷序", maxLines = 1, softWrap = false) },
                                onClick = {
                                    showOverflowMenu = false
                                    openSection("series")
                                }
                            )
                            if (book.sourceStatus == "MISSING") {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Outlined.FindInPage, contentDescription = null) },
                                    text = { Text("重新定位来源", maxLines = 1, softWrap = false) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.beginRelocate(book.id)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.EnhancedEncryption, null) },
                                text = { Text(if (book.isVaultEncrypted) "加密导出" else "加密文件") },
                                onClick = {
                                    showOverflowMenu = false
                                    if (book.isVaultEncrypted) viewModel.pushSubScreen(com.example.ui.SubScreen.SettingsSection("fileExport:${book.id}"))
                                    else viewModel.beginEncryptBook(book.id)
                                }
                            )
                            if (!book.isVaultEncrypted) DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (book.isVaultProtected) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (book.isVaultProtected) "取消隐藏" else "隐藏作品",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.setBookHidden(book.id, !book.isVaultProtected)
                                }
                            )
                        }
                    }
                }
            }

            // Main Scrollable Canvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isExpandedTablet) 32.dp else 16.dp)
                    .padding(bottom = com.asterreader.core.designsystem.AsterLayout.BottomSpacing),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 880.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 来源失联提示：保留阅读记录，可重新定位
                    if (book.sourceStatus == "MISSING") {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "源文件已移动或授权失效，阅读进度已保留",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.beginRelocate(book.id) }) {
                                    Text("重新定位", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    // Hero Section (No comic type badges)
                    if (isExpandedTablet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            DetailCoverCard(
                                book = book,
                                modifier = Modifier.width(190.dp)
                            )
                            DetailMetadataColumn(
                                book = book,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailCoverCard(
                                book = book,
                                modifier = Modifier.width(144.dp)
                            )
                            DetailMetadataColumn(
                                book = book,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Synopsis Card (没有简介就不显示对应的位置块)
                    if (book.synopsis.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = asterColors.surfaceContainerLow,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = com.asterreader.core.designsystem.AsterMotion.effectsSpec(com.asterreader.core.designsystem.LocalAsterReduceMotion.current))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Subject,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "简介",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = book.synopsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { isSynopsisExpanded = !isSynopsisExpanded },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("synopsis_toggle_btn")
                                    ) {
                                        Text(
                                            text = if (isSynopsisExpanded) "收起" else "展开",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                            color = colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Icon(
                                            imageVector = if (isSynopsisExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Table of Contents Preview Card (目录若为空则单章节显示当前书名)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = asterColors.surfaceContainerLow,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Toc,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "目录",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(asterColors.surfaceContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (chapters.isNotEmpty()) "共 ${chapters.size} 话" else "全本",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                if (chapters.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { showFullTocSheet = true }
                                            .heightIn(min = com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("open_full_toc_btn"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "全部",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (chapters.isNotEmpty()) {
                                    chapters.take(6).forEach { ch ->
                                        ChapterPreviewChip(
                                            chapter = ch,
                                            onClick = { onStartReading(ch.startPage) }
                                        )
                                    }
                                } else {
                                    // 没有目录时只显示当前书名，算一个章节
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = asterColors.surfaceContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable { onStartReading(1) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "共 ${book.totalPages} 页",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.primary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bookmarks Summary Card (没有书签就不显示对应的位置块)
                    if (annotations.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = asterColors.surfaceContainerLow,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bookmark,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "书签",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                    TextButton(onClick = { showAnnotationsSheet = true }) {
                                        Text("管理 (${annotations.size})", maxLines = 1, softWrap = false)
                                    }
                                }

                                annotations.take(2).forEach { item ->
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = asterColors.surfaceContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.medium)
                                            .clickable { onStartReading(item.pageNumber) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "${item.chapterTitle} · P.${item.pageNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.createdAtText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.outline,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                            Text(
                                                text = "“${item.quoteText}”",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurface,
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
            }
        }

        // ====================================================================
        // Unified Bottom Dock: Reading Progress Bar + Action Options Merged
        // ====================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp, top = 4.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = asterColors.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .testTag("detail_floating_action_island")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row inside Dock: Merged Reading Progress Info & LinearProgressIndicator
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    text = "第 ${book.currentPage} / ${book.totalPages} 页",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            val remaining = (book.totalPages - book.currentPage).coerceAtLeast(0)
                            Text(
                                text = "已读 ${book.progressPercent}% · 剩 $remaining 页",
                                style = MaterialTheme.typography.labelSmall,
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
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }

                    // Bottom Row inside Dock: 高亮强化的 [▶ 继续阅读 / 开始阅读] 主按钮 + [书签]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary,
                            contentColor = colorScheme.onPrimary,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = com.asterreader.core.designsystem.AsterLayout.TouchTarget)
                                .clip(CircleShape)
                                .clickable { onStartReading(book.currentPage) }
                                .testTag("detail_start_reading_cta")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (book.progressPercent > 0) "继续阅读" else "开始阅读",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        DetailIslandSecondaryButton(
                            icon = Icons.Outlined.BookmarkBorder,
                            label = "书签",
                            onClick = { showAnnotationsSheet = true }
                        )
                    }
                }
            }
        }
    }

    // Full Table of Contents Bottom Sheet
    if (showFullTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFullTocSheet = false },
            containerColor = asterColors.surfaceBright
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "章节目录",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${book.title} · 共 ${chapters.size} 话",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { showFullTocSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(chapters, key = { it.id }) { ch ->
                        ChapterPreviewChip(
                            chapter = ch,
                            onClick = {
                                showFullTocSheet = false
                                onStartReading(ch.startPage)
                            }
                        )
                    }
                }
            }
        }
    }

    // Bookmarks, Highlights & Notes Bottom Sheet
    if (showAnnotationsSheet) {
        var newNoteInput by remember { mutableStateOf("") }
        ModalBottomSheet(
            onDismissRequest = { showAnnotationsSheet = false },
            containerColor = asterColors.surfaceBright
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "书签与札记",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Button(
                        onClick = {
                            viewModel.addAnnotation(
                                bookId = book.id,
                                chapterTitle = book.currentChapterTitle,
                                pageNumber = book.currentPage,
                                quoteText = "第 ${book.currentPage} 页定位点",
                                noteText = newNoteInput.ifBlank { "书签标记" },
                                colorName = "GREEN",
                                type = if (newNoteInput.isBlank()) "BOOKMARK" else "NOTE"
                            )
                            newNoteInput = ""
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("添加书签", maxLines = 1, softWrap = false)
                    }
                }

                OutlinedTextField(
                    value = newNoteInput,
                    onValueChange = { newNoteInput = it },
                    label = { Text("输入笔记内容") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(annotations, key = { it.id }) { item ->
                        val badgeTint = when (item.colorName) {
                            "YELLOW" -> AnnotationYellow
                            "BLUE" -> AnnotationBlue
                            "PINK" -> AnnotationPink
                            else -> AnnotationGreen
                        }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = asterColors.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    showAnnotationsSheet = false
                                    onStartReading(item.pageNumber)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(badgeTint)
                                        )
                                        Text(
                                            text = "${item.chapterTitle} · P.${item.pageNumber}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "“${item.quoteText}”",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurface
                                    )
                                    if (item.noteText.isNotBlank()) {
                                        Text(
                                            text = item.noteText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteAnnotation(item.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "删除",
                                        tint = colorScheme.outline
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

@Composable
private fun DetailCoverCard(
    book: BookEntity,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.medium)
            .background(asterColors.surfaceContainerHigh)
    ) {
        AsterBookCover(
            coverUrl = book.altCoverUrl.ifBlank { book.coverUrl },
            title = book.title,
            format = book.format,
            modifier = Modifier.fillMaxSize()
        )
        if (book.isVaultProtected) {
            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainerLowest.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "受保护",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailMetadataColumn(
    book: BookEntity,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (book.isVaultProtected) {
                Text(
                    text = if (book.isVaultEncrypted) "文件已加密" else "已隐藏 · 需验证身份",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colorScheme.onSurface
            )
            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderZip,
                        contentDescription = null,
                        tint = colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${book.fileSizeText} · ${book.totalPages} 页",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = book.updatedTimeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterPreviewChip(
    chapter: ChapterEntity,
    onClick: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (chapter.isCurrent) asterColors.primaryFixed.copy(alpha = 0.35f) else asterColors.surfaceContainer,
        border = if (chapter.isCurrent) BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                chapter.isCurrent -> colorScheme.primary
                                chapter.isCompleted -> colorScheme.secondaryContainer
                                else -> asterColors.surfaceDim
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chapter.chapterNumber.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            chapter.isCurrent -> colorScheme.onPrimary
                            chapter.isCompleted -> colorScheme.onSecondaryContainer
                            else -> colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (chapter.isCurrent) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = if (chapter.isCurrent) colorScheme.primary else colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when {
                chapter.isCurrent -> {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "在读",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                chapter.isCompleted -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "已读",
                        tint = colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                else -> {
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
    }
}

@Composable
private fun DetailIslandSecondaryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .heightIn(min = com.asterreader.core.designsystem.AsterLayout.TouchTarget)
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}
