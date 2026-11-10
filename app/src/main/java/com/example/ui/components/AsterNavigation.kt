package com.example.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.asterreader.core.designsystem.AsterMotion
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.example.ui.SubScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class NavigationFrame(val screen: SubScreen?, val tab: MainNavDestination, val depth: Int)
internal val LocalRootHandlesBack = staticCompositionLocalOf { false }

private val NavigationFrame.stateKey: String get() = "${tab.name}/$depth/$screen"

internal fun isReturning(previous: NavigationFrame, current: NavigationFrame): Boolean =
    current.depth < previous.depth

/** 只组合当前页面；保留在栈页面的可恢复状态，不保留旧页面的返回处理和资源。 */
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
    SideEffect {
        val validKeys = backStack?.map { it.stateKey }?.toSet()
        val removed = retainedKeys.filter {
            validKeys != null && it !in validKeys || !retainState && it == current.stateKey
        }
        removed.forEach(holder::removeState)
        retainedKeys = ArrayList((retainedKeys - removed + if (retainState) listOf(current.stateKey) else emptyList()).distinct())
    }
    val progress = remember(current) { Animatable(0f) }
    var edge by remember(current) { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val scope = rememberCoroutineScope()
    val latestFrame by rememberUpdatedState(current)
    val latestBack by rememberUpdatedState(onBack)
    val latestEnabled by rememberUpdatedState(enabled)
    var recovering by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    PredictiveBackHandler(enabled = enabled) { events ->
        val startedAt = latestFrame
        recovering?.cancel()
        try {
            events.collect { event ->
                edge = event.swipeEdge
                progress.snapTo(if (reduceMotion || !animateBack) 0f else event.progress.coerceIn(0f, 1f))
            }
            if (latestEnabled && latestFrame == startedAt) latestBack()
            progress.snapTo(0f)
        } catch (cancelled: CancellationException) {
            recovering = scope.launch { progress.animateTo(0f, AsterMotion.fastEffectsSpec) }
            throw cancelled
        }
    }
    var presented by remember { mutableStateOf(current) }
    val opacity = remember(current) {
        Animatable(if (current != presented && !isReturning(presented, current) && !reduceMotion) 0f else 1f)
    }
    SideEffect { presented = current }
    LaunchedEffect(current, reduceMotion) {
        if (reduceMotion) opacity.snapTo(1f)
        else opacity.animateTo(1f, AsterMotion.fastEffectsSpec)
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        key(current, retainState) {
            CompositionLocalProvider(LocalRootHandlesBack provides true) {
                val page: @Composable () -> Unit = {
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        val fraction = if (reduceMotion) 0f else progress.value
                        alpha = opacity.value
                        scaleX = 1f - AsterMotion.BackScaleReduction * fraction
                        scaleY = scaleX
                        translationX = AsterMotion.BackTranslation.toPx() * fraction *
                            if (edge == BackEventCompat.EDGE_LEFT) 1f else -1f
                        shape = RoundedCornerShape(AsterMotion.BackCornerRadius * fraction)
                        clip = fraction > 0f
                    }.background(MaterialTheme.colorScheme.surface)) { content(current) }
                }
                if (retainState) holder.SaveableStateProvider(current.stateKey, page)
                else CompositionLocalProvider(LocalSaveableStateRegistry provides null) { page() }
            }
        }
    }
}
