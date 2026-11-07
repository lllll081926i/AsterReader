package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.onClick
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.asterreader.core.designsystem.AsterMotion
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.ReaderLayoutMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 漫画渲染画布：
 * 1. 条漫页内进度按「当前页 + 页内比例」上报，旋转/重建后可恢复；
 * 2. 触点中心双击缩放、双指缩放、图片边缘翻页；
 * 3. 双页对开独立排版；
 * 4. 翻页前预热相邻页，放大时叠加原始分辨率条带。
 */
@Composable
fun PublicationPageCanvas(
    publicationId: String,
    page: Int,
    totalPages: Int,
    mode: ReaderLayoutMode,
    expanded: Boolean,
    coverSingle: Boolean = true,
    rightToLeft: Boolean = mode == ReaderLayoutMode.RTL_PAGER,
    fitMode: String = "适合屏幕",
    initialScale: Float = 1f,
    initialPanX: Float = 0f,
    initialPanY: Float = 0f,
    initialOffsetFraction: Float = 0f,
    loadPage: suspend (Int, Int) -> Bitmap,
    loadRegion: suspend (Int, Float, Float, Int) -> Bitmap? = { _, _, _, _ -> null },
    onPrefetch: (List<Int>) -> Unit = {},
    onPageChanged: (Int) -> Unit,
    onViewportChanged: (page: Int, offsetFraction: Float, scale: Float, panX: Float, panY: Float) -> Unit,
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onToggleChrome: () -> Unit
) {
    val reduceMotion = LocalAsterReduceMotion.current
    var zoomScale by remember(publicationId, page) { mutableFloatStateOf(initialScale.coerceIn(1f, 4f)) }
    var panX by remember(publicationId, page) { mutableFloatStateOf(initialPanX) }
    var panY by remember(publicationId, page) { mutableFloatStateOf(initialPanY) }

    // 相邻页预热：翻页之前就把下一/上一页读进缓存，消除翻页转圈
    LaunchedEffect(publicationId, page, totalPages) {
        val neighbors = buildList {
            if (page + 1 <= totalPages) add(page + 1)
            if (page - 1 >= 1) add(page - 1)
            if (mode == ReaderLayoutMode.VERTICAL_SCROLL && page + 2 <= totalPages) add(page + 2)
        }
        if (neighbors.isNotEmpty()) onPrefetch(neighbors)
    }

    if (mode == ReaderLayoutMode.VERTICAL_SCROLL) {
        val list = rememberLazyListState(initialFirstVisibleItemIndex = (page - 1).coerceIn(0, totalPages - 1))
        var lastReported by remember(publicationId) { mutableIntStateOf(page) }
        var restored by remember(publicationId) { mutableStateOf(false) }

        // 恢复页内滚动比例：首帧布局完成后按比例跳转
        LaunchedEffect(page, initialOffsetFraction, list.layoutInfo.totalItemsCount) {
            if (restored || initialOffsetFraction <= 0f) return@LaunchedEffect
            val info = list.layoutInfo.visibleItemsInfo.firstOrNull { it.index == page - 1 } ?: return@LaunchedEffect
            if (info.size > 0) {
                list.scrollToItem(page - 1, (info.size * initialOffsetFraction).toInt())
                restored = true
            }
        }

        var programmaticScroll by remember(publicationId) { mutableStateOf(false) }
        LaunchedEffect(page) {
            if (page != lastReported) {
                programmaticScroll = true
                try {
                    if (reduceMotion) list.scrollToItem((page - 1).coerceIn(0, totalPages - 1))
                    else list.animateScrollToItem((page - 1).coerceIn(0, totalPages - 1))
                } finally {
                    lastReported = page
                    programmaticScroll = false
                }
            }
        }
        LaunchedEffect(list, zoomScale, panX, panY) {
            snapshotFlow {
                val visible = list.layoutInfo.visibleItemsInfo.firstOrNull()
                Triple(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset, visible?.size ?: 0)
            }
                .distinctUntilChanged()
                .collect { (index, offset, size) ->
                    if (programmaticScroll) return@collect
                    val actualPage = index + 1
                    lastReported = actualPage
                    val fraction = if (size > 0) (offset.toFloat() / size).coerceIn(0f, 0.999f) else 0f
                    onPageChanged(actualPage)
                    onViewportChanged(actualPage, fraction, zoomScale, panX, panY)
                }
        }
        LazyColumn(
            state = list,
            userScrollEnabled = zoomScale <= 1.01f,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(totalPages, key = { it }) { index ->
                DocumentPage(
                    publicationId = publicationId,
                    page = index + 1,
                    load = loadPage,
                    loadRegion = loadRegion,
                    onToggleChrome = onToggleChrome,
                    onNext = onNextPage,
                    onPrev = onPrevPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp),
                    resetToken = page,
                    fitMode = fitMode,
                    vertical = true,
                    startScale = if (index == page - 1) initialScale else 1f,
                    startPanX = if (index == page - 1) initialPanX else 0f,
                    startPanY = if (index == page - 1) initialPanY else 0f,
                    onZoomChanged = { scale, x, y -> if (index + 1 == page) { zoomScale = scale; panX = x; panY = y } }
                )
            }
        }
    } else {
        val spreads = remember(totalPages, mode, expanded, coverSingle) {
            ComicSpreads(totalPages, mode == ReaderLayoutMode.DUAL_PAGE, coverSingle)
        }
        val pager = androidx.compose.runtime.key(spreads) {
            rememberPagerState(initialPage = spreads.indexOf(page), pageCount = { spreads.count })
        }
        val scope = rememberCoroutineScope()
        var lastReported by remember(publicationId, spreads) { mutableIntStateOf(page) }
        LaunchedEffect(page, spreads) {
            if (page != lastReported) pager.scrollToPage(spreads.indexOf(page))
        }
        LaunchedEffect(pager, spreads, zoomScale, panX, panY) {
            snapshotFlow { pager.settledPage }.distinctUntilChanged().collect {
                val actualPage = spreads.firstPage(it)
                lastReported = actualPage
                onPageChanged(actualPage)
                onViewportChanged(actualPage, 0f, zoomScale, panX, panY)
            }
        }
        HorizontalPager(
            state = pager,
            userScrollEnabled = zoomScale <= 1.01f,
            reverseLayout = rightToLeft,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                for (number in spreads.pages(index).let { if (rightToLeft) it.reversed() else it }) {
                    if (number <= totalPages) {
                        DocumentPage(
                            publicationId = publicationId,
                            page = number,
                            load = loadPage,
                            loadRegion = loadRegion,
                            onToggleChrome = onToggleChrome,
                            onNext = {
                                scope.launch {
                                    if (pager.currentPage < pager.pageCount - 1) {
                                        if (reduceMotion) pager.scrollToPage(pager.currentPage + 1)
                                        else pager.animateScrollToPage(pager.currentPage + 1)
                                    } else {
                                        onNextPage()
                                    }
                                }
                            },
                            onPrev = {
                                scope.launch {
                                    if (pager.currentPage > 0) {
                                        if (reduceMotion) pager.scrollToPage(pager.currentPage - 1)
                                        else pager.animateScrollToPage(pager.currentPage - 1)
                                    } else {
                                        onPrevPage()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            resetToken = page,
                            fitMode = fitMode,
                            vertical = false,
                            startScale = if (number == page) initialScale else 1f,
                            startPanX = if (number == page) initialPanX else 0f,
                            startPanY = if (number == page) initialPanY else 0f,
                            onZoomChanged = { scale, x, y -> if (number == page) { zoomScale = scale; panX = x; panY = y } }
                        )
                    }
                }
            }
        }
    }
}

/** 单页绘制。加载新分辨率时保留旧图，避免每次缩放都闪一次转圈。 */
@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun DocumentPage(
    publicationId: String,
    page: Int,
    load: suspend (Int, Int) -> Bitmap,
    loadRegion: suspend (Int, Float, Float, Int) -> Bitmap?,
    onToggleChrome: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier,
    resetToken: Int,
    fitMode: String,
    vertical: Boolean,
    startScale: Float,
    startPanX: Float,
    startPanY: Float,
    onZoomChanged: (Float, Float, Float) -> Unit
) {
    var scale by remember(publicationId, page, resetToken) { mutableFloatStateOf(startScale.coerceIn(1f, 4f)) }
    var translation by remember(publicationId, page, resetToken) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(scale, translation, viewport) {
        val limitX = viewport.width * (scale - 1f) / 2f
        val limitY = viewport.height * (scale - 1f) / 2f
        onZoomChanged(scale, if (limitX > 0f) translation.x / limitX else 0f,
            if (limitY > 0f) translation.y / limitY else 0f)
    }

    // 缩放后的像素预算：放大越多，请求越高分辨率，避免位图被拉伸糊化
    val maxPixels = remember(scale, page) {
        when {
            scale >= 2.5f -> 6_000_000
            scale >= 1.5f -> 4_000_000
            else -> 2_500_000
        }
    }

    // 触点中心缩放 + 平移：缩放前后让触点下的像素保持不动。
    // 注意：这里不能用 detectTransformGestures —— 它会把单指拖动也一并消费掉，
    // 导致外层 LazyColumn 收不到滚动事件，条漫模式完全划不动。
    // 所以只在「双指」或「已经放大」时拦截手势，单指拖动在 1x 时交给滚动容器。
    val transformModifier = Modifier.pointerInput(viewport) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val pressedCount = event.changes.count { it.pressed }
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                val shouldHandle = pressedCount > 1 || scale > 1.01f
                if (shouldHandle && (zoomChange != 1f || panChange != Offset.Zero)) {
                    if (viewport.width == 0 || viewport.height == 0) break
                    val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                    val factor = newScale / scale
                    val centroid = event.calculateCentroid(useCurrent = true)
                    val center = Offset(viewport.width / 2f, viewport.height / 2f)
                    val c = centroid - center
                    var next = c - (c - translation) * factor + panChange
                    val limitX = viewport.width * (newScale - 1) / 2
                    val limitY = viewport.height * (newScale - 1) / 2
                    next = Offset(next.x.coerceIn(-limitX, limitX), next.y.coerceIn(-limitY, limitY))
                    if (newScale != scale || next != translation) {
                        scale = newScale
                        translation = if (newScale <= 1.01f) Offset.Zero else next
                    }
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }

    // 恢复缩放状态时同步还原平移比例
    LaunchedEffect(viewport, startScale) {
        if (viewport.width > 0 && scale > 1.01f && translation == Offset.Zero) {
            val limitX = viewport.width * (scale - 1) / 2
            val limitY = viewport.height * (scale - 1) / 2
            translation = Offset(startPanX * limitX, startPanY * limitY)
        }
    }

    var attempt by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    val result by produceState<Result<Bitmap>?>(null, publicationId, page, attempt, maxPixels, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                value = try {
                    Result.success(load(page, maxPixels))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Result.failure(IllegalStateException("无法读取页面"))
                }
                awaitCancellation()
            } finally {
                value = null
            }
        }
    }

    // 换页时保留上一张位图作占位：预取命中缓存后几乎立刻替换，不再闪全屏转圈。
    // 只有超过 220ms 还没加载出来，才在角落补一个轻量指示器。
    var shown by remember(publicationId, page) { mutableStateOf<Bitmap?>(null) }
    var slowLoad by remember(publicationId, page) { mutableStateOf(false) }
    LaunchedEffect(publicationId, page) {
        slowLoad = false
        delay(220)
        slowLoad = true
    }
    val ready = result?.getOrNull()
    LaunchedEffect(ready) { shown = ready }
    val displayed = ready ?: shown

    val fade by animateFloatAsState(
        targetValue = if (displayed != null) 1f else 0f,
        animationSpec = AsterMotion.effectsSpec(LocalAsterReduceMotion.current),
        label = "page-fade"
    )

    // 条漫模式：item 高度按图片真实比例撑开（而不是被最小高度截断成一条），
    // 这样长图才会连续滚动，短图也不会被拉伸变形。
    val pageRatio = remember(ready) {
        val b = ready
        if (b != null && b.width > 0 && b.height > 0) b.width.toFloat() / b.height else 0f
    }

    Box(
        modifier = modifier
            .then(if (vertical && pageRatio > 0f) Modifier.aspectRatio(pageRatio) else Modifier)
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .then(transformModifier)
            .pointerInput(scale, viewport) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.01f) {
                            scale = 1f
                            translation = Offset.Zero
                        } else {
                            scale = 2.2f
                            val centerX = viewport.width / 2f
                            val centerY = viewport.height / 2f
                            val offsetX = (centerX - tapOffset.x) * (scale - 1)
                            val offsetY = (centerY - tapOffset.y) * (scale - 1)
                            val limitX = viewport.width * (scale - 1) / 2
                            val limitY = viewport.height * (scale - 1) / 2
                            translation = Offset(offsetX.coerceIn(-limitX, limitX), offsetY.coerceIn(-limitY, limitY))
                        }
                    },
                    onTap = { tapOffset ->
                        val width = viewport.width.toFloat()
                        if (scale <= 1.01f && !vertical && width > 0) {
                            // 左右各留 25% 作为系统返回手势安全区之外才触发翻页
                            val edgeThreshold = width * 0.25f
                            when {
                                tapOffset.x < edgeThreshold -> onPrev()
                                tapOffset.x > (width - edgeThreshold) -> onNext()
                                else -> onToggleChrome()
                            }
                        } else {
                            onToggleChrome()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when {
            result?.isFailure == true -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第 $page 页无法读取，请检查文件")
                TextButton(onClick = { attempt++ }) { Text("重试") }
            }
            displayed != null -> {
                val bitmap = displayed!!
                val box = viewport
                val fitted = remember(box, bitmap.width, bitmap.height, vertical, fitMode) {
                    fitRect(box, bitmap.width, bitmap.height, vertical, fitMode == "原始比例")
                }
                // 垂直滚动的 item 有 heightIn(min = 320.dp)：短图会被外层 Box 居中，
                // 这里把绘制矩形同步下移，保证高清条带与整页位图严丝合缝。
                val regionRect = if (vertical && box.height > fitted.height) {
                    val dy = (box.height - fitted.height) / 2f
                    Rect(fitted.left, fitted.top + dy, fitted.right, fitted.bottom + dy)
                } else fitted
                val layer = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = fade
                        scaleX = scale
                        scaleY = scale
                        translationX = translation.x
                        translationY = translation.y
                    }

                // 放大时请求可见条带的原始分辨率，覆盖在整页位图之上
                val detail = rememberRegion(
                    loadRegion = loadRegion,
                    page = page,
                    scale = scale,
                    translation = translation,
                    box = box,
                    fitted = regionRect,
                    // 上下滚动（条漫，默认模式）同样支持放大后的高清条带，
                    // 之前只在翻页模式启用，导致默认阅读模式下长图放大仍然发糊。
                    enabled = box.width > 0
                )

                // 整页位图与高清条带叠加在同一坐标系：两者使用完全一致的
                // graphicsLayer（缩放 / 平移 / 淡入），避免放大后错位。
                Canvas(
                    modifier = layer.semantics {
                        contentDescription = "第 $page 页"
                        onClick("显示或隐藏阅读工具") { onToggleChrome(); true }
                        customActions = listOf(
                            CustomAccessibilityAction("上一页") { onPrev(); true },
                            CustomAccessibilityAction("下一页") { onNext(); true },
                            CustomAccessibilityAction("放大") { scale = (scale + 0.5f).coerceAtMost(4f); true },
                            CustomAccessibilityAction("缩小") { scale = (scale - 0.5f).coerceAtLeast(1f); translation = Offset.Zero; true }
                        )
                    }
                ) {
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(fitted.left.roundToInt(), fitted.top.roundToInt()),
                        dstSize = IntSize(fitted.width.roundToInt(), fitted.height.roundToInt()),
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.Low
                    )
                    detail?.let { band ->
                        drawImage(
                            image = band.bitmap.asImageBitmap(),
                            dstOffset = IntOffset(regionRect.left.roundToInt(), (regionRect.top + regionRect.height * band.top).roundToInt()),
                            dstSize = IntSize(
                                regionRect.width.roundToInt(),
                                (regionRect.height * (band.bottom - band.top)).roundToInt().coerceAtLeast(1)
                            ),
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.Low
                        )
                    }
                }
            }
            else -> CircularProgressIndicator()
        }
        if (displayed != null && slowLoad && ready == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(22.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

private data class DetailBand(val bitmap: Bitmap, val top: Float, val bottom: Float)

/**
 * 由当前缩放/平移反推「屏幕上可见的画面纵向范围」，节流后请求原始分辨率条带。
 * 条带两侧各留 50% 余量，小幅拖动不必重新解码。
 */
@Composable
private fun rememberRegion(
    loadRegion: suspend (Int, Float, Float, Int) -> Bitmap?,
    page: Int,
    scale: Float,
    translation: Offset,
    box: IntSize,
    fitted: Rect,
    enabled: Boolean
): DetailBand? {
    if (!enabled || scale < 1.6f || box.height <= 0 || fitted.height <= 0f) return null
    val zoom = (scale * 4).roundToInt() / 4f
    val center = box.height / 2f
    val topPx = center + (0f - center - translation.y) / zoom
    val bottomPx = center + (box.height - center - translation.y) / zoom
    val rawTop = ((topPx - fitted.top) / fitted.height)
    val rawBottom = ((bottomPx - fitted.top) / fitted.height)
    val margin = (rawBottom - rawTop) * 0.5f
    val bandTop = (rawTop - margin).coerceIn(0f, 0.99f)
    val bandBottom = (rawBottom + margin).coerceIn(bandTop + 0.01f, 1f)
    if (bandBottom - bandTop > 0.92f) return null

    // 与 DocumentRenderSession 的 1/40 量化保持一致，避免同一视口反复解码
    val topKey = (bandTop * 40).roundToInt()
    val bottomKey = (bandBottom * 40).roundToInt()
    val quantizedTop = (topKey / 40f).coerceIn(0f, 0.975f)
    val quantizedBottom = (bottomKey / 40f).coerceIn(quantizedTop + 0.025f, 1f)
    val owner = LocalLifecycleOwner.current
    val state = produceState<DetailBand?>(null, page, zoom, topKey, bottomKey, box.width, box.height, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        try {
        delay(180)
        val height = ((quantizedBottom - quantizedTop) * fitted.height * zoom).roundToInt().coerceAtLeast(1)
        val pixels = (box.width * height * 1.6f).roundToInt().coerceIn(1_500_000, 12_000_000)
        value = try {
            loadRegion(page, quantizedTop, quantizedBottom, pixels)
                ?.let { DetailBand(it, quantizedTop, quantizedBottom) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { null }
        awaitCancellation()
        } finally { value = null }
        }
    }
    return state.value
}

/** 与 ContentScale.Fit 一致的绘制矩形；[natural] 为真时按 1:1 像素绘制。 */
private fun fitRect(
    box: IntSize,
    imageWidth: Int,
    imageHeight: Int,
    vertical: Boolean,
    natural: Boolean = false
): Rect {
    if (box.width <= 0 || box.height <= 0 || imageWidth <= 0 || imageHeight <= 0) return Rect.Zero
    if (natural) {
        val left = (box.width - imageWidth) / 2f
        val top = (box.height - imageHeight) / 2f
        return Rect(left, top, left + imageWidth, top + imageHeight)
    }
    if (vertical) {
        // 条漫按宽度铺满，高度由图片比例决定
        val height = box.width.toFloat() * imageHeight / imageWidth
        return Rect(0f, 0f, box.width.toFloat(), height)
    }
    val scale = minOf(box.width.toFloat() / imageWidth, box.height.toFloat() / imageHeight)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return Rect(
        left = (box.width - width) / 2f,
        top = (box.height - height) / 2f,
        right = (box.width + width) / 2f,
        bottom = (box.height + height) / 2f
    )
}
