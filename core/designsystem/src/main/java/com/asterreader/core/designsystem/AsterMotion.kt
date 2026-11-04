package com.asterreader.core.designsystem

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
 * Aster Reader Material 3 Expressive Motion Tokens
 * As specified in design.md Section 5 and docs/21_MOTION_ANIMATION_SPEC.md.
 */
object AsterMotion {

    // Spatial Specs (Positional, Bounds, Resizing)
    val fastSpatialSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val defaultSpatialSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val slowSpatialSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    // Effects Specs (Alpha, Color, Elevation, Icon states)
    val fastEffectsSpec = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
    val defaultEffectsSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
    val slowEffectsSpec = tween<Float>(durationMillis = 400, easing = LinearOutSlowInEasing)

    /**
     * Standard page forward/backward navigation transition
     */
    fun pageTransition(
        reduceMotion: Boolean = false,
        forward: Boolean = true
    ): ContentTransform {
        if (reduceMotion) {
            return fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
        }

        val slideOffset = if (forward) 1 else -1
        return (slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetX = { it / 5 * slideOffset }
        ) + fadeIn(animationSpec = tween(220))) togetherWith
                (slideOutHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    targetOffsetX = { -it / 5 * slideOffset }
                ) + fadeOut(animationSpec = tween(180)))
    }

    /**
     * Modal sheet presentation transition
     */
    fun sheetTransition(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return fadeIn(animationSpec = tween(120)) togetherWith fadeOut(animationSpec = tween(100))
        }
        return (slideInVertically(
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            initialOffsetY = { it / 4 }
        ) + fadeIn(animationSpec = tween(200))) togetherWith
                (slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    targetOffsetY = { it / 4 }
                ) + fadeOut(animationSpec = tween(180)))
    }
}

/**
 * Tactile bouncing micro-interaction modifier for cards, covers, and interactive pills.
 * Subtly scales down on press (to 0.975f) with spring physics.
 */
fun Modifier.asterBounceClick(
    enabled: Boolean = true,
    targetScale: Float = 0.975f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
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
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                val up = waitForUpOrCancellation()
                isPressed = false
                if (up != null && onClick != null) {
                    onClick()
                }
            }
        }
}
