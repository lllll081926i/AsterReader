package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.asterreader.core.designsystem.AsterMotion
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.example.ui.SubScreen

internal data class NavigationFrame(val screen: SubScreen?, val tab: MainNavDestination, val depth: Int)
internal val LocalRootHandlesBack = staticCompositionLocalOf { false }

/** 只保留当前页面，普通淡入切换不延长旧页面的资源与返回处理生命周期。 */
@Composable
internal fun AsterNavigation(
    current: NavigationFrame,
    enabled: Boolean,
    onBack: () -> Unit,
    content: @Composable (NavigationFrame) -> Unit
) {
    BackHandler(enabled = enabled, onBack = onBack)
    val reduceMotion = LocalAsterReduceMotion.current
    var presented by remember { mutableStateOf(current) }
    val opacity = remember(current) { Animatable(if (current != presented && !reduceMotion) 0f else 1f) }
    SideEffect { presented = current }
    LaunchedEffect(current, reduceMotion) {
        if (reduceMotion) opacity.snapTo(1f)
        else opacity.animateTo(1f, AsterMotion.fastEffectsSpec)
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        key(current) {
            CompositionLocalProvider(LocalRootHandlesBack provides true) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = opacity.value }) { content(current) }
            }
        }
    }
}
