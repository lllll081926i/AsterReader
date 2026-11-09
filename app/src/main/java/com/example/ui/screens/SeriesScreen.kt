package com.example.ui.screens

import com.example.ui.components.AsterSubScreenHeader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asterreader.core.database.BookEntity
import com.example.ui.SubScreen
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.asterreader.core.designsystem.asterCardInteraction
import com.example.ui.components.AsterBookCover
import com.example.ui.components.AsterEmptyState

/**
 * 系列视图：按 [BookEntity.series] 聚合，卷序取自 [BookEntity.seriesOrder]。
 * 没有系列信息的作品不在此列表出现，避免把散本混进来。
 */
@Composable
fun SeriesScreen(
    allBooks: List<BookEntity>,
    uiState: AsterUiState,
    isExpandedTablet: Boolean,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    val series = remember(allBooks, uiState.isVaultUnlocked) {
        allBooks
            .filter { it.series.isNotBlank() }
            .filter { !it.isVaultProtected || uiState.isVaultUnlocked }
            .groupBy { it.series.trim() }
            .mapValues { (_, volumes) -> volumes.sortedBy { it.seriesOrder } }
            .toList()
            .sortedByDescending { (_, volumes) -> volumes.maxOfOrNull { it.lastReadTimestamp } ?: 0L }
    }

    val looseCount = allBooks.count { it.series.isBlank() }
    var expanded by remember { mutableStateOf<String?>(series.firstOrNull()?.first) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AsterSubScreenHeader("系列", onBack)

        if (series.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsterEmptyState(
                    title = "还没有系列",
                    description = "书名带有卷号时会自动归入同一系列。",
                    actionLabel = "返回书库",
                    onAction = onBack,
                    showActionIcon = false
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(series, key = { it.first }) { (name, volumes) ->
                val isOpen = expanded == name
                val readVolume = volumes.filter { it.lastReadTimestamp > 0 }.maxByOrNull { it.lastReadTimestamp }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .asterCardInteraction(onClick = { expanded = if (isOpen) null else name })
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                volumes.take(3).forEachIndexed { index, book ->
                                    AsterBookCover(
                                        coverUrl = book.coverUrl,
                                        title = book.title,
                                        format = book.format,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .aspectRatio(0.68f)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "共 ${volumes.size} 卷" +
                                        (readVolume?.let { " · 读到 ${it.seriesOrder}" } ?: ""),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = if (isOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = isOpen) {
                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                                volumes.forEach { book ->
                                    VolumeRow(
                                        book = book,
                                        onOpen = {
                                            viewModel.pushSubScreen(SubScreen.PublicationDetail(book.id))
                                        },
                                        onRead = {
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
            }
        }
    }
}

@Composable
private fun VolumeRow(book: BookEntity, onOpen: () -> Unit, onRead: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 26.dp, height = 26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.seriesOrder.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (book.sourceStatus == "MISSING") {
                Text(
                    text = "文件已失效",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (book.progressPercent > 0) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
        Text(
            text = if (book.progressPercent >= 100) "已读完" else "${book.progressPercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 10.dp)
                .clickable(onClick = onRead)
                .heightIn(min = com.asterreader.core.designsystem.AsterLayout.TouchTarget)
        )
    }
}
