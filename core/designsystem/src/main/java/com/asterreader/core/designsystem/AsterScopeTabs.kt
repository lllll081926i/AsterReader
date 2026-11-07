package com.asterreader.core.designsystem

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsterScopeTabs(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    SecondaryTabRow(selectedTabIndex = selectedIndex, modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary,
        divider = {}) {
        labels.forEachIndexed { index, label ->
            Tab(selected = selectedIndex == index, onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = AsterLayout.TouchTarget),
                text = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) })
        }
    }
}
