package com.asterreader.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

data class AsterChoice(val label: String, val icon: ImageVector, val description: String? = null)

object AsterSettingsColors {
    val container
        @Composable get() = lerp(MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.primaryContainer, 0.30f)
}

/** 短组使用系统分段按钮；长组及大字体使用整行勾选。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterSettingsChoice(options: List<AsterChoice>, selectedIndex: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier, compact: Boolean = false, singleRow: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (singleRow && options.isNotEmpty()) {
            val width = maxOf((maxWidth - AsterLayout.CompactSpacing * (options.size - 1)) / options.size,
                AsterLayout.CompactChoiceMinWidth * fontScale)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(AsterLayout.CompactSpacing)) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    Row(Modifier.width(width)
                        .background(if (selected) colors.primaryContainer else AsterSettingsColors.container, MaterialTheme.shapes.small)
                        .selectable(selected, role = Role.RadioButton, onClick = { onSelect(index) })
                        .semantics { option.description?.let { contentDescription = it } }
                        .heightIn(min = AsterLayout.TouchTarget).padding(AsterLayout.CompactSpacing),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(if (selected) Icons.Outlined.Check else option.icon, null,
                            Modifier.size(AsterLayout.CompactChoiceIconSize), colors.primary)
                        Spacer(Modifier.width(AsterLayout.CompactSpacing))
                        Text(option.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false,
                            color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                    }
                }
            }
        } else if (compact && options.size == 4 && maxWidth >= 300.dp * fontScale) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
                options.indices.chunked(2).forEach { indices ->
                    Row(horizontalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
                        indices.forEach { index ->
                            val selected = index == selectedIndex
                            val option = options[index]
                            Row(Modifier.weight(1f)
                                .background(if (selected) colors.primaryContainer else AsterSettingsColors.container,
                                    MaterialTheme.shapes.small)
                                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(index) })
                                .heightIn(min = AsterLayout.TouchTarget).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(if (selected) Icons.Outlined.Check else option.icon, null,
                                    Modifier.size(AsterLayout.HeaderIconSize), colors.primary)
                                Text(option.label, style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                            }
                        }
                    }
                }
            }
        } else if (options.size in 2..3 && maxWidth >= 104.dp * fontScale * options.size) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(selected = selectedIndex == index, onClick = { onSelect(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        modifier = Modifier.weight(1f).heightIn(min = if (compact) AsterLayout.TouchTarget else AsterLayout.SettingsRowHeight),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primaryContainer, activeContentColor = colors.onPrimaryContainer,
                            activeBorderColor = colors.primary, inactiveContainerColor = AsterSettingsColors.container,
                            inactiveContentColor = colors.onSurface, inactiveBorderColor = colors.outlineVariant),
                        icon = { Icon(if (selectedIndex == index) Icons.Outlined.Check else option.icon, null, Modifier.size(18.dp)) },
                        label = { Text(option.label, style = MaterialTheme.typography.labelLarge) })
                }
            }
        } else Column(Modifier.fillMaxWidth().selectableGroup()) {
            options.forEachIndexed { index, option ->
                val selected = selectedIndex == index
                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth()
                    .background(if (selected) colors.primaryContainer else AsterSettingsColors.container)
                    .selectable(selected, role = Role.RadioButton, onClick = { onSelect(index) })
                    .heightIn(min = if (compact) AsterLayout.TouchTarget else AsterLayout.SettingsRowHeight)
                    .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(option.icon, null, Modifier.size(20.dp), if (selected) colors.onPrimaryContainer else colors.primary)
                    Text(option.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                    if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), colors.onPrimaryContainer)
                    else Spacer(Modifier.size(20.dp))
                }
            }
        }
    }
}
