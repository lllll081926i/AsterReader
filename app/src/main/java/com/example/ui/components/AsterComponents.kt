package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.ui.semantics.selected
import com.asterreader.core.designsystem.AsterMotion
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asterreader.core.designsystem.asterBounceClick
import com.example.ui.theme.LocalAsterColors

enum class MainNavDestination(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    LIBRARY("library", "书库", Icons.Filled.LocalLibrary, Icons.Outlined.LocalLibrary),
    SHELVES("shelves", "书架", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
    SEARCH("search", "搜索", Icons.Filled.Search, Icons.Outlined.Search)
}

/**
 * Compact Apple-style Floating Bottom Dock:
 * - Icon on top, short title on bottom
 * - Displays Library ("书库"), Shelves ("书架"), and Search ("搜索")
 * - Never wraps text
 */
@Composable
fun AppleFloatingBottomDock(
    currentDestination: MainNavDestination,
    onSelectDestination: (MainNavDestination) -> Unit,
    libraryCount: Int,
    isVaultUnlocked: Boolean,
    isExpandedTablet: Boolean,
    lastReadTitle: String? = null,
    onQuickResumeRead: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme
    val dockItems = listOf(
        MainNavDestination.LIBRARY,
        MainNavDestination.SHELVES,
        MainNavDestination.SEARCH
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = asterColors.surfaceContainerHigh.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(
                width = 0.8.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier.testTag("apple_floating_bottom_dock")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dockItems.forEach { dest ->
                    val selected = dest == currentDestination
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) colorScheme.secondaryContainer else Color.Transparent,
                        animationSpec = AsterMotion.effectsSpec(LocalAsterReduceMotion.current),
                        label = "dock_bg_${dest.name}"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                        animationSpec = AsterMotion.effectsSpec(LocalAsterReduceMotion.current),
                        label = "dock_fg_${dest.name}"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        animationSpec = AsterMotion.effectsSpec(LocalAsterReduceMotion.current),
                        label = "dock_icon_${dest.name}"
                    )

                    Column(
                        modifier = Modifier
                            .widthIn(min = 68.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(pillBg)
                            .asterBounceClick { onSelectDestination(dest) }
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .semantics { this.selected = selected }
                            .testTag("nav_dock_${dest.route}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                            contentDescription = dest.label,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (isExpandedTablet && !lastReadTitle.isNullOrBlank() && onQuickResumeRead != null) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .width(1.dp)
                            .height(26.dp)
                            .background(colorScheme.outlineVariant.copy(alpha = 0.45f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .asterBounceClick { onQuickResumeRead() }
                            .testTag("dock_quick_resume_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = "继续阅读",
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "续读",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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

/**
 * Compact 52dp Top Search Capsule
 */
@Composable
fun AsterTopSearchCapsule(
    placeholder: String = "搜索书名、作者、标签",
    activeFilterCount: Int = 0,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = CircleShape,
        color = asterColors.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .clickable { onSearchClick() }
            .testTag("aster_top_search_capsule")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "搜索",
                    tint = colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.outline,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = CircleShape,
                color = if (activeFilterCount > 0) colorScheme.secondaryContainer else Color.Transparent,
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .clickable { onFilterClick() }
                    .testTag("top_search_filter_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (activeFilterCount > 0) "筛选 $activeFilterCount" else "筛选",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activeFilterCount > 0) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "筛选",
                        tint = if (activeFilterCount > 0) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 书库中的「已隐藏」占位卡：只改变可见性，文件本身没有任何加密。
 * 点击后走应用锁验证（PIN / 系统生物识别）。
 */
@Composable
fun SafeProtectedBookCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .asterBounceClick(onClick = onClick)
            .semantics { contentDescription = "已隐藏作品，点击验证身份" }
            .testTag("vault_protected_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .background(asterColors.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "已隐藏",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(asterColors.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "已隐藏",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "验证后可见",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.outline,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "已隐藏作品",
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = "需验证身份",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.outline,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}

/**
 * AsterSettingsGroup: Continuous card rounded at 24dp using surfaceContainerLow fill
 */
@Composable
fun AsterSettingsGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!title.isNullOrBlank()) {
            // 分组标题只保留一行小字：不再画图标徽章 + 主色标题 + 说明，
            // 三层信息堆在一起既抢眼又让每屏看起来杂乱。
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp
                ),
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = asterColors.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun AsterGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

/**
 * AsterSettingsRow: Clean setting row without multi-line badge wrapping
 */
@Composable
fun AsterSettingsRow(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    leadingIcon: ImageVector? = null,
    trailingText: String? = null,
    trailingTextHighlight: Boolean = false,
    isDanger: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val titleColor = if (isDanger) colorScheme.error else colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isDanger) colorScheme.error else colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (trailingTextHighlight) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = if (trailingTextHighlight) colorScheme.primary else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (isDanger) colorScheme.error else colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * AsterSwitchRow: Clean settings switch row without text wrapping
 */
@Composable
fun AsterSwitchRow(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .semantics { role = Role.Switch }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!badgeText.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            thumbContent = if (checked) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorScheme.onPrimary,
                checkedTrackColor = colorScheme.primary,
                checkedIconColor = colorScheme.primary,
                uncheckedThumbColor = colorScheme.outline,
                uncheckedTrackColor = asterColors.surfaceContainerHighest
            )
        )
    }
}
