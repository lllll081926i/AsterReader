package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import com.asterreader.core.designsystem.AsterLayout

@Composable
fun AsterMainNavigation(expanded: Boolean, current: MainNavDestination,
    onSelect: (MainNavDestination) -> Unit, modifier: Modifier = Modifier, showStatsTab: Boolean = true) {
    val destinations = MainNavDestination.entries.filter { showStatsTab || it != MainNavDestination.STATS }
    if (expanded) NavigationRail(modifier.verticalScroll(rememberScrollState()).testTag("main_navigation_rail"), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        destinations.forEach { destination ->
            NavigationRailItem(selected = destination == current, onClick = { onSelect(destination) },
                icon = { Icon(destination.outlinedIcon, null, Modifier.size(AsterLayout.HeaderIconSize)) }, label = { Text(destination.label) },
                modifier = Modifier.testTag("nav_dock_${destination.route}"))
        }
    } else AppleFloatingBottomDock(currentDestination = current, onSelectDestination = onSelect,
        isExpandedTablet = false, modifier = modifier, showStatsTab = showStatsTab)
}
