package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.ui.ReaderLayoutMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PublicationPageCanvas(
    publicationId: String,
    page: Int,
    totalPages: Int,
    mode: ReaderLayoutMode,
    expanded: Boolean,
    coverSingle: Boolean = true,
    fitMode: String = "适合屏幕",
    loadPage: suspend (Int) -> Bitmap,
    onPageChanged: (Int) -> Unit,
    onToggleChrome: () -> Unit
) {
    var zoomed by remember(publicationId) { mutableStateOf(false) }
    LaunchedEffect(page, mode) { zoomed = false }
    if (mode == ReaderLayoutMode.VERTICAL_SCROLL) {
        val list = rememberLazyListState(initialFirstVisibleItemIndex = (page - 1).coerceIn(0, totalPages - 1))
        var lastReported by remember(publicationId) { mutableIntStateOf(page) }
        LaunchedEffect(page) {
            if (page != lastReported) list.scrollToItem(page - 1)
        }
        LaunchedEffect(list) {
            snapshotFlow { list.firstVisibleItemIndex + 1 }.distinctUntilChanged().collect {
                lastReported = it
                onPageChanged(it)
            }
        }
        LazyColumn(state = list, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(totalPages, key = { it }) { index ->
                DocumentPage(publicationId, index + 1, loadPage, onToggleChrome,
                    Modifier.fillMaxWidth().heightIn(min = 400.dp), page, fitMode, true) { zoomed = it }
            }
        }
    } else {
        val spreads = remember(totalPages, mode, expanded, coverSingle) {
            ComicSpreads(totalPages, mode == ReaderLayoutMode.DUAL_PAGE && expanded, coverSingle)
        }
        val pager = androidx.compose.runtime.key(spreads) {
            rememberPagerState(initialPage = spreads.indexOf(page), pageCount = { spreads.count })
        }
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
        HorizontalPager(pager, userScrollEnabled = !zoomed, reverseLayout = mode == ReaderLayoutMode.RTL_PAGER, modifier = Modifier.fillMaxSize()) { index ->
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                for (number in spreads.pages(index)) {
                    if (number <= totalPages) DocumentPage(publicationId, number, loadPage, onToggleChrome,
                        Modifier.weight(1f).fillMaxSize(), page, fitMode, false) { zoomed = it }
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
    modifier: Modifier,
    resetToken: Int,
    fitMode: String,
    vertical: Boolean,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember(publicationId, page, resetToken) { mutableFloatStateOf(1f) }
    var translation by remember(publicationId, page, resetToken) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 4f)
        val limitX = viewport.width * (scale - 1) / 2
        val limitY = viewport.height * (scale - 1) / 2
        translation = Offset((translation.x + pan.x).coerceIn(-limitX, limitX),
            (translation.y + pan.y).coerceIn(-limitY, limitY))
        onZoomChanged(scale > 1.01f)
    }
    var attempt by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    val result by produceState<Result<Bitmap>?>(null, publicationId, page, attempt, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                value = try { Result.success(load(page)) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { Result.failure(IllegalStateException("无法读取页面")) }
                awaitCancellation()
            } finally { value = null }
        }
    }
    Box(modifier.clipToBounds().onSizeChanged { viewport = it }
        .transformable(state = transform, canPan = { scale > 1.01f })
        .combinedClickable(onClick = onToggleChrome, onDoubleClick = {
            scale = if (scale > 1.01f) 1f else 2f
            translation = Offset.Zero
            onZoomChanged(scale > 1.01f)
        }).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        when {
            result == null -> CircularProgressIndicator()
            result!!.isSuccess -> Image(result!!.getOrThrow().asImageBitmap(), "第 $page 页",
                modifier = (if (vertical) Modifier.fillMaxWidth() else Modifier.fillMaxSize()).graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = translation.x; translationY = translation.y
                }, contentScale = if (fitMode == "原始比例") ContentScale.Inside else ContentScale.Fit)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第 $page 页无法读取，请检查文件或重新授权")
                TextButton(onClick = { attempt++ }) { Text("重试") }
            }
        }
    }
}
