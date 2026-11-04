package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DocumentRenderSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
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
    private fun archive(app: Application): File = File.createTempFile("session-fixture-", ".cbz", app.cacheDir).apply {
        ZipOutputStream(outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("1.png"))
            zip.write(java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII="))
            zip.closeEntry()
        }
    }

    @Test
    fun boundedCacheReleasesBitmapsArchiveAndTemporaryFile() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val copied = archive(app)
        val book = comicFixture().copy(totalPages = 1)
        try {
            val first = session.render(book, 1, "版本1", { copied }, { listOf("1.png") })
            assertSame(first, session.render(book, 1, "版本1", { error("不应再次复制") }, { error("不应再次读取索引") }))
            assertTrue(session.cachedBytes() > 0)
            assertTrue(session.cachedBytes() <= session.maxCacheBytes())
            session.trimMemory()
            assertEquals(0, session.cachedBytes())
        } finally { session.close() }
        assertFalse(copied.exists())
        assertEquals(0, session.cachedBytes())
        session.close()
    }

    @Test
    fun cancelledInitializationClosesArchiveAndCanRetry() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val session = DocumentRenderSession(app)
        val copied = archive(app)
        val book = comicFixture().copy(totalPages = 1)
        try {
            assertTrue(runCatching {
                session.render(book, 1, "已取消", { copied }, { throw CancellationException("取消测试") })
            }.exceptionOrNull() is CancellationException)
            assertFalse(copied.exists())
            assertEquals(0, session.cachedBytes())
            val retry = archive(app)
            session.render(book, 1, "重试", { retry }, { listOf("1.png") })
        } finally { session.close() }
        Unit
    }
}
