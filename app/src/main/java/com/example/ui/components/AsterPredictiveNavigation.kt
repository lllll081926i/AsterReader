package com.example.ui.components

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.asterreader.core.designsystem.AsterMotion
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.example.ui.SubScreen

internal data class NavigationFrame(val screen: SubScreen?, val tab: MainNavDestination, val depth: Int)
internal val LocalRootHandlesBack = staticCompositionLocalOf { false }

/** 手势完成才提交回退；返回不播放转场，取消保持原页。 */
@Composable
internal fun AsterPredictiveNavigation(
    current: NavigationFrame,
    previous: NavigationFrame,
    enabled: Boolean,
    selectionMode: Boolean,
    onBack: () -> Unit,
    content: @Composable (NavigationFrame) -> Unit
) {
    val reduceMotion = LocalAsterReduceMotion.current
    var backTarget by remember { mutableStateOf<NavigationFrame?>(null) }
    LaunchedEffect(current) {
        if (current != backTarget) backTarget = null
    }
    PredictiveBackHandler(enabled = enabled) { events ->
        events.collect { }
        if (!selectionMode) backTarget = previous
        onBack()
    }
    AnimatedContent(
        targetState = current,
        modifier = Modifier.fillMaxSize(),
        contentKey = { it },
        transitionSpec = {
            if (targetState == backTarget || targetState.depth < initialState.depth) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (initialState.screen == null && targetState.screen == null) AsterMotion.tabTransition(reduceMotion)
            else AsterMotion.pageTransition(reduceMotion, forward = targetState.depth >= initialState.depth)
        },
        label = "aster_navigation"
    ) { frame ->
        CompositionLocalProvider(LocalRootHandlesBack provides true) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) { content(frame) }
        }
    }
}
