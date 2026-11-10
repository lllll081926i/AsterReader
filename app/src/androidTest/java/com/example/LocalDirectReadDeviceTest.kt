package com.example

import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AsterDatabase
import com.example.data.local.LibraryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

/** 独立测试目录与内存数据库；不访问用户书库。 */
@RunWith(AndroidJUnit4::class)
class LocalDirectReadDeviceTest {
    @Test fun reusedRegionDecoderKeepsPixelsAfterInputCloses() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val original = android.graphics.Bitmap.createBitmap(400, 1600, android.graphics.Bitmap.Config.ARGB_8888)
        original.eraseColor(android.graphics.Color.RED)
        android.graphics.Canvas(original).drawRect(0f, 800f, 400f, 1600f,
            android.graphics.Paint().apply { color = android.graphics.Color.BLUE })
        val bytes = java.io.ByteArrayOutputStream().also {
            original.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it)
        }.toByteArray()
        original.recycle()
        val opened = java.util.concurrent.atomic.AtomicInteger()
        val source = com.example.data.local.PageSource.Directory(1) {
            opened.incrementAndGet()
            bytes.inputStream()
        }
        val session = com.example.data.local.DocumentRenderSession(app)
        try {
            val top = session.renderRegion("区域像素", 1, 0f, 0.25f, 160_000) { source }!!
            val bottom = session.renderRegion("区域像素", 1, 0.5f, 0.75f, 160_000) { source }!!
            assertTrue(android.graphics.Color.red(top.getPixel(200, 200)) > 200)
            assertTrue(android.graphics.Color.blue(bottom.getPixel(200, 200)) > 200)
            assertEquals(1, opened.get())
            session.close()
            assertNotNull(session.renderRegion("区域像素", 1, 0.25f, 0.5f, 160_000) { source })
            assertEquals(2, opened.get())
        } finally { session.close() }
    }

    @Test fun safThreeGiBTarImportsAndRendersWithoutWholeFileCopies() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "local-direct-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(directory, "大文件.cbt")
        val png = android.util.Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=", android.util.Base64.DEFAULT)
        val header = ByteArray(512)
        "01.png".toByteArray().copyInto(header)
        fun octal(offset: Int, size: Int, value: Long) {
            value.toString(8).padStart(size - 1, '0').toByteArray().copyInto(header, offset)
        }
        octal(100, 8, 420); octal(124, 12, png.size.toLong())
        for (index in 148..155) header[index] = 32
        header[156] = '0'.code.toByte()
        "ustar".toByteArray().copyInto(header, 257)
        octal(148, 7, header.sumOf { it.toInt() and 255 }.toLong()); header[155] = 32
        RandomAccessFile(source, "rw").use { file ->
            file.write(header); file.write(png)
            file.setLength(3L * 1024 * 1024 * 1024)
        }
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun getType(uri: Uri) = "application/x-tar"
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor =
                MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED)).apply {
                    addRow(arrayOf<Any>(source.name, source.length(), source.lastModified()))
                }
            override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
            override fun insert(uri: Uri, values: ContentValues?): Uri? = error("只读测试来源")
            override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = error("只读测试来源")
            override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = error("只读测试来源")
        }
        provider.attachInfo(app, ProviderInfo().apply { authority = "local.direct.test" })
        val resolver = ContentResolver.wrap(provider)
        val isolated = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getContentResolver() = resolver
            override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(directory, "private").apply { mkdirs() }
        }
        val db = Room.inMemoryDatabaseBuilder(app, AsterDatabase::class.java).build()
        val repository = LibraryRepository(isolated, db.asterDao())
        try {
            val book = repository.importDocument(Uri.parse("content://local.direct.test/book"), persist = false)
            assertEquals(source.length(), book.sizeBytes)
            assertEquals(1, book.totalPages)
            assertEquals(1, repository.renderPage(book.id, 1).width)
            assertFalse(File(isolated.cacheDir, "read").exists())
            assertTrue(isolated.cacheDir.listFiles().orEmpty().none { it.name.startsWith("document-") })
            repository.closeReadSession()
            assertTrue(source.exists())
        } finally {
            repository.closeReadSession()
            db.close()
            // 测试资产留在独立缓存目录，由系统缓存策略回收。
        }
    }
}
