package com.asterreader.core.designsystem

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp

data class AsterChoice(val label: String, val icon: ImageVector)

object AsterSettingsColors {
    val container
        @Composable get() = lerp(MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.primaryContainer, 0.30f)
}

/** 短组使用系统分段按钮；长组及大字体使用整行勾选。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterSettingsChoice(options: List<AsterChoice>, selectedIndex: Int, onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (options.size in 2..3 && maxWidth >= 104.dp * fontScale * options.size) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    SegmentedButton(selected = selectedIndex == index, onClick = { onSelect(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        modifier = Modifier.weight(1f).heightIn(min = AsterLayout.SettingsRowHeight),
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
                    .heightIn(min = AsterLayout.SettingsRowHeight).padding(horizontal = 16.dp, vertical = 10.dp),
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
