package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAsterColors

/**
 * 统一空状态：插画 + 标题 + 说明 + 主/次操作。
 * 书库、搜索、系列、统计等页面共用，避免各处「一行灰字」的敷衍感。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsterEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    compact: Boolean = false,
    showActionIcon: Boolean = true,
    actionIcon: ImageVector = Icons.Outlined.Description,
    secondaryIcon: ImageVector = Icons.Outlined.CreateNewFolder
) {
    val colorScheme = MaterialTheme.colorScheme
    val asterColors = LocalAsterColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = asterColors.surfaceContainerLow,
            modifier = Modifier.size(if (compact) 88.dp else 120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                TranquilDeskEmptyIllustration(size = if (compact) 72.dp else 100.dp)
            }
        }
        Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            if (secondaryLabel != null && onSecondaryAction != null) {
                AsterPairedActions(actionLabel, actionIcon, onAction, secondaryLabel, secondaryIcon, onSecondaryAction)
            } else {
                Button(onClick = onAction, shape = CircleShape, modifier = Modifier.heightIn(min = com.asterreader.core.designsystem.AsterLayout.TouchTarget)) {
                    if (showActionIcon) {
                        Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(actionLabel)
                }
            }
        }
    }
}

/** 轻量分区标题：标题 + 计数 + 可选尾随内容，用于书库 / 系列等长列表。 */@Composable
fun AsterSectionLabel(
    title: String,
    count: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 二级页面统一顶栏：返回箭头贴在最左侧（距屏幕边缘 4dp），标题左对齐。
 * 所有子页面共用同一套间距，避免「返回按钮到处飘、标题每页都不一样」。
 */
@Composable
fun AsterSubScreenHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = com.asterreader.core.designsystem.AsterLayout.HeaderHeight)
            .padding(start = if (onBack == null) 16.dp else 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(com.asterreader.core.designsystem.AsterLayout.TouchTarget)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) 0.dp else 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailing?.invoke()
    }
}
