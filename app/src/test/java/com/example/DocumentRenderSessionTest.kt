package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DocumentRenderSession
import com.example.data.local.PageSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class DocumentRenderSessionTest {
    private val png = java.util.Base64.getDecoder()
        .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=")

    @Test fun higherResolutionReplacesLowerResolutionWithoutDecodingAgain() = runBlocking {
        val session = DocumentRenderSession(ApplicationProvider.getApplicationContext<Application>())
        val streams = AtomicInteger()
        val source = PageSource.Directory(2) { streams.incrementAndGet(); png.inputStream() }
        try {
            val preview = session.render("清晰度", 1, 600_000) { source }
            val full = session.render("清晰度", 1, 2_500_000) { source }
            assertNotSame(preview, full)
            assertEquals("升级清晰度复用尺寸，只再读取一次像素", 2, streams.get())
            assertEquals(full.allocationByteCount, session.cachedBytes())
            assertFalse("缓存替换不能回收界面仍引用的预览", preview.isRecycled)
            repeat(8) {
                assertSame(full, session.render("清晰度", 1, 600_000) { error("来源应复用") })
                session.prefetch("清晰度", listOf(1), 600_000) { error("来源应复用") }
            }
            assertEquals("回读和预加载都不应再次解码", 2, streams.get())
            assertNotSame(full, session.render("清晰度", 2, 600_000) { source })
            session.trimMemory()
            session.render("清晰度", 1, 600_000) { source }
            assertEquals("内存清理后重读像素仍可复用本次来源的尺寸", 4, streams.get())
        } finally { session.close() }
    }

    @Test fun queuedLowerResolutionReusesInFlightHigherResolution() = runBlocking {
        val session = DocumentRenderSession(ApplicationProvider.getApplicationContext<Application>())
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val streams = AtomicInteger()
        val source = PageSource.Directory(1) {
            streams.incrementAndGet()
            entered.complete(Unit)
            check(release.await(10, TimeUnit.SECONDS))
            png.inputStream()
        }
        try {
            withTimeout(15000) {
                val full = async(start = CoroutineStart.UNDISPATCHED) { session.render("并发清晰度", 1, 2_500_000) { source } }
                entered.await()
                val preview = async(start = CoroutineStart.UNDISPATCHED) { session.render("并发清晰度", 1, 600_000) { source } }
                release.countDown()
                assertSame(full.await(), preview.await())
                assertEquals(1, streams.get())
            }
        } finally { release.countDown(); session.close() }
    }

    @Test fun foregroundDecodeDefersPrefetchWithoutBlockingSourceSwitch() = runBlocking {
        val session = DocumentRenderSession(ApplicationProvider.getApplicationContext<Application>())
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val prefetched = AtomicInteger()
        val source = PageSource.Directory(2) { page ->
            if (page == 1) {
                entered.complete(Unit)
                check(release.await(10, TimeUnit.SECONDS))
            } else prefetched.incrementAndGet()
            png.inputStream()
        }
        try {
            withTimeout(15000) {
                val visible = async(start = CoroutineStart.UNDISPATCHED) { session.render("旧书", 1, 1000) { source } }
                entered.await()
                val warming = async(start = CoroutineStart.UNDISPATCHED) { session.prefetch("旧书", listOf(2), 1000) { source } }
                assertEquals(0, prefetched.get())
                val next = async(start = CoroutineStart.UNDISPATCHED) {
                    session.render("新书", 1, 1000) { PageSource.Directory(1) { png.inputStream() } }
                }
                warming.cancelAndJoin()
                release.countDown()
                visible.await()
                val bitmap = next.await()
                session.close("旧书")
                assertSame(bitmap, session.render("新书", 1, 1000) { error("旧书清理不能关闭新书") })
            }
        } finally { release.countDown(); session.close() }
    }

    @Test fun renderAndPrefetchShareOneDecode() = runBlocking {
        val session = DocumentRenderSession(ApplicationProvider.getApplicationContext<Application>())
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val streams = AtomicInteger()
        val source = PageSource.Directory(1) {
            streams.incrementAndGet()
            entered.complete(Unit)
            check(release.await(10, TimeUnit.SECONDS))
            png.inputStream()
        }
        try {
            withTimeout(15000) {
                val first = async(start = CoroutineStart.UNDISPATCHED) { session.render("共享", 1, 1000) { source } }
                entered.await()
                val prefetch = async(start = CoroutineStart.UNDISPATCHED) { session.prefetch("共享", listOf(1), 1000) { error("重复打开") } }
                val second = async(start = CoroutineStart.UNDISPATCHED) { session.render("共享", 1, 1000) { error("重复打开") } }
                release.countDown()
                assertSame(first.await(), second.await())
                prefetch.await()
                assertEquals("一次解码复用尺寸与像素流", 1, streams.get())
            }
        } finally { release.countDown(); session.close() }
    }

    @Test fun parallelDecodesAreBoundedAndCloseWaitsForTheirCompletion() = runBlocking {
        val session = DocumentRenderSession(ApplicationProvider.getApplicationContext<Application>())
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val streams = AtomicInteger()
        val source = PageSource.Directory(8) {
            val count = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, count) }
            streams.incrementAndGet()
            if (count == 2) entered.complete(Unit)
            check(release.await(10, TimeUnit.SECONDS))
            object : java.io.ByteArrayInputStream(png) {
                override fun close() { active.decrementAndGet(); super.close() }
            }
        }
        try {
            withTimeout(15000) {
                val requests = (1..8).map { page -> async(start = CoroutineStart.UNDISPATCHED) { session.render("并发", page, 1000) { source } } }
                entered.await()
                val closing = async(start = CoroutineStart.UNDISPATCHED) { session.close() }
                assertFalse("在途解码结束前不能关闭来源", closing.isCompleted)
                release.countDown()
                requests.awaitAll()
                closing.await()
                assertEquals(2, peak.get())
                assertEquals(8, streams.get())
                assertEquals(0, active.get())
                assertEquals(0, session.cachedBytes())
            }
        } finally { release.countDown(); session.close() }
    }

    @Test fun cancellingQueuedDecodeReleasesItsSessionLease() = runBlocking {
        val session = DocumentRenderSession(ApplicationProvider.getApplicationContext<Application>())
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val streams = AtomicInteger()
        val source = PageSource.Directory(3) { page ->
            check(page != 3) { "取消的预取不能开始读取" }
            if (streams.incrementAndGet() == 2) entered.complete(Unit)
            check(release.await(10, TimeUnit.SECONDS))
            png.inputStream()
        }
        try {
            withTimeout(15000) {
                val active = (1..2).map { page -> async(start = CoroutineStart.UNDISPATCHED) { session.render("取消", page, 1000) { source } } }
                entered.await()
                val queued = async(start = CoroutineStart.UNDISPATCHED) { session.render("取消", 3, 1000) { source } }
                queued.cancelAndJoin()
                val closing = async(start = CoroutineStart.UNDISPATCHED) { session.close() }
                release.countDown()
                active.awaitAll()
                closing.await()
                assertEquals(2, streams.get())
                assertEquals(0, session.cachedBytes())
            }
        } finally { release.countDown(); session.close() }
    }

    private fun archive(app: Application, entries: List<String>): File =
        File.createTempFile("session-fixture-", ".cbz", app.cacheDir).apply {
            ZipOutputStream(outputStream()).use { zip ->
                entries.forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(png)
                    zip.closeEntry()
                }
            }
        }

    private fun tar(app: Application, entries: List<String>): File =
        File.createTempFile("session-fixture-", ".cbt", app.cacheDir).apply {
            outputStream().use { out ->
                entries.forEach { name ->
                    out.write(tarHeader(name, png.size.toLong()))
                    out.write(png)
                    val padding = (512 - png.size % 512) % 512
                    out.write(ByteArray(padding))
                }
                out.write(ByteArray(1024))
            }
        }

    private fun tarHeader(name: String, size: Long): ByteArray {
        val header = ByteArray(512)
        val nameBytes = name.toByteArray()
        System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 100))
        writeOctal(header, 100, 8, 0b111111111) // 0777
        writeOctal(header, 108, 8, 0)
        writeOctal(header, 116, 8, 0)
        writeOctal(header, 124, 12, size)
        writeOctal(header, 136, 12, 0)
        for (index in 148 until 156) header[index] = ' '.code.toByte()
        header[156] = '0'.code.toByte()
        val magic = "ustar".toByteArray()
        System.arraycopy(magic, 0, header, 257, magic.size)
        header[262] = 0
        header[263] = '0'.code.toByte()
        header[264] = '0'.code.toByte()
        var checksum = 0
        header.forEach { checksum += it.toInt() and 0xFF }
        writeOctal(header, 148, 7, checksum.toLong())
        header[155] = ' '.code.toByte()
        return header
    }

    private fun writeOctal(target: ByteArray, offset: Int, length: Int, value: Long) {
        val text = value.toString(8).padStart(length - 1, '0')
        val bytes = text.toByteArray()
        System.arraycopy(bytes, 0, target, offset, minOf(bytes.size, length - 1))
        target[offset + length - 1] = 0
    }

    @Test
    fun boundedCacheReleasesBitmapsArchiveAndTemporaryFile() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val copied = archive(app, listOf("1.png"))
        try {
            val first = session.render("版本1", 1, 2_000_000) { PageSource.Document(copied, "CBZ") }
            assertSame(first, session.render("版本1", 1, 2_000_000) { error("不应再次复制") })
            assertTrue(session.cachedBytes() > 0)
            assertTrue(session.cachedBytes() <= session.maxCacheBytes())
            session.trimMemory()
            assertEquals(0, session.cachedBytes())
        } finally { session.close() }
        assertFalse(copied.exists())
        assertEquals(0, session.cachedBytes())
    }

    @Test
    fun failedInitializationDeletesTemporarySourceAndCanRetry() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val corrupt = File.createTempFile("corrupt-", ".cbz", app.cacheDir).apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            assertTrue(runCatching {
                session.render("损坏", 1, 2_000_000) { PageSource.Document(corrupt, "CBZ") }
            }.isFailure)
            // 损坏来源必须被回收，不能留下临时文件或缓存
            assertFalse(corrupt.exists())
            assertEquals(0, session.cachedBytes())
            val retry = archive(app, listOf("1.png"))
            session.render("重试", 1, 2_000_000) { PageSource.Document(retry, "CBZ") }
        } finally { session.close() }
        Unit
    }

    @Test
    fun readsTarArchivePagesByRealOffsets() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val file = tar(app, listOf("卷1/01.png", "卷1/02.png"))
        try {
            val first = session.render("tar", 1, 1_000_000) { PageSource.Document(file, "CBT") }
            val second = session.render("tar", 2, 1_000_000) { error("不应重建来源") }
            assertEquals(1, first.width)
            assertEquals(1, second.width)
            assertNotSame(first, second)
            // 越界页码必须失败而不是返回空白
            assertTrue(runCatching { session.render("tar", 3, 1_000_000) { error("不应重建来源") } }.isFailure)
        } finally { session.close() }
    }

    @Test fun directSourceClosesHandleOnceWithoutDeletingOriginal() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val original = archive(app, listOf("1.png"))
        var releases = 0
        session.render("原件", 1, 1000) { PageSource.Document(original, "CBZ", owned = false, release = { releases++ }) }
        session.close()
        session.close()
        assertTrue(original.exists())
        assertEquals(1, releases)
        val corrupt = File.createTempFile("direct-invalid-", ".cbz", app.cacheDir).apply { writeBytes(byteArrayOf(1)) }
        assertTrue(runCatching {
            session.render("损坏原件", 1, 1000) { PageSource.Document(corrupt, "CBZ", owned = false, release = { releases++ }) }
        }.isFailure)
        assertTrue(corrupt.exists())
        assertEquals(2, releases)
        session.close()
        assertEquals(2, releases)
    }

    @Test
    fun readsDirectorySourceWithoutCopyingArchive() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val file = File.createTempFile("page-", ".png", app.cacheDir).apply { writeBytes(png) }
        val openedPages = mutableListOf<Int>()
        try {
            val bitmap = session.render("dir", 1, 1_000_000) {
                PageSource.Directory(1) { number ->
                    openedPages.add(number)
                    file.inputStream()
                }
            }
            assertEquals(1, bitmap.width)
            assertEquals(listOf(1), openedPages)
        } finally {
            session.close()
            file.delete()
        }
    }
}
