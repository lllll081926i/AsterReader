@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.asterreader.core.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onVisibilityChanged

/**
 * 跨页面共享元素上下文：统一封装 ExperimentalSharedTransitionApi，
 * 业务模块无需直接引入实验性动画 API，并支持在 Lazy 列表中按可见性延迟挂接。
 */
val LocalAsterSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAsterAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@Immutable
data class AsterSharedScopes(
    val sharedScope: SharedTransitionScope?,
    val animatedScope: AnimatedVisibilityScope?,
    val reduceMotion: Boolean
)

@Composable
fun rememberAsterSharedScopes(): AsterSharedScopes {
    val shared = LocalAsterSharedTransitionScope.current
    val animated = LocalAsterAnimatedVisibilityScope.current
    val reduceMotion = LocalAsterReduceMotion.current
    return remember(shared, animated, reduceMotion) {
        AsterSharedScopes(shared, animated, reduceMotion)
    }
}

@Composable
fun AsterSharedTransitionLayout(
    modifier: Modifier = Modifier,
    content: @Composable (SharedTransitionScope) -> Unit
) {
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalAsterSharedTransitionScope provides this) {
            content(this)
        }
    }
}

@Composable
fun ProvideAsterAnimatedScope(
    scope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAsterAnimatedVisibilityScope provides scope, content = content)
}

private val AsterBoundsTransform = BoundsTransform { _, _ -> AsterMotion.boundsSpatialSpec }

/**
 * 共享元素修饰符（如书封面 → 详情封面）。
 * [visibleOnly] = true 时通过 onVisibilityChanged 仅对视口内可见项挂接共享过渡，消除快速滚动时的预取开销。
 */
@Composable
fun Modifier.asterSharedElement(
    key: Any?,
    scopes: AsterSharedScopes = rememberAsterSharedScopes(),
    enabled: Boolean = true,
    visibleOnly: Boolean = false,
    clipShape: Shape? = null
): Modifier {
    val shared = scopes.sharedScope
    val animated = scopes.animatedScope
    if (!enabled || scopes.reduceMotion || shared == null || animated == null || key == null) {
        return this
    }
    var isVisible by remember(key, visibleOnly) { mutableStateOf(!visibleOnly) }
    val base = if (visibleOnly) {
        this.onVisibilityChanged(minFractionVisible = 0.001f) { visible ->
            isVisible = visible
        }
    } else {
        this
    }
    if (!isVisible) return base
    return with(shared) {
        if (clipShape != null) {
            base.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animated,
                boundsTransform = AsterBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(clipShape)
            )
        } else {
            base.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animated,
                boundsTransform = AsterBoundsTransform
            )
        }
    }
}

/**
 * 共享容器边界修饰符（如顶部搜索胶囊 → 搜索栏、书架卡 → 书架详情）。
 */
@Composable
fun Modifier.asterSharedBounds(
    key: Any?,
    scopes: AsterSharedScopes = rememberAsterSharedScopes(),
    enabled: Boolean = true,
    visibleOnly: Boolean = false,
    clipShape: Shape? = null
): Modifier {
    val shared = scopes.sharedScope
    val animated = scopes.animatedScope
    if (!enabled || scopes.reduceMotion || shared == null || animated == null || key == null) {
        return this
    }
    var isVisible by remember(key, visibleOnly) { mutableStateOf(!visibleOnly) }
    val base = if (visibleOnly) {
        this.onVisibilityChanged(minFractionVisible = 0.001f) { visible ->
            isVisible = visible
        }
    } else {
        this
    }
    if (!isVisible) return base
    return with(shared) {
        if (clipShape != null) {
            base.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animated,
                enter = EnterTransition.None + fadeIn(AsterMotion.fastEffectsSpec),
                exit = ExitTransition.None + fadeOut(AsterMotion.fastEffectsSpec),
                boundsTransform = AsterBoundsTransform,
                clipInOverlayDuringTransition = OverlayClip(clipShape)
            )
        } else {
            base.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animated,
                enter = EnterTransition.None + fadeIn(AsterMotion.fastEffectsSpec),
                exit = ExitTransition.None + fadeOut(AsterMotion.fastEffectsSpec),
                boundsTransform = AsterBoundsTransform
            )
        }
    }
}
