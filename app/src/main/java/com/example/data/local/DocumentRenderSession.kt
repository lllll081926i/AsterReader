package com.example.data.local

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile

/** 普通文档会话。受保护文件在进入此处前必须被拒绝。 */
internal class DocumentRenderSession(context: Context) {
    private val mutex = Mutex()
    private val budget = minOf(24L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 10,
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass.toLong() * 1024 * 1024 / 10)
        .coerceAtLeast(2L * 1024 * 1024).toInt()
    private val pages = object : LruCache<String, Bitmap>(budget) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private var sourceKey: String? = null
    private var fingerprint = ""
    private var sourceFile: File? = null
    private var archive: ZipFile? = null
    private var renderer: PdfRenderer? = null
    private var entries: List<String> = emptyList()

    suspend fun render(
        book: BookEntity,
        page: Int,
        key: String,
        copySource: suspend () -> File,
        listImages: (ZipFile) -> List<String>
    ): Bitmap = withContext(Dispatchers.IO) {
        require(!book.isVaultProtected && !book.isVaultEncrypted)
        mutex.withLock {
            if (sourceKey != key) {
                closeLocked()
                val file = copySource()
                sourceFile = file
                try {
                    fingerprint = java.io.RandomAccessFile(file, "r").use { source ->
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(64 * 1024)
                        val head = source.read(buffer)
                        if (head > 0) digest.update(buffer, 0, head)
                        if (source.length() > buffer.size) {
                            source.seek((source.length() - buffer.size).coerceAtLeast(0))
                            val tail = source.read(buffer)
                            if (tail > 0) digest.update(buffer, 0, tail)
                        }
                        digest.update(source.length().toString().toByteArray())
                        digest.digest().joinToString("") { "%02x".format(it) }
                    }
                    if (book.format == "PDF") {
                        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        try { renderer = PdfRenderer(descriptor) }
                        catch (failure: Throwable) { descriptor.close(); throw failure }
                    } else {
                        // 支持所有压缩及漫画归档格式：CBZ, ZIP, CBR, RAR, CB7, 7Z, CBT, TAR, EPUB, MOBI
                        val zip = ZipFile(file)
                        archive = zip
                        entries = listImages(zip)
                    }
                    sourceKey = key
                } catch (failure: Throwable) { closeLocked(); throw failure }
            }
            val cacheKey = "$fingerprint:${book.format}:$page"
            pages.get(cacheKey)?.let { return@withLock it }
            currentCoroutineContext().ensureActive()
            val bitmap = try {
                if (renderer != null) renderPdf(page) else renderImage(page)
            } catch (_: OutOfMemoryError) {
                pages.evictAll()
                try {
                    if (renderer != null) renderPdf(page, 0.5f) else renderImage(page, 500_000)
                } catch (_: OutOfMemoryError) { throw java.io.IOException("可用内存不足，请关闭其他应用后重试") }
            }
            currentCoroutineContext().ensureActive()
            if (bitmap.allocationByteCount <= budget) pages.put(cacheKey, bitmap)
            bitmap
        }
    }

    private fun renderPdf(number: Int, quality: Float = 1f): Bitmap = renderer!!.openPage(number - 1).use { page ->
        val scale = minOf(1200f / page.width, 1600f / page.height, 2f) * quality
        Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1),
            (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(android.graphics.Color.WHITE)
            try { page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
            catch (failure: Throwable) { bitmap.recycle(); throw failure }
        }
    }

    private fun renderImage(number: Int, maxPixels: Int = 2_000_000): Bitmap {
        val zip = archive!!
        val entry = zip.getEntry(entries.getOrNull(number - 1) ?: error("页面不存在")) ?: error("页面不存在")
        require(entry.size in 1..MAX_IMAGE_BYTES) { "图像资源过大或尺寸未知" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        zip.getInputStream(entry).use { BitmapFactory.decodeStream(LimitedStream(it), null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "图像已损坏" }
        var sample = 1
        while (bounds.outWidth.toLong() / sample * (bounds.outHeight / sample) > maxPixels) sample *= 2
        return zip.getInputStream(entry).use {
            BitmapFactory.decodeStream(LimitedStream(it), null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("图像无法解码")
    }

    fun trimMemory() { pages.evictAll() }

    suspend fun close() = withContext(Dispatchers.IO) { mutex.withLock { closeLocked() } }

    internal fun cachedBytes() = pages.size()
    internal fun maxCacheBytes() = budget

    private fun closeLocked() {
        pages.evictAll()
        try { renderer?.close() } finally {
            renderer = null
            try { archive?.close() } finally {
                archive = null
                sourceFile?.delete()
                sourceFile = null
                sourceKey = null
                fingerprint = ""
                entries = emptyList()
            }
        }
    }

    private class LimitedStream(input: InputStream) : FilterInputStream(input) {
        private var remaining = MAX_IMAGE_BYTES
        override fun read(): Int {
            check(remaining > 0) { "图像资源超出限制" }
            return super.read().also { if (it >= 0) remaining-- }
        }
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            check(remaining > 0) { "图像资源超出限制" }
            return `in`.read(bytes, offset, minOf(length.toLong(), remaining).toInt())
                .also { if (it > 0) remaining -= it }
        }
    }

    companion object { private const val MAX_IMAGE_BYTES = 32L * 1024 * 1024 }
}
