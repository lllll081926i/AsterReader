package com.example.ui.components

import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun AsterMainNavigation(expanded: Boolean, current: MainNavDestination,
    onSelect: (MainNavDestination) -> Unit, modifier: Modifier = Modifier) {
    val destinations = listOf(MainNavDestination.LIBRARY, MainNavDestination.SHELVES)
    if (expanded) NavigationRail(modifier, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        destinations.forEach { destination ->
            NavigationRailItem(selected = destination == current, onClick = { onSelect(destination) },
                icon = { Icon(destination.outlinedIcon, null) }, label = { Text(destination.label) },
                modifier = Modifier.testTag("nav_dock_${destination.route}"))
        }
    } else Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow).navigationBarsPadding()) {
        NavigationBar(Modifier.height(com.asterreader.core.designsystem.AsterLayout.MainNavigationHeight),
            windowInsets = WindowInsets(0), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        destinations.forEach { destination ->
            NavigationBarItem(selected = destination == current, onClick = { onSelect(destination) },
                icon = { Icon(if (destination == current) destination.filledIcon else destination.outlinedIcon, null) },
                label = { Text(destination.label) }, modifier = Modifier.testTag("nav_dock_${destination.route}"))
        }
        }
    }
}
