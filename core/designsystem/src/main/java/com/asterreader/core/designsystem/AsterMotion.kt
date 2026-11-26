package com.asterreader.core.designsystem

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Aster Reader Material 3 Expressive Motion System
 * 基于 MaterialTheme.motionScheme + graphicsLayer 相位延迟，兼顾 60/120Hz 流畅度与可中断性。
 */
val LocalAsterReduceMotion = staticCompositionLocalOf { false }

object AsterMotion {
    const val BackScaleReduction = 0.08f
    val BackTranslation = 24.dp
    val BackCornerRadius = 28.dp

    // Material 3 Emphasized / Predictive Back curves
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0f, 1.0f)
    val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    // Spatial Specs (Positional, Bounds, Resizing)
    val fastSpatialSpec = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 500f
    )

    val defaultSpatialSpec = spring<Float>(
        dampingRatio = 0.88f,
        stiffness = 380f
    )

    val slowSpatialSpec = spring<Float>(
        dampingRatio = 0.90f,
        stiffness = 260f
    )

    val boundsSpatialSpec: FiniteAnimationSpec<Rect> = spring(
        dampingRatio = 0.88f,
        stiffness = 420f
    )

    val intOffsetSpatialSpec: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.88f,
        stiffness = 420f
    )

    val intSizeSpatialSpec: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.88f,
        stiffness = 420f
    )

    val pressSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 620f
    )

    // Effects Specs
    val fastEffectsSpec = tween<Float>(durationMillis = 160, easing = EmphasizedDecelerateEasing)
    val defaultEffectsSpec = tween<Float>(durationMillis = 240, easing = EmphasizedDecelerateEasing)
    val slowEffectsSpec = tween<Float>(durationMillis = 320, easing = EmphasizedDecelerateEasing)
    val errorShakeSpec = tween<Float>(durationMillis = 45, easing = LinearEasing)

    fun <T> effectsSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else tween(durationMillis = 180, easing = EmphasizedDecelerateEasing)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun <T> themeEffectsSpec(reduceMotion: Boolean = LocalAsterReduceMotion.current): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else MaterialTheme.motionScheme.fastEffectsSpec()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun <T> themeSpatialSpec(reduceMotion: Boolean = LocalAsterReduceMotion.current): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else MaterialTheme.motionScheme.defaultSpatialSpec()

    val dockPillSpringSpec = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = 460f
    )

    /**
     * 主导航 Tab 切换：按 Tab 顺序方向做清晰的水平滑入滑出 + 平滑交叉淡变，与底部滑块联动。
     */
    fun tabTransition(
        reduceMotion: Boolean,
        direction: Int = 1
    ): ContentTransform = if (reduceMotion) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val dir = if (direction >= 0) 1 else -1
        val enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 260, easing = EmphasizedDecelerateEasing),
            initialOffsetX = { (it * 0.18f * dir).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 30, easing = EmphasizedDecelerateEasing)
        )
        val exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 210, easing = EmphasizedAccelerateEasing),
            targetOffsetX = { (-it * 0.14f * dir).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150, easing = EmphasizedAccelerateEasing)
        )
        enter togetherWith exit
    }

    /**
     * 流体页面导航转场（MD3 Shared-Axis X 风格）：
     * 对称支持前进（入栈）与返回（出栈），基于 graphicsLayer 零重组执行。
     */
    fun pageTransition(
        reduceMotion: Boolean = false,
        forward: Boolean = true
    ): ContentTransform {
        if (reduceMotion) {
            return EnterTransition.None togetherWith ExitTransition.None
        }

        val slideDir = if (forward) 1 else -1
        val enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 280, easing = EmphasizedDecelerateEasing),
            initialOffsetX = { (it * (if (forward) 0.14f else 0.10f) * slideDir).toInt() }
        ) + fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 20, easing = EmphasizedDecelerateEasing)
        ) + scaleIn(
            animationSpec = tween(durationMillis = 280, easing = EmphasizedDecelerateEasing),
            initialScale = if (forward) 0.965f else 0.98f
        )

        val exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 240, easing = EmphasizedAccelerateEasing),
            targetOffsetX = { (-it * (if (forward) 0.08f else 0.16f) * slideDir).toInt() }
        ) + fadeOut(
            animationSpec = tween(durationMillis = 170, easing = EmphasizedAccelerateEasing)
        ) + scaleOut(
            animationSpec = tween(durationMillis = 240, easing = EmphasizedAccelerateEasing),
            targetScale = if (forward) 0.98f else 0.955f
        )

        return enter togetherWith exit
    }

    /**
     * Navigation 3 预测性返回转场：使用 LinearEasing 让手势进度 1:1 丝滑跟手。
     */
    fun predictivePopTransition(
        reduceMotion: Boolean = false,
        fromLeftEdge: Boolean = true
    ): ContentTransform {
        if (reduceMotion) {
            return EnterTransition.None togetherWith ExitTransition.None
        }
        val swipeDir = if (fromLeftEdge) 1 else -1
        val enter = slideInHorizontally(
            animationSpec = tween(durationMillis = 260, easing = LinearEasing),
            initialOffsetX = { (-it * 0.08f * swipeDir).toInt() }
        ) + scaleIn(
            animationSpec = tween(durationMillis = 260, easing = LinearEasing),
            initialScale = 0.96f
        ) + fadeIn(
            animationSpec = tween(durationMillis = 260, easing = LinearEasing),
            initialAlpha = 0.65f
        )

        val exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 260, easing = LinearEasing),
            targetOffsetX = { (it * 0.24f * swipeDir).toInt() }
        ) + scaleOut(
            animationSpec = tween(durationMillis = 260, easing = LinearEasing),
            targetScale = 0.90f
        ) + fadeOut(
            animationSpec = tween(durationMillis = 260, easing = LinearEasing),
            targetAlpha = 0.0f
        )

        return enter togetherWith exit
    }

    /** 进入阅读器：详情轻退，阅读沉浸画布接管。 */
    fun readerEnterTransition(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) return EnterTransition.None togetherWith ExitTransition.None
        val enter = fadeIn(tween(durationMillis = 210, easing = EmphasizedDecelerateEasing)) +
            scaleIn(tween(durationMillis = 260, easing = EmphasizedDecelerateEasing), initialScale = 0.97f)
        val exit = fadeOut(tween(durationMillis = 160, easing = EmphasizedAccelerateEasing)) +
            scaleOut(tween(durationMillis = 220, easing = EmphasizedAccelerateEasing), targetScale = 0.98f)
        return enter togetherWith exit
    }

    /** 退出阅读器：阅读画布轻收，详情/书库接管。 */
    fun readerExitTransition(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) return EnterTransition.None togetherWith ExitTransition.None
        val enter = fadeIn(tween(durationMillis = 200, easing = EmphasizedDecelerateEasing)) +
            scaleIn(tween(durationMillis = 240, easing = EmphasizedDecelerateEasing), initialScale = 0.98f)
        val exit = fadeOut(tween(durationMillis = 170, easing = EmphasizedAccelerateEasing)) +
            scaleOut(tween(durationMillis = 200, easing = EmphasizedAccelerateEasing), targetScale = 0.95f)
        return enter togetherWith exit
    }

    /** 应用锁解锁转场：锁屏柔和缩小淡出，主界面稳健淡入。 */
    fun authUnlockTransition(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) return EnterTransition.None togetherWith ExitTransition.None
        val enter = fadeIn(tween(durationMillis = 240, delayMillis = 40, easing = EmphasizedDecelerateEasing)) +
            scaleIn(tween(durationMillis = 260, easing = EmphasizedDecelerateEasing), initialScale = 0.975f)
        val exit = fadeOut(tween(durationMillis = 160, easing = EmphasizedAccelerateEasing)) +
            scaleOut(tween(durationMillis = 180, easing = EmphasizedAccelerateEasing), targetScale = 0.95f)
        return enter togetherWith exit
    }

    /** 底部/模态抽屉转场。 */
    fun sheetTransition(reduceMotion: Boolean = false): ContentTransform {
        if (reduceMotion) {
            return EnterTransition.None togetherWith ExitTransition.None
        }
        val enter = slideInVertically(
            animationSpec = tween(durationMillis = 280, easing = EmphasizedDecelerateEasing),
            initialOffsetY = { (it * 0.15f).toInt() }
        ) + fadeIn(animationSpec = tween(durationMillis = 210)) + scaleIn(initialScale = 0.97f)

        val exit = slideOutVertically(
            animationSpec = tween(durationMillis = 210, easing = EmphasizedAccelerateEasing),
            targetOffsetY = { (it * 0.15f).toInt() }
        ) + fadeOut(animationSpec = tween(durationMillis = 170))

        return enter togetherWith exit
    }
}

private data class PressScaleElement(
    val enabled: Boolean,
    val targetScale: Float
) : ModifierNodeElement<PressScaleNode>() {
    override fun create(): PressScaleNode = PressScaleNode(enabled = enabled, targetScale = targetScale)

    override fun update(node: PressScaleNode) {
        node.updateConfig(enabled = enabled, targetScale = targetScale)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "asterPressScale"
        properties["enabled"] = enabled
        properties["targetScale"] = targetScale
    }
}

/**
 * 基于 Modifier.Node 的零重组按压弹性节点：
 * 在 Initial 阶段监听按下/抬起/被滑动消费事件，Draw 阶段直接缩放，无 Modifier.composed 开销。
 */
private class PressScaleNode(
    private var enabled: Boolean,
    private var targetScale: Float
) : Modifier.Node(), PointerInputModifierNode, DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val scaleAnim = Animatable(1f)
    private var isPressed = false
    private var animJob: Job? = null

    fun updateConfig(enabled: Boolean, targetScale: Float) {
        this.enabled = enabled
        this.targetScale = targetScale
        if (!enabled && isPressed) {
            setPressed(false)
        }
    }

    private fun setPressed(pressed: Boolean) {
        if (isPressed == pressed) return
        isPressed = pressed
        val reduceMotion = currentValueOf(LocalAsterReduceMotion)
        val destination = if (pressed && enabled && !reduceMotion) targetScale else 1f
        animJob?.cancel()
        if (!isAttached) return
        animJob = coroutineScope.launch {
            if (reduceMotion) {
                scaleAnim.snapTo(destination)
            } else {
                scaleAnim.animateTo(destination, AsterMotion.pressSpringSpec)
            }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass != PointerEventPass.Initial) return
        if (!enabled) {
            if (isPressed) setPressed(false)
            return
        }
        when (pointerEvent.type) {
            PointerEventType.Press -> setPressed(true)
            PointerEventType.Move -> {
                if (isPressed && (pointerEvent.changes.any { it.isConsumed } || pointerEvent.changes.none { it.pressed })) {
                    setPressed(false)
                }
            }
            PointerEventType.Release, PointerEventType.Exit -> setPressed(false)
        }
    }

    override fun onCancelPointerInput() {
        setPressed(false)
    }

    override fun onDetach() {
        animJob?.cancel()
        isPressed = false
        super.onDetach()
    }

    override fun ContentDrawScope.draw() {
        val currentScale = scaleAnim.value
        if (currentScale == 1f) {
            drawContent()
        } else {
            scale(scaleX = currentScale, scaleY = currentScale) {
                this@draw.drawContent()
            }
        }
    }
}

/**
 * 卡片级微交互：按压时轻微回弹缩放 + 系统涟漪，同时保留长按。
 * 零 Modifier.composed 开销，遵守「减弱动效」。
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.asterCardInteraction(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = this
    .then(PressScaleElement(enabled = enabled, targetScale = 0.975f))
    .combinedClickable(
        enabled = enabled,
        onLongClick = onLongClick,
        onClick = onClick
    )

/**
 * 具有触控弹性的微交互修饰符：按压时柔和微缩至 targetScale，松手迅速弹性回弹。
 */
fun Modifier.asterBounceClick(
    enabled: Boolean = true,
    targetScale: Float = 0.975f,
    onClick: (() -> Unit)? = null
): Modifier = this
    .then(PressScaleElement(enabled = enabled, targetScale = targetScale))
    .then(
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
        } else {
            Modifier
        }
    )

/**
 * 判断系统级动效状态是否应当降级为「减弱动效」。
 */
fun isSystemReduceMotionEnabled(
    animatorScale: Float,
    animatorsEnabled: Boolean
): Boolean = animatorScale <= 0f || !animatorsEnabled

/**
 * 根容器级系统动效缩放监听：仅在根节点注册单个 ContentObserver，
 * 动态响应「开发者选项 -> 动画程序时长缩放」变更而无需重启 Activity。
 */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    fun readReduceMotion(): Boolean {
        val scale = runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        val enabled = runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
        return isSystemReduceMotionEnabled(scale, enabled)
    }
    var reduceMotion by remember(resolver) { mutableStateOf(readReduceMotion()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = readReduceMotion()
            }
        }
        runCatching {
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
        }
        reduceMotion = readReduceMotion()
        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }
    return reduceMotion
}

private data class ContentRevealElement(
    val contentKey: Any?,
    val ready: Boolean
) : ModifierNodeElement<ContentRevealNode>() {
    override fun create(): ContentRevealNode = ContentRevealNode(contentKey, ready)

    override fun update(node: ContentRevealNode) {
        node.updateState(contentKey, ready)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "asterContentReveal"
        properties["contentKey"] = contentKey
        properties["ready"] = ready
    }
}

private class ContentRevealNode(
    private var contentKey: Any?,
    private var ready: Boolean
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    private val progress = Animatable(1f)
    private var initialized = false
    private var lastKey: Any? = contentKey
    private var lastReady: Boolean = ready
    private var revealJob: Job? = null

    override fun onAttach() {
        super.onAttach()
        if (!initialized) {
            initialized = true
            lastKey = contentKey
            lastReady = ready
            val reduceMotion = currentValueOf(LocalAsterReduceMotion)
            if (!ready && !reduceMotion) {
                coroutineScope.launch { progress.snapTo(0f) }
            }
        }
    }

    fun updateState(newKey: Any?, newReady: Boolean) {
        val keyChanged = newKey != lastKey
        val readyChanged = newReady != lastReady
        contentKey = newKey
        ready = newReady
        if (!keyChanged && !readyChanged) return

        val prevReady = lastReady
        lastKey = newKey
        lastReady = newReady
        val reduceMotion = currentValueOf(LocalAsterReduceMotion)
        revealJob?.cancel()
        if (!isAttached) return
        revealJob = coroutineScope.launch {
            when {
                reduceMotion -> progress.snapTo(1f)
                !newReady -> progress.snapTo(0f)
                else -> {
                    val startAlpha = if (!prevReady) 0f else 0.72f
                    progress.snapTo(startAlpha)
                    progress.animateTo(1f, AsterMotion.fastEffectsSpec)
                }
            }
        }
    }

    override fun onDetach() {
        revealJob?.cancel()
        revealJob = null
        super.onDetach()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val liftPx = 6.dp.toPx()
        return layout(placeable.width, placeable.height) {
            val value = progress.value.coerceIn(0f, 1f)
            if (value >= 0.999f) {
                placeable.place(0, 0)
            } else {
                placeable.placeWithLayer(0, 0) {
                    alpha = value
                    translationY = (1f - value) * liftPx
                }
            }
        }
    }
}

/**
 * 轻量内容显影：当筛选/搜索结果切换或数据首次就绪时，保持列表与滚动位置不卸载，
 * 仅在 RenderNode 图层阶段做轻微上浮 + 淡入，零重组开销且严格遵守「减弱动效」。
 */
fun Modifier.asterContentReveal(
    contentKey: Any?,
    ready: Boolean = true
): Modifier = this.then(ContentRevealElement(contentKey = contentKey, ready = ready))

private data class AsterShimmerElement(
    val baseColor: Color,
    val enabled: Boolean
) : ModifierNodeElement<AsterShimmerNode>() {
    override fun create(): AsterShimmerNode = AsterShimmerNode(baseColor, enabled)

    override fun update(node: AsterShimmerNode) {
        node.updateState(baseColor, enabled)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "asterShimmer"
        properties["enabled"] = enabled
    }
}

private class AsterShimmerNode(
    private var baseColor: Color,
    private var enabled: Boolean
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val phase = Animatable(0f)
    private var shimmerJob: Job? = null

    override fun onAttach() {
        super.onAttach()
        syncAnimation()
    }

    fun updateState(newColor: Color, newEnabled: Boolean) {
        baseColor = newColor
        enabled = newEnabled
        syncAnimation()
    }

    private fun syncAnimation() {
        val active = isAttached && enabled && !currentValueOf(LocalAsterReduceMotion)
        if (!active) {
            shimmerJob?.cancel()
            shimmerJob = null
            return
        }
        if (shimmerJob?.isActive == true) return
        shimmerJob = coroutineScope.launch {
            phase.snapTo(0f)
            phase.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    override fun onDetach() {
        shimmerJob?.cancel()
        shimmerJob = null
        super.onDetach()
    }

    override fun ContentDrawScope.draw() {
        val reduceMotion = currentValueOf(LocalAsterReduceMotion)
        if (!enabled || reduceMotion) {
            drawRect(color = baseColor.copy(alpha = 0.42f))
        } else {
            val p = phase.value
            val span = (size.width + size.height).coerceAtLeast(1f)
            val travel = span * (p * 1.6f - 0.3f)
            val bandWidth = (size.width * 0.55f).coerceAtLeast(1f)
            val brush = Brush.linearGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.45f),
                    baseColor.copy(alpha = 0.16f),
                    baseColor.copy(alpha = 0.45f)
                ),
                start = Offset(travel - bandWidth, 0f),
                end = Offset(travel, size.height)
            )
            drawRect(brush = brush)
        }
        drawContent()
    }
}

/**
 * 合规骨架闪烁修饰符：纯 Modifier.Node 绘制，开启「减弱动效」时自动停用无限循环动画（符合 design.md §5.5）。
 */
fun Modifier.asterShimmer(
    baseColor: Color,
    enabled: Boolean = true
): Modifier = this.then(AsterShimmerElement(baseColor = baseColor, enabled = enabled))
