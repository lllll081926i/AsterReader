package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

/** 在同一次测量中预留导航空间，避免导航高度回写状态引起首帧跳动。 */
@Composable
fun AsterNavigationLayout(reserveNavigation: Boolean, useRail: Boolean, modifier: Modifier = Modifier,
    content: @Composable () -> Unit, navigation: @Composable () -> Unit, overlay: @Composable () -> Unit) {
    Layout(modifier = modifier.fillMaxSize(), content = {
        Box(Modifier.fillMaxSize()) { content() }
        Box { navigation() }
        Box(Modifier.fillMaxSize()) { overlay() }
    }) { children, constraints ->
        val nav = children[1].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val start = if (reserveNavigation && useRail) nav.width else 0
        val bottom = if (reserveNavigation && !useRail) nav.height else 0
        val bodyConstraints = androidx.compose.ui.unit.Constraints.fixed(
            (width - start).coerceAtLeast(0), (height - bottom).coerceAtLeast(0))
        val body = children[0].measure(bodyConstraints)
        val overlayPlaceable = children[2].measure(bodyConstraints)
        layout(width, height) {
            body.placeRelative(start, 0)
            nav.placeRelative(if (useRail) 0 else (width - nav.width) / 2,
                if (useRail) (height - nav.height) / 2 else height - nav.height)
            overlayPlaceable.placeRelative(start, 0)
        }
    }
}
