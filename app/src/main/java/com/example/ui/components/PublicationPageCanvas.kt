package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.ReaderLayoutMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 升级版漫画渲染画布：
 * 1. P0: 记录条漫页内像素偏移量与缩放状态；
 * 2. P0: 触点中心缩放、双击缩放、图片边缘翻页与系统返回手势避让；
 * 3. P0: 双页对开独立排版。
 */
@Composable
fun PublicationPageCanvas(
    publicationId: String,
    page: Int,
    totalPages: Int,
    mode: ReaderLayoutMode,
    expanded: Boolean,
    coverSingle: Boolean = true,
    fitMode: String = "适合屏幕",
    initialScrollOffset: Int = 0,
    loadPage: suspend (Int) -> Bitmap,
    onPageChanged: (Int) -> Unit,
    onScrollOffsetChanged: (Int, Int) -> Unit = { _, _ -> },
    onNextPage: () -> Unit = {},
    onPrevPage: () -> Unit = {},
    onToggleChrome: () -> Unit
) {
    var zoomed by remember(publicationId) { mutableStateOf(false) }
    LaunchedEffect(page, mode) { zoomed = false }

    if (mode == ReaderLayoutMode.VERTICAL_SCROLL) {
        val list = rememberLazyListState(
            initialFirstVisibleItemIndex = (page - 1).coerceIn(0, totalPages - 1),
            initialFirstVisibleItemScrollOffset = initialScrollOffset
        )
        var lastReported by remember(publicationId) { mutableIntStateOf(page) }
        LaunchedEffect(page) {
            if (page != lastReported) list.scrollToItem(page - 1)
        }
        LaunchedEffect(list) {
            snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .collect { (idx, offset) ->
                    val actualPage = idx + 1
                    lastReported = actualPage
                    onPageChanged(actualPage)
                    onScrollOffsetChanged(actualPage, offset)
                }
        }
        LazyColumn(
            state = list,
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(totalPages, key = { it }) { index ->
                DocumentPage(
                    publicationId = publicationId,
                    page = index + 1,
                    load = loadPage,
                    onToggleChrome = onToggleChrome,
                    onNext = onNextPage,
                    onPrev = onPrevPage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 400.dp),
                    resetToken = page,
                    fitMode = fitMode,
                    vertical = true
                ) { zoomed = it }
            }
        }
    } else {
        val spreads = remember(totalPages, mode, expanded, coverSingle) {
            ComicSpreads(totalPages, mode == ReaderLayoutMode.DUAL_PAGE && expanded, coverSingle)
        }
        val pager = androidx.compose.runtime.key(spreads) {
            rememberPagerState(initialPage = spreads.indexOf(page), pageCount = { spreads.count })
        }
        val scope = rememberCoroutineScope()
        var lastReported by remember(publicationId, spreads) { mutableIntStateOf(page) }
        LaunchedEffect(page, spreads) {
            if (page != lastReported) pager.scrollToPage(spreads.indexOf(page))
        }
        LaunchedEffect(pager, spreads) {
            snapshotFlow { pager.settledPage }.distinctUntilChanged().collect {
                val actualPage = spreads.firstPage(it)
                lastReported = actualPage
                onPageChanged(actualPage)
            }
        }
        HorizontalPager(
            state = pager,
            userScrollEnabled = !zoomed,
            reverseLayout = mode == ReaderLayoutMode.RTL_PAGER,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                for (number in spreads.pages(index)) {
                    if (number <= totalPages) {
                        DocumentPage(
                            publicationId = publicationId,
                            page = number,
                            load = loadPage,
                            onToggleChrome = onToggleChrome,
                            onNext = {
                                scope.launch {
                                    if (pager.currentPage < pager.pageCount - 1) {
                                        pager.animateScrollToPage(pager.currentPage + 1)
                                    } else {
                                        onNextPage()
                                    }
                                }
                            },
                            onPrev = {
                                scope.launch {
                                    if (pager.currentPage > 0) {
                                        pager.animateScrollToPage(pager.currentPage - 1)
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
                            vertical = false
                        ) { zoomed = it }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun DocumentPage(
    publicationId: String,
    page: Int,
    load: suspend (Int) -> Bitmap,
    onToggleChrome: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier,
    resetToken: Int,
    fitMode: String,
    vertical: Boolean,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember(publicationId, page, resetToken) { mutableFloatStateOf(1f) }
    var translation by remember(publicationId, page, resetToken) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    // 触点中心缩放手势处理器
    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4.5f)
        val limitX = viewport.width * (scale - 1) / 2
        val limitY = viewport.height * (scale - 1) / 2
        translation = Offset(
            (translation.x + panChange.x).coerceIn(-limitX, limitX),
            (translation.y + panChange.y).coerceIn(-limitY, limitY)
        )
        onZoomChanged(scale > 1.01f)
    }

    var attempt by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    val result by produceState<Result<Bitmap>?>(null, publicationId, page, attempt, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                value = try {
                    Result.success(load(page))
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

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .transformable(state = transform, canPan = { scale > 1.01f })
            .pointerInput(scale, viewport) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        // 触点中心双击缩放
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
                        onZoomChanged(scale > 1.01f)
                    },
                    onTap = { tapOffset ->
                        val width = viewport.width.toFloat()
                        // 系统返回手势安全避让区：仅屏幕中间 20%~80% 区域触发左右边缘翻页与菜单
                        if (scale <= 1.01f && !vertical) {
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
            }
            .padding(vertical = if (vertical) 2.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            result == null -> CircularProgressIndicator()
            result!!.isSuccess -> Image(
                bitmap = result!!.getOrThrow().asImageBitmap(),
                contentDescription = "第 $page 页",
                modifier = (if (vertical) Modifier.fillMaxWidth() else Modifier.fillMaxSize()).graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = translation.x
                    translationY = translation.y
                },
                contentScale = if (fitMode == "原始比例") ContentScale.Inside else ContentScale.Fit
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第 $page 页无法读取，请检查文件")
                TextButton(onClick = { attempt++ }) { Text("重试") }
            }
        }
    }
}
