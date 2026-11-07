package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterreader.core.designsystem.asterCardInteraction
import com.asterreader.core.database.BookEntity
import com.example.ui.AsterViewModel
import com.example.ui.components.AsterBookCover
import com.example.ui.components.AsterEmptyState
import com.example.ui.components.AsterSubScreenHeader
import com.example.ui.theme.LocalAsterColors

/**
 * 已隐藏作品。进入前必须通过应用锁验证（入口由 [AsterViewModel.openHiddenBooks] 保证）。
 * 这里只改变可见性：文件字节保持原样，验证身份后照常阅读。
 */
@Composable
fun HiddenBooksScreen(
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    val books by viewModel.hiddenBooks.collectAsStateWithLifecycle()
    var pendingUnhide by remember { mutableStateOf<BookEntity?>(null) }
    val colorScheme = MaterialTheme.colorScheme
    val asterColors = LocalAsterColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
    ) {
        AsterSubScreenHeader(
            title = "已隐藏",
            onBack = onBack,
            subtitle = "验证后可见 · 文件未改动",
            trailing = {
                if (books.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${books.size} 本",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        )

        if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsterEmptyState(
                    title = "没有隐藏的作品",
                    description = "在作品详情里选择「隐藏作品」，它就会从书库列表移到这里。",
                    compact = true,
                    actionLabel = "返回书库",
                    onAction = onBack,
                    showActionIcon = false
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = asterColors.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "这些作品只是从书库移出，文件本身没有改动。点封面即可继续阅读。",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(books, key = { it.id }) { book ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = asterColors.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AsterBookCover(
                                coverUrl = book.coverUrl,
                                title = book.title,
                                format = book.format,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .asterCardInteraction(onClick = { viewModel.openBookReader(book.id) })
                            )
                            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 6.dp)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = buildString {
                                        append(book.format)
                                        if (book.totalPages > 0) append(" · ${book.totalPages} 页")
                                        if (book.progressPercent in 1..99) append(" · ${book.progressPercent}%")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                    .asterCardInteraction(onClick = { pendingUnhide = book })
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "取消隐藏",
                                    style = MaterialTheme.typography.labelLarge,
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
    }

    pendingUnhide?.let { book ->
        AlertDialog(
            onDismissRequest = { pendingUnhide = null },
            title = { Text("取消隐藏", fontWeight = FontWeight.SemiBold) },
            text = { Text("《${book.title}》将重新出现在书库中，文件本身不会有任何改动。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setBookHidden(book.id, false)
                    pendingUnhide = null
                }) { Text("取消隐藏") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnhide = null }) { Text("返回") }
            }
        )
    }
}
