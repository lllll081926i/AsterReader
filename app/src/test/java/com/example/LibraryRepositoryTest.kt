package com.example

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsterDatabase
import com.example.data.local.LibraryRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class LibraryRepositoryTest {
    private lateinit var app: Application
    private lateinit var db: AsterDatabase
    private lateinit var repository: LibraryRepository
    private val files = mutableMapOf<String, File>()
    private val opened = java.util.concurrent.atomic.AtomicInteger()
    private var onOpen: (() -> Unit)? = null

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AsterDatabase::class.java).allowMainThreadQueries().build()
        repository = LibraryRepository(app, db.asterDao())
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun getType(uri: Uri) = "application/octet-stream"
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
                if (uri.lastPathSegment == "children") {
                    return MatrixCursor(arrayOf(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                        android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)).apply {
                        files.forEach { (name, _) -> addRow(arrayOf(name, "application/octet-stream", name)) }
                    }
                }
                val file = files.getValue(uri.lastPathSegment!!)
                return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)).apply {
                    addRow(arrayOf(file.name, file.length(), file.lastModified()))
                }
            }
            override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
                opened.incrementAndGet()
                onOpen?.invoke()
                return ParcelFileDescriptor.open(files.getValue(uri.lastPathSegment!!), ParcelFileDescriptor.MODE_READ_ONLY)
            }
            override fun insert(uri: Uri, values: ContentValues?): Uri? = error("只读来源")
            override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = error("不允许删除原件")
            override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = error("不允许修改原件")
        }
        provider.attachInfo(app, android.content.pm.ProviderInfo().apply { authority = "audit.documents" })
        ShadowContentResolver.registerProviderInternal("audit.documents", provider)
    }

    @After
    fun tearDown() = runBlocking { repository.closeReadSession(); db.close() }

    private fun document(name: String, bytes: ByteArray): Uri {
        val folder = File(app.cacheDir, "fixture-${java.util.UUID.randomUUID()}").apply { mkdirs() }
        files[name] = File(folder, name).apply { writeBytes(bytes) }
        return Uri.parse("content://audit.documents/$name")
    }

    private fun comicBytes(count: Int = 2): ByteArray {
        val bytes = java.io.ByteArrayOutputStream()
        val png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=")
        ZipOutputStream(bytes).use { zip ->
            for (number in 1..count) {
                zip.putNextEntry(ZipEntry("$number.png")); zip.write(png); zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    @Test
    fun importsActualComicAndPreservesSourceUriWithoutDuplicates() = runBlocking {
        val bytes = comicBytes()
        val uri = document("真实漫画.cbz", bytes)
        val imported = repository.importDocument(uri, persist = false)
        assertEquals(uri.toString(), imported.filePath)
        assertEquals(bytes.size.toLong(), imported.sizeBytes)
        assertEquals(2, imported.totalPages)
        assertEquals(imported.id, repository.importDocument(uri, persist = false).id)
        assertEquals(1, db.asterDao().getBookCount())
        assertTrue(files.getValue("真实漫画.cbz").exists())
    }

    @Test
    fun unsupportedAndDamagedFilesNeverCreateSuccessfulImports() = runBlocking {
        for ((name, bytes) in listOf("损坏.cbz" to byteArrayOf(1, 2), "不支持.bin" to byteArrayOf(3))) {
            assertTrue(name, runCatching { repository.importDocument(document(name, bytes), persist = false) }.isFailure)
        }
        assertEquals(0, db.asterDao().getBookCount())
        assertTrue(app.cacheDir.listFiles()!!.none { it.name.startsWith("document-") })
    }

    @Test
    fun unchangedComicRescanDoesNotReopenArchive() = runBlocking {
        val uri = document("增量扫描.cbz", comicBytes())
        val initial = repository.importDocument(uri, persist = false)
        val count = opened.get()
        assertEquals(initial, repository.importDocument(uri, persist = false, refreshExisting = true))
        assertEquals(count, opened.get())
        val file = files.getValue("增量扫描.cbz")
        file.writeBytes(comicBytes(3))
        file.setLastModified(initial.sourceModifiedAt + 1000)
        val updated = repository.importDocument(uri, persist = false, refreshExisting = true)
        assertEquals(initial.id, updated.id)
        assertEquals(3, updated.totalPages)
        assertEquals(1, db.asterDao().getBookCount())
        assertTrue(opened.get() > count)
    }

    @Test
    fun repeatedPagesReuseSessionAndClosingReleasesTemporaryFile() = runBlocking {
        val uri = document("会话缓存.cbz", comicBytes())
        val book = repository.importDocument(uri, persist = false)
        val count = opened.get()
        val first = repository.renderPage(book.id, 1)
        repository.renderPage(book.id, 2)
        assertSame(first, repository.renderPage(book.id, 1))
        assertEquals(count + 1, opened.get())
        assertEquals(1, app.cacheDir.listFiles()!!.count { it.name.startsWith("document-") })
        repository.trimPageMemory()
        repository.closeReadSession()
        repository.closeReadSession()
        assertTrue(app.cacheDir.listFiles()!!.none { it.name.startsWith("document-") })
        assertTrue(files.getValue("会话缓存.cbz").exists())
    }

    @Test
    fun directoryScanCountsActualFilesAndIncrementalScanDoesNotReparse() = runBlocking {
        document("漫画1.cbz", comicBytes())
        document("漫画2.cbz", comicBytes(3))
        document("忽略.bin", byteArrayOf(1))
        val tree = Uri.parse("content://audit.documents/tree/root")
        assertEquals(2 to 0, repository.scanRepository(tree, "测试仓库", "每个文件作为作品"))
        val count = opened.get()
        assertEquals(2 to 0, repository.scanRepository(tree, "测试仓库", "每个文件作为作品"))
        assertEquals(count, opened.get())
        assertEquals(2, db.asterDao().getBookCount())
    }

    @Test
    fun cancellingScanClosesPendingSourcesAndKeepsRepositoryRetryable() = runBlocking {
        document("待取消1.cbz", comicBytes())
        document("待取消2.cbz", comicBytes())
        val entered = java.util.concurrent.CountDownLatch(1)
        val proceed = java.util.concurrent.CountDownLatch(1)
        onOpen = { entered.countDown(); check(proceed.await(5, java.util.concurrent.TimeUnit.SECONDS)) }
        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.scanRepository(Uri.parse("content://audit.documents/tree/root"), "取消仓库", "每个文件作为作品")
        }
        try {
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS))
            job.cancel()
        } finally { proceed.countDown() }
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(app.cacheDir.listFiles()!!.none { it.name.startsWith("document-") })
        val repo = repository.observeAllRepositories().first().single()
        assertFalse(repo.isHealthy)
        assertEquals("扫描已取消", repo.lastScanText)
    }

    @Test
    fun pageOrderingUsesNumericSegments() {
        assertEquals(listOf("1.jpg", "2.jpg", "10.jpg"), listOf("10.jpg", "2.jpg", "1.jpg")
            .sortedWith(LibraryRepository::naturalCompare))
    }
}
