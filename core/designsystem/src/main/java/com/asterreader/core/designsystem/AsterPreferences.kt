package com.asterreader.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

object AsterPreferenceLayout {
    val ContentMaxWidth = 640.dp
    val SectionSpacing = 24.dp
    val TitleSpacing = 8.dp
    val IconSize = 24.dp
    val RowHeight = 56.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterPreferenceHeader(title: String, onBack: (() -> Unit)?, backLabel: String = "返回") {
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, backLabel)
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        windowInsets = WindowInsets(0, 0, 0, 0))
}

@Composable
fun AsterPreferenceGroup(title: String? = null, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AsterPreferenceLayout.TitleSpacing)) {
        if (!title.isNullOrBlank()) Text(title,
            Modifier.padding(horizontal = AsterLayout.PagePadding).semantics { heading() },
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
fun AsterPreferenceDivider() {
    HorizontalDivider(Modifier.padding(horizontal = AsterLayout.PagePadding), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AsterPreferenceItem(title: String, leadingIcon: ImageVector? = null, trailingText: String? = null,
    isDanger: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    val contentColor = (if (isDanger) colors.error else colors.onSurface).let {
        if (enabled) it else it.copy(alpha = 0.38f)
    }
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = trailingText?.takeIf(String::isNotBlank)?.let { value ->
            { Text(value, style = MaterialTheme.typography.bodyMedium) }
        },
        leadingContent = leadingIcon?.let { icon ->
            { Icon(icon, null, Modifier.size(AsterPreferenceLayout.IconSize)) }
        },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
            Modifier.size(AsterPreferenceLayout.IconSize)) },
        colors = ListItemDefaults.colors(containerColor = colors.surfaceContainerLow,
            headlineColor = contentColor, leadingIconColor = if (enabled) colors.onSurfaceVariant else contentColor,
            trailingIconColor = if (enabled) colors.onSurfaceVariant else contentColor),
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = AsterPreferenceLayout.RowHeight)
    )
}

@Composable
fun AsterPreferenceSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true, modifier: Modifier = Modifier) {
    ListItem(headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        trailingContent = { Switch(checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)),
        modifier = modifier.fillMaxWidth()
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = AsterPreferenceLayout.RowHeight))
}

@Composable
fun AsterPreferenceSlider(label: String, valueText: String, value: Float,
    range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AsterLayout.PagePadding, vertical = AsterLayout.ControlSpacing)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value, onChange, Modifier.fillMaxWidth().semantics {
            contentDescription = label
            stateDescription = valueText
        }, valueRange = range, steps = steps)
    }
}
