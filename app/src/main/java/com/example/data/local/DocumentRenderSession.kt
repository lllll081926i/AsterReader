package com.example.data.local

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

/** 归档或目录页的打开方式；安全来源由 Repository 完成授权。 */
internal sealed interface PageSource {
    /** [owned] 为 true 时会话关闭时删除该文件；复用的本地副本必须传 false。 */
    class Document(val file: File, val format: String, val owned: Boolean = true, val release: () -> Unit = {}) : PageSource
    class Directory(val pageCount: Int, val openPage: (Int) -> InputStream?) : PageSource
    class SecurePdf(val descriptor: ParcelFileDescriptor, val release: () -> Unit) : PageSource
    class SecureArchive(val archive: ComicArchive) : PageSource
    class Archive(val archive: ComicArchive) : PageSource
}

/**
 * 普通文档会话。
 *
 * 线程模型：
 * - [sourceMutex] 只负责来源的打开 / 切换 / 关闭，解码过程不持锁；
 * - 每个来源自带 [OpenSource.gate]，ZIP 与目录可并发取页（预取不会阻塞当前页），
 *   7z / RAR / PDF 的真实随机读不支持并发，退化为串行；
 * - [close] 会等待在途解码结束，避免关闭归档时另一个线程还在读。
 */
internal class DocumentRenderSession(context: Context, cacheBudgetBytes: Int = budget(context)) {
    data class Index(val pageCount: Int, val pageNames: List<String>)

    suspend fun index(key: String, prepare: suspend () -> PageSource): Index {
        val open = ensureSource(key, prepare)
        return try { Index(open.pageCount, open.archive?.pageNames.orEmpty()) }
        finally { inFlight.update { it - 1 } }
    }
    private val sourceMutex = Mutex()
    private data class PageKey(val source: String, val page: Int, val pixels: Int, val top: Int = -1, val bottom: Int = -1) {
        fun sameArea(other: PageKey) = source == other.source && page == other.page && top == other.top && bottom == other.bottom
    }
    private val pages = object : LruCache<PageKey, Bitmap>(cacheBudgetBytes) {
        override fun sizeOf(key: PageKey, value: Bitmap) = value.allocationByteCount
    }
    @Volatile private var source: OpenSource? = null
    private val dimensions = java.util.Collections.synchronizedMap(java.util.WeakHashMap<Bitmap, Pair<Int, Int>>())
    fun nativeSize(bitmap: Bitmap): Pair<Int, Int>? = dimensions[bitmap]
    private val inFlight = MutableStateFlow(0)
    private val pageGates = Array(32) { Mutex() }
    private val decodeSlots = Semaphore(2)
    private val foregroundRequests = MutableStateFlow(0)
    @Volatile private var foregroundPage: Pair<String, Int>? = null

    fun cachedPageSize(key: String, page: Int): Pair<Int, Int>? =
        source?.takeIf { it.key == key }?.imageSizes?.get(page)

    /** 仅读取已解码内存，不打开来源；供页面首帧复用预加载结果。 */
    fun cachedPage(key: String, page: Int, maxPixels: Int): Bitmap? {
        val open = source?.takeIf { it.key == key } ?: return null
        return cachedPage(PageKey(open.fingerprint, page, maxPixels))?.also { foregroundPage = key to page }
    }

    suspend fun render(
        key: String,
        page: Int,
        maxPixels: Int,
        prepare: suspend () -> PageSource
    ): Bitmap {
        foregroundPage = key to page
        foregroundRequests.update { it + 1 }
        try {
        val open = ensureSource(key, prepare)
        try {
        val cacheKey = PageKey(open.fingerprint, page, maxPixels)
        return cachedDecode(open, cacheKey) { renderBitmap(open, page, maxPixels) }!!
        } finally { inFlight.update { it - 1 } }
        } finally { foregroundRequests.update { it - 1 } }
    }

    /**
     * 预热相邻页：只填缓存，不返回位图，失败静默。
     * 与 [render] 共用缓存，因此翻页时直接命中。
     */
    suspend fun prefetch(
        key: String,
        pagesToLoad: List<Int>,
        maxPixels: Int,
        prepare: suspend () -> PageSource
    ) {
        var prefetchedBytes = 0L
        for (page in pagesToLoad.distinct()) {
            if (page < 1) continue
            while (true) {
                currentCoroutineContext().ensureActive()
                // 等待时不持有来源租约，换书不会与可见页互相等待。
                foregroundRequests.first { it == 0 }
                val open = ensureSource(key, prepare)
                try {
                    if (foregroundRequests.value > 0) continue
                    if (page > open.pageCount) break
                    val cacheKey = PageKey(open.fingerprint, page, maxPixels)
                    try {
                        val bitmap = cachedDecode(open, cacheKey, background = true) { renderBitmap(open, page, maxPixels) }
                        if (bitmap == null) continue
                        prefetchedBytes += bitmap.allocationByteCount
                        // 不为远处预加载淘汰当前页；缓存不能容纳时结束本轮。
                        if (cachedPage(cacheKey) == null) return
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { /* 可见页负责显示读取错误。 */ }
                    break
                } finally { inFlight.update { it - 1 } }
            }
            if (prefetchedBytes >= pages.maxSize() * 3L / 4) break
        }
    }

    /**
     * 只解码可见纵向条带，供放大状态使用：长条漫放大后按视口取原始分辨率，
     * 而不是把整张长图缩成一个小位图再拉伸。
     *
     * @param topFraction 条带顶端在整页中的比例
     * @param bottomFraction 条带底端比例
     * @return 条带位图；来源无法区域解码时返回 null，调用方退回整页位图。
     */
    suspend fun renderRegion(
        key: String,
        page: Int,
        topFraction: Float,
        bottomFraction: Float,
        maxPixels: Int,
        prepare: suspend () -> PageSource
    ): Bitmap? {
        foregroundRequests.update { it + 1 }
        try {
        val open = ensureSource(key, prepare)
        try {
        val top = topFraction.coerceIn(0f, 0.999f)
        val bottom = bottomFraction.coerceIn(top + 0.001f, 1f)
        // 量化到 1/40 档：轻微滚动不应产生新 key，否则缓存永不命中、
        // 还会不断把整页位图挤出 LRU（放大反而更卡）。
        val topKey = (top * 40).roundToInt()
        val bottomKey = (bottom * 40).roundToInt()
        val cacheKey = PageKey(open.fingerprint, page, maxPixels, topKey, bottomKey)
        return try {
            cachedDecode(open, cacheKey) { renderRegionBitmap(open, page, top, bottom, maxPixels) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) { pages.evictAll(); null }
        catch (_: Exception) { null }
        } finally { inFlight.update { it - 1 } }
        } finally { foregroundRequests.update { it - 1 } }
    }

    private suspend fun ensureSource(key: String, prepare: suspend () -> PageSource): OpenSource {
        currentCoroutineContext().ensureActive()
        sourceMutex.withLock {
            source?.let { if (it.key == key) { inFlight.update { count -> count + 1 }; return it } }
            inFlight.first { it == 0 }
            closeLocked()
            val opened = openSource(key, prepare())
            source = opened
            inFlight.update { it + 1 }
            return opened
        }
    }

    private suspend fun openSource(key: String, prepared: PageSource): OpenSource {
        var temporaryFile: File? = null
        try {
            return when (prepared) {
                is PageSource.Archive -> OpenSource(key, "archive:$key", prepared.archive.pageCount,
                    prepared.archive, null, null, null, concurrent = prepared.archive.concurrentReads)
                is PageSource.SecureArchive -> OpenSource(key, "secure:$key", prepared.archive.pageCount,
                    prepared.archive, null, null, null, concurrent = false)
                is PageSource.SecurePdf -> {
                    val renderer = try { PdfRenderer(prepared.descriptor) }
                    catch (failure: Throwable) { prepared.descriptor.close(); prepared.release(); throw failure }
                    OpenSource(key, "secure:$key", renderer.pageCount, null, renderer, null, null,
                        concurrent = false, secureRelease = prepared.release)
                }
                is PageSource.Document -> {
                    if (prepared.owned) temporaryFile = prepared.file
                    val fingerprint = fingerprintOf(prepared.file)
                    when {
                        prepared.format == "PDF" -> {
                            val descriptor = ParcelFileDescriptor.open(
                                prepared.file, ParcelFileDescriptor.MODE_READ_ONLY
                            )
                            val renderer = try {
                                PdfRenderer(descriptor)
                            } catch (failure: Throwable) {
                                descriptor.close()
                                throw failure
                            }
                            OpenSource(
                                key, fingerprint, renderer.pageCount, null, renderer, null,
                                temporaryFile, concurrent = false, secureRelease = prepared.release
                            )
                        }
                        prepared.format == "IMAGE_DIR" -> throw IOException("目录作品必须通过目录来源打开")
                        else -> {
                            val archive = ComicArchives.open(prepared.file, prepared.format)
                            OpenSource(
                                key, fingerprint, archive.pageCount, archive, null, null,
                                temporaryFile, concurrent = archive.concurrentReads, secureRelease = prepared.release
                            )
                        }
                    }
                }
                is PageSource.Directory -> OpenSource(
                    key,
                    "dir:${key.hashCode().toUInt().toString(16)}:${prepared.pageCount}",
                    prepared.pageCount, null, null, prepared, null, concurrent = true
                )
            }
        } catch (failure: Throwable) {
            // 打开失败时 source 还没赋值，closeLocked() 不会回收，必须在这里删掉自己持有的副本
            closeLocked()
            temporaryFile?.delete()
            if (prepared is PageSource.Document) prepared.release()
            throw failure
        }
    }

    private fun cachedPage(key: PageKey): Bitmap? {
        pages.get(key)?.let { return it }
        val compatible = pages.snapshot().keys.filter { it.sameArea(key) && it.pixels >= key.pixels }
            .minByOrNull { it.pixels } ?: return null
        return pages.get(compatible)
    }

    private suspend fun cachedDecode(open: OpenSource, key: PageKey, background: Boolean = false, block: () -> Bitmap?): Bitmap? {
        cachedPage(key)?.let { return it }
        // 同一页的不同清晰度共用门锁，高清完成后预览直接复用，不再排队重复解码。
        val gate = (31 * key.source.hashCode() + key.page).and(Int.MAX_VALUE) % pageGates.size
        return pageGates[gate].withLock {
            cachedPage(key) ?: decode(open, background) {
                block()?.also { bitmap ->
                    bitmap.prepareToDraw()
                    if (bitmap.allocationByteCount <= pages.maxSize()) {
                        synchronized(pages) {
                            val anchor = foregroundPage?.takeIf { it.first == open.key }?.second
                            if (background && anchor != null) {
                                val snapshot = pages.snapshot()
                                val required = bitmap.allocationByteCount - (pages.maxSize() - pages.size())
                                val candidates = snapshot.keys.filter {
                                    kotlin.math.abs(it.page.toLong() - anchor) > kotlin.math.abs(key.page.toLong() - anchor)
                                }.sortedByDescending { kotlin.math.abs(it.page.toLong() - anchor) }
                                if (candidates.sumOf { snapshot.getValue(it).allocationByteCount.toLong() } < required) return@also
                                var freed = 0L
                                for (candidate in candidates) {
                                    if (freed >= required) break
                                    freed += pages.remove(candidate)?.allocationByteCount ?: 0
                                }
                            }
                            pages.snapshot().keys.filter { it.sameArea(key) && it.pixels < key.pixels }.forEach(pages::remove)
                            pages.put(key, bitmap)
                        }
                    }
                }
            }
        }
    }

    private suspend fun decode(open: OpenSource, background: Boolean, block: () -> Bitmap?): Bitmap? {
        suspend fun admitted(): Bitmap? = withContext(Dispatchers.IO) {
            currentCoroutineContext().ensureActive()
            if (background && foregroundRequests.value > 0) null else block()
        }
        return if (open.concurrent) decodeSlots.withPermit { admitted() }
            else open.gate.withLock { admitted() }
    }

    private fun renderBitmap(open: OpenSource, page: Int, maxPixels: Int): Bitmap = try {
        if (open.renderer != null) renderPdf(open, page, maxPixels) else renderImage(open, page, maxPixels)
    } catch (_: OutOfMemoryError) {
        pages.evictAll()
        try {
            if (open.renderer != null) renderPdf(open, page, maxPixels / 4)
            else renderImage(open, page, maxPixels / 4)
        } catch (_: OutOfMemoryError) {
            throw IOException("可用内存不足，请关闭其他应用后重试")
        }
    }

    private fun renderPdf(open: OpenSource, number: Int, maxPixels: Int): Bitmap =
        open.renderer!!.openPage(number - 1).use { page ->
            open.imageSizes[number] = page.width to page.height
            val nativePixels = page.width.toLong() * page.height
            val scale = kotlin.math.sqrt(maxPixels.toDouble() / nativePixels).toFloat().coerceAtMost(3f)
            val width = (page.width * scale).toInt().coerceIn(1, page.width * 3)
            val height = (page.height * scale).toInt().coerceIn(1, page.height * 3)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                dimensions[bitmap] = page.width to page.height
                bitmap.eraseColor(android.graphics.Color.WHITE)
                try {
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } catch (failure: Throwable) {
                    bitmap.recycle(); throw failure
                }
            }
        }

    private fun renderImage(open: OpenSource, number: Int, maxPixels: Int): Bitmap {
        fun stream() = java.io.BufferedInputStream(openStream(open, number), 64 * 1024)
        var input = stream()
        try {
            val original = open.imageSizes[number] ?: run {
                // 常规图片只打开一次条目；超长文件头超出缓冲时才重新打开像素流。
                input.mark(64 * 1024)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "图像已损坏" }
                try { input.reset() } catch (_: IOException) { input.close(); input = stream() }
                (bounds.outWidth to bounds.outHeight).also { open.imageSizes[number] = it }
            }
            return (BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                inSampleSize = sampleFor(original.first, original.second, maxPixels)
            }) ?: error("图像无法解码")).also { dimensions[it] = original }
        } finally { input.close() }
    }

    private fun renderRegionBitmap(
        open: OpenSource,
        number: Int,
        top: Float,
        bottom: Float,
        maxPixels: Int
    ): Bitmap? {
        if (open.renderer != null) {
            return open.renderer.openPage(number - 1).use { page ->
                val pageWidth = page.width
                val pageHeight = page.height
                val bandTop = (pageHeight * top).toInt()
                val bandBottom = (pageHeight * bottom).toInt().coerceAtLeast(bandTop + 1)
                val bandHeight = bandBottom - bandTop
                val pixels = pageWidth.toLong() * bandHeight
                val scale = kotlin.math.sqrt(maxPixels.toDouble() / pixels).toFloat().coerceAtMost(3f)
                val width = (pageWidth * scale).toInt().coerceAtLeast(1)
                val height = (bandHeight * scale).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    val matrix = Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(0f, -bandTop * scale)
                    }
                    try {
                        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    } catch (failure: Throwable) {
                        bitmap.recycle(); throw failure
                    }
                }
            }
        }

        return synchronized(open.regionGate) {
            if (open.regionPage != number) {
                open.regionDecoder?.recycle()
                open.regionDecoder = null
                open.regionPage = -1
            }
            val decoder = open.regionDecoder ?: openStream(open, number).use {
                BitmapRegionDecoder.newInstance(it, false)
            }?.also { open.regionDecoder = it; open.regionPage = number } ?: return@synchronized null
            val width = decoder.width
            val height = decoder.height
            val rect = Rect(0, (height * top).toInt(), width, (height * bottom).toInt().coerceAtLeast(1))
            val sample = sampleFor(rect.width(), rect.height(), maxPixels)
            decoder.decodeRegion(rect, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    /**
     * 严格按像素预算选采样率，绝不允许为了「保住最小宽度」而突破预算：
     * 极长条漫（如 1000×100000）曾因宽度下限被钳制而解码出 25M 像素。
     * 整页位图只是占位，放大后的清晰度由 renderRegion 的高清条带补齐。
     */
    private fun sampleFor(width: Int, height: Int, maxPixels: Int): Int {
        var sample = 1
        while (sample < (1 shl 30)) {
            val pixels = (width / sample).toLong() * (height / sample)
            if (pixels <= maxPixels.coerceAtLeast(1)) break
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun openStream(open: OpenSource, page: Int): InputStream {
        open.archive?.let { return it.openPage(page) }
        open.directory?.let { source -> return source.openPage(page) ?: throw IOException("页面来源已失效") }
        throw IOException("该作品没有可读取的图片页")
    }

    fun trimMemory() { pages.evictAll() }

    /** 关闭会话：拒绝新解码，等在途解码结束，再释放归档与临时文件；之后仍可重新打开。 */
    suspend fun close(expectedKey: String? = null) = withContext(Dispatchers.IO) {
        sourceMutex.withLock {
            if (expectedKey != null && source?.key != expectedKey) return@withLock
            withContext(kotlinx.coroutines.NonCancellable) {
                inFlight.first { it == 0 }
                closeLocked()
            }
        }
    }

    internal fun cachedBytes() = pages.size()
    internal fun maxCacheBytes() = pages.maxSize()

    private fun closeLocked() {
        val open = source ?: return
        source = null
        pages.evictAll()
        synchronized(open.regionGate) {
            open.regionDecoder?.recycle()
            open.regionDecoder = null
        }
        try {
            open.renderer?.close()
        } finally {
            try {
                open.archive?.close()
            } finally {
                try { open.temporaryFile?.delete() } finally { open.secureRelease?.invoke() }
            }
        }
    }

    private fun fingerprintOf(file: File): String = java.io.RandomAccessFile(file, "r").use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        val head = input.read(buffer)
        if (head > 0) digest.update(buffer, 0, head)
        if (input.length() > buffer.size) {
            input.seek((input.length() - buffer.size).coerceAtLeast(0))
            val tail = input.read(buffer)
            if (tail > 0) digest.update(buffer, 0, tail)
        }
        digest.update(input.length().toString().toByteArray())
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class OpenSource(
        val key: String,
        val fingerprint: String,
        val pageCount: Int,
        val archive: ComicArchive?,
        val renderer: PdfRenderer?,
        val directory: PageSource.Directory?,
        val temporaryFile: File?,
        val concurrent: Boolean,
        val secureRelease: (() -> Unit)? = null
    ) {
        val gate = Mutex()
        val imageSizes = java.util.concurrent.ConcurrentHashMap<Int, Pair<Int, Int>>()
        val regionGate = Any()
        var regionPage = -1
        var regionDecoder: BitmapRegionDecoder? = null
    }

    companion object {
        private fun budget(context: Context): Int = minOf(
            96L * 1024 * 1024,
            Runtime.getRuntime().maxMemory() / 6,
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .memoryClass.toLong() * 1024 * 1024 / 6
        ).coerceAtLeast(4L * 1024 * 1024).toInt()
    }
}
