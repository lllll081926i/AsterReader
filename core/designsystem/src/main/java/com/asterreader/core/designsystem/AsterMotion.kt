package com.asterreader.core.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Aster Reader Material 3 Expressive Motion System
 * 采用柔顺流体缓动（Emphasized Easing）与微位移、轻量缩放与交错淡入淡出，彻底告别生硬卡顿。
 */
val LocalAsterReduceMotion = staticCompositionLocalOf { false }

object AsterMotion {
    // 经典流体柔顺缓动 (Material 3 Emphasized / Predictive Back curves)
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    fun <T> effectsSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis = 180, easing = EmphasizedDecelerateEasing)

    // Spatial Specs (Positional, Bounds, Resizing)
    val fastSpatialSpec = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

    val defaultSpatialSpec = spring<Float>(
        dampingRatio = 0.86f,
        stiffness = 300f
    )

    val slowSpatialSpec = spring<Float>(
        dampingRatio = 0.90f,
        stiffness = 200f
    )

    // Effects Specs
    val fastEffectsSpec = tween<Float>(durationMillis = 160, easing = EmphasizedDecelerateEasing)
    val defaultEffectsSpec = tween<Float>(durationMillis = 240, easing = EmphasizedDecelerateEasing)
    val slowEffectsSpec = tween<Float>(durationMillis = 350, easing = EmphasizedDecelerateEasing)

    /**
     * 流体页面导航转场：
     * 采用微位移（8%~10%）+ 微缩放（0.96 -> 1.0）+ 优雅淡入淡出，极致顺滑自然。
     */
    fun pageTransition(
        reduceMotion: Boolean = false,
        forward: Boolean = true
    ): ContentTransform {
        if (reduceMotion) {
            return androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
        }

        val slideDir = if (forward) 1 else -1
        val enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 280, easing = EmphasizedDecelerateEasing),
            initialOffsetX = { (it * 0.12f * slideDir).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 240, easing = EmphasizedDecelerateEasing)
        ) + scaleIn(
            animationSpec = tween(durationMillis = 280, easing = EmphasizedDecelerateEasing),
            initialScale = if (forward) 0.96f else 1.03f
        )

        val exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 240, easing = EmphasizedAccelerateEasing),
            targetOffsetX = { (-it * 0.08f * slideDir).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = EmphasizedAccelerateEasing)
        ) + scaleOut(
            animationSpec = tween(durationMillis = 240, easing = EmphasizedAccelerateEasing),
            targetScale = if (forward) 1.02f else 0.96f
        )

        return enter togetherWith exit
    }

    /**
     * 底部/模态抽屉转场
     */
    fun sheetTransition(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
        }
        val enter = slideInVertically(
            animationSpec = tween(durationMillis = 300, easing = EmphasizedDecelerateEasing),
            initialOffsetY = { (it * 0.15f).toInt() }
        ) + fadeIn(animationSpec = tween(durationMillis = 220)) + scaleIn(initialScale = 0.97f)

        val exit = slideOutVertically(
            animationSpec = tween(durationMillis = 220, easing = EmphasizedAccelerateEasing),
            targetOffsetY = { (it * 0.15f).toInt() }
        ) + fadeOut(animationSpec = tween(durationMillis = 180))

        return enter togetherWith exit
    }
}

/**
 * 卡片级微交互：按压时轻微回弹缩放 + 系统涟漪，同时保留长按。
 * 用于书库 / 书架的作品卡片，比单纯 ripple 更有触感，且遵守「减弱动效」。
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.asterCardInteraction(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalAsterReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "aster_card_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

/**
 * 具有触控弹性的微交互修饰符：按压时柔和微缩至 0.975f，松手迅速弹性回弹。
 */
fun Modifier.asterBounceClick(
    enabled: Boolean = true,
    targetScale: Float = 0.975f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = LocalAsterReduceMotion.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reduceMotion) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "aster_bounce_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}
