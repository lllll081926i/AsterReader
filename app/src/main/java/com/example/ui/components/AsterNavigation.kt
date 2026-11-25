package com.example.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.asterreader.core.designsystem.AsterMotion
import com.asterreader.core.designsystem.AsterSharedTransitionLayout
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.asterreader.core.designsystem.ProvideAsterAnimatedScope
import com.example.ui.SubScreen

internal data class NavigationFrame(
    val screen: SubScreen?,
    val tab: MainNavDestination,
    val depth: Int
) : NavKey

internal val LocalRootHandlesBack = staticCompositionLocalOf { false }

private const val FRAME_META_KEY = "aster_navigation_frame"

internal val NavigationFrame.stateKey: String get() = "${tab.name}/$depth/$screen"

internal fun isReturning(previous: NavigationFrame, current: NavigationFrame): Boolean =
    current.depth < previous.depth

/**
 * 基于 Jetpack Navigation 3 (`NavDisplay`) + `SharedTransitionLayout` 的根导航容器：
 * - 完整支持入栈、出栈、方向性 Tab 切换、预测性返回与共享元素过渡。
 * - 按主标签与在栈条目保留可恢复状态，出栈及受保护页面自动清理状态。
 * - 仅栈顶当前页面获得 `LocalRootHandlesBack`，防止退场动画中的页面重复拦截返回。
 */
@Composable
internal fun AsterNavigation(
    current: NavigationFrame,
    enabled: Boolean,
    onBack: () -> Unit,
    backStack: List<NavigationFrame>? = null,
    retainState: Boolean = true,
    animateBack: Boolean = true,
    content: @Composable (NavigationFrame) -> Unit
) {
    val reduceMotion = LocalAsterReduceMotion.current
    val holder = rememberSaveableStateHolder()
    var retainedKeys by rememberSaveable { mutableStateOf(arrayListOf<String>()) }

    val activeStack = remember(current, backStack) {
        val raw = if (!backStack.isNullOrEmpty()) {
            if (backStack.last() == current) backStack
            else {
                val filtered = backStack.filterNot { it == current }
                val base = if (filtered.isEmpty() && current.screen != null) {
                    listOf(NavigationFrame(null, MainNavDestination.LIBRARY, 0))
                } else {
                    filtered
                }
                base + current
            }
        } else if (current.screen == null && current.tab == MainNavDestination.LIBRARY) {
            listOf(current)
        } else if (current.screen == null) {
            listOf(NavigationFrame(null, MainNavDestination.LIBRARY, 0), current)
        } else {
            listOf(NavigationFrame(null, MainNavDestination.LIBRARY, 0), current)
        }
        raw.distinctBy { it.stateKey }
    }

    SideEffect {
        val tabBaseKeys = MainNavDestination.entries.map { NavigationFrame(null, it, 0).stateKey }
        val validKeys = (tabBaseKeys + activeStack.map { it.stateKey }).toSet()
        val removed = retainedKeys.filter {
            it !in validKeys || (!retainState && it == current.stateKey)
        }
        removed.forEach(holder::removeState)
        retainedKeys = ArrayList(
            (retainedKeys - removed.toSet() + if (retainState) listOf(current.stateKey) else emptyList()).distinct()
        )
    }

    // 单栈根页面处于批量选择等临时模式时，补充处理 Back 退出该模式。
    BackHandler(enabled = enabled && activeStack.size <= 1, onBack = onBack)

    val surfaceColor = MaterialTheme.colorScheme.surface
    AsterSharedTransitionLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) { sharedScope ->
        val entries = activeStack.map { frame ->
            val isTopFrame = frame == current
            val shouldRetainFrame = retainState || !isTopFrame
            NavEntry(
                key = frame,
                contentKey = frame.stateKey,
                metadata = mapOf(FRAME_META_KEY to frame)
            ) { navFrame ->
                val animatedScope = LocalNavAnimatedContentScope.current
                ProvideAsterAnimatedScope(animatedScope) {
                    CompositionLocalProvider(LocalRootHandlesBack provides isTopFrame) {
                        val page: @Composable () -> Unit = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(surfaceColor)
                            ) {
                                content(navFrame)
                            }
                        }
                        if (shouldRetainFrame && retainState) {
                            holder.SaveableStateProvider(navFrame.stateKey, page)
                        } else {
                            CompositionLocalProvider(LocalSaveableStateRegistry provides null) {
                                page()
                            }
                        }
                    }
                }
            }
        }

        NavDisplay(
            entries = entries,
            modifier = Modifier.fillMaxSize(),
            sharedTransitionScope = sharedScope,
            onBack = {
                if (enabled) onBack()
            },
            transitionSpec = {
                val initialFrame = initialState.entries.lastOrNull()?.metadata?.get(FRAME_META_KEY) as? NavigationFrame
                val targetFrame = targetState.entries.lastOrNull()?.metadata?.get(FRAME_META_KEY) as? NavigationFrame
                when {
                    targetFrame?.screen is SubScreen.Reader ->
                        AsterMotion.readerEnterTransition(reduceMotion)
                    initialFrame?.screen is SubScreen.Reader && targetFrame?.screen !is SubScreen.Reader ->
                        AsterMotion.readerExitTransition(reduceMotion)
                    initialFrame?.screen == null && targetFrame?.screen == null -> {
                        val initialIdx = MainNavDestination.entries.indexOf(initialFrame?.tab ?: MainNavDestination.LIBRARY)
                        val targetIdx = MainNavDestination.entries.indexOf(targetFrame?.tab ?: MainNavDestination.LIBRARY)
                        AsterMotion.tabTransition(reduceMotion, direction = if (targetIdx >= initialIdx) 1 else -1)
                    }
                    else -> AsterMotion.pageTransition(
                        reduceMotion = reduceMotion,
                        forward = (targetFrame?.depth ?: 0) >= (initialFrame?.depth ?: 0)
                    )
                }
            },
            popTransitionSpec = {
                val initialFrame = initialState.entries.lastOrNull()?.metadata?.get(FRAME_META_KEY) as? NavigationFrame
                val targetFrame = targetState.entries.lastOrNull()?.metadata?.get(FRAME_META_KEY) as? NavigationFrame
                when {
                    initialFrame?.screen is SubScreen.Reader ->
                        AsterMotion.readerExitTransition(reduceMotion)
                    initialFrame?.screen == null && targetFrame?.screen == null -> {
                        val initialIdx = MainNavDestination.entries.indexOf(initialFrame?.tab ?: MainNavDestination.LIBRARY)
                        val targetIdx = MainNavDestination.entries.indexOf(targetFrame?.tab ?: MainNavDestination.LIBRARY)
                        AsterMotion.tabTransition(reduceMotion, direction = if (targetIdx >= initialIdx) 1 else -1)
                    }
                    else -> AsterMotion.pageTransition(
                        reduceMotion = reduceMotion,
                        forward = false
                    )
                }
            },
            predictivePopTransitionSpec = { swipeEdge ->
                if (!animateBack || reduceMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    AsterMotion.predictivePopTransition(
                        reduceMotion = false,
                        fromLeftEdge = swipeEdge == BackEventCompat.EDGE_LEFT
                    )
                }
            }
        )
    }
}
