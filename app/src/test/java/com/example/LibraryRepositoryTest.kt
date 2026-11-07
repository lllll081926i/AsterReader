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
    @org.robolectric.annotation.Implements(android.content.ContentResolver::class)
    class TemporaryPermissionResolver : ShadowContentResolver() {
        @org.robolectric.annotation.Implementation
        override fun takePersistableUriPermission(uri: Uri, flags: Int) {
            throw SecurityException("测试提供者仅授予临时读取")
        }
    }
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

    @Test
    @Config(shadows = [TemporaryPermissionResolver::class])
    fun temporaryPermissionCreatesDurableSource() = runBlocking {
        val uri = document("临时漫画.cbz", comicBytes())
        val book = repository.importDocument(uri)
        assertEquals("file", Uri.parse(book.filePath).scheme)
        val managed = File(Uri.parse(book.filePath).path!!).canonicalFile
        assertTrue("副本 $managed 应位于 ${app.filesDir.canonicalFile}", managed.toPath().startsWith(app.filesDir.canonicalFile.toPath()))
        files.clear()
        repository.closeReadSession()
        val rendered = repository.renderPage(book.id, 1, 1000)
        assertTrue(rendered.width > 0)
        assertEquals(2, book.totalPages)
    }

    @Test
    @Config(shadows = [TemporaryPermissionResolver::class])
    fun failedTemporaryImportLeavesNoManagedFile() = runBlocking {
        val imports = File(app.filesDir, "imports")
        val before = imports.listFiles()?.map { it.name }?.toSet().orEmpty()
        val uri = document("损坏.cbz", byteArrayOf(1, 2, 3))
        assertTrue(runCatching { repository.importDocument(uri) }.isFailure)
        assertEquals(before, imports.listFiles()?.map { it.name }?.toSet().orEmpty())
    }

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

    private val png: ByteArray = java.util.Base64.getDecoder()
        .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=")

    /** 构造真实 USTAR 归档，用于验证 TAR 随机读取与按目录分章。 */
    private fun tarBytes(names: List<String>): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        names.forEach { name ->
            val header = ByteArray(512)
            val nameBytes = name.toByteArray()
            System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 100))
            writeOctal(header, 100, 8, 0b111111111)
            writeOctal(header, 108, 8, 0)
            writeOctal(header, 116, 8, 0)
            writeOctal(header, 124, 12, png.size.toLong())
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
            out.write(header)
            out.write(png)
            out.write(ByteArray((512 - png.size % 512) % 512))
        }
        out.write(ByteArray(1024))
        return out.toByteArray()
    }

    private fun writeOctal(target: ByteArray, offset: Int, length: Int, value: Long) {
        val text = value.toString(8).padStart(length - 1, '0')
        val bytes = text.toByteArray()
        System.arraycopy(bytes, 0, target, offset, minOf(bytes.size, length - 1))
        target[offset + length - 1] = 0
    }

    @Test
    fun importsStandaloneImageAndPersistsViewport() = runBlocking {
        val uri = document("单页.png", png)
        val book = repository.importDocument(uri, persist = false)
        assertEquals("IMAGE", book.format)
        assertEquals(1, book.totalPages)
        assertEquals(1, repository.renderPage(book.id, 1).width)

        repository.saveViewport(book.id, com.asterreader.core.model.ReaderViewport(
            page = 1, offsetFraction = 0.42f, scale = 2.5f, panX = 0.25f, panY = 0f))
        val state = repository.readerState(book.id)
        assertEquals(0.42f, state?.offsetFraction)
        assertEquals(2.5f, state?.scale)
    }

    @Test
    fun importsTarArchiveAndDerivesChaptersFromFolders() = runBlocking {
        val uri = document("合订本.cbt", tarBytes(listOf("卷1/01.png", "卷1/02.png", "卷2/01.png")))
        val book = repository.importDocument(uri, persist = false)
        assertEquals("CBT", book.format)
        assertEquals(3, book.totalPages)
        val chapters = db.asterDao().getChapters(book.id)
        assertEquals(listOf("卷1", "卷2"), chapters.map { it.title })
        assertEquals(1, chapters[0].startPage)
        assertEquals(2, chapters[0].endPage)
        assertEquals(3, chapters[1].startPage)
        assertEquals(1, repository.renderPage(book.id, 3).width)
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
    fun zipImportGeneratesReadableCoverFile() = runBlocking {
        val imported = repository.importDocument(document("封面.cbz", comicBytes()), persist = false)
        assertTrue("封面路径为空", imported.coverUrl.isNotBlank())
        val cover = File(imported.coverUrl)
        assertTrue("封面文件不存在：${imported.coverUrl}", cover.exists() && cover.length() > 0)
        assertEquals(1, android.graphics.BitmapFactory.decodeFile(cover.absolutePath)?.width)
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
    fun repeatedPagesReuseSessionAndKeepLocalCopyForReopen() = runBlocking {
        val uri = document("会话缓存.cbz", comicBytes())
        val book = repository.importDocument(uri, persist = false)
        val count = opened.get()
        val first = repository.renderPage(book.id, 1)
        repository.renderPage(book.id, 2)
        assertSame(first, repository.renderPage(book.id, 1))
        assertEquals(count + 1, opened.get())
        // 阅读用的本地副本落在 read/ 下，不再是需要每次重建的 document- 临时文件
        val copyDir = java.io.File(app.cacheDir, "read")
        assertEquals(1, copyDir.listFiles()!!.count { it.name.endsWith(".arc") })
        repository.trimPageMemory()
        repository.closeReadSession()
        repository.closeReadSession()
        // 副本保留以便再次打开时直接命中，避免整本重新复制
        assertEquals(1, copyDir.listFiles()!!.count { it.name.endsWith(".arc") })
        repository.renderPage(book.id, 1)
        assertEquals(count + 1, opened.get())
        assertTrue(files.getValue("会话缓存.cbz").exists())
        repository.closeReadSession()
    }

    @Test
    fun directoryScanCountsActualFilesAndIncrementalScanDoesNotReparse() = runBlocking {
        document("漫画1.cbz", comicBytes())
        document("漫画2.cbz", comicBytes(3))
        document("忽略.bin", byteArrayOf(1))
        val tree = Uri.parse("content://audit.documents/tree/root")
        val first = repository.scanRepository(tree, "测试仓库", "每个文件作为作品")
        assertEquals(2, first.imported)
        assertEquals(0, first.failed)
        val count = opened.get()
        val second = repository.scanRepository(tree, "测试仓库", "每个文件作为作品")
        assertEquals(0, second.imported)
        assertEquals(0, second.updated)
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
