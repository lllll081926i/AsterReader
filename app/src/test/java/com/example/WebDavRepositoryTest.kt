package com.example

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.provider.DocumentsContract
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsterDatabase
import com.example.data.local.LibraryRepository
import com.example.data.remote.*
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class WebDavRepositoryTest {
    private lateinit var context: Context
    private lateinit var accounts: WebDavAccounts
    private lateinit var repository: WebDavRepository
    private lateinit var server: HttpServer
    private lateinit var db: AsterDatabase
    private lateinit var library: LibraryRepository
    private var etag = "v1"
    private var brokenLength = false
    private var missingRevision = false
    private val gets = AtomicInteger()
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val comic by lazy {
        val png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=")
        java.io.ByteArrayOutputStream().also { bytes -> java.util.zip.ZipOutputStream(bytes).use { zip ->
            for (page in 1..2) { zip.putNextEntry(java.util.zip.ZipEntry("$page.png")); zip.write(png); zip.closeEntry() }
        } }.toByteArray()
    }

    @Before fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.cacheDir, "dav-${UUID.randomUUID()}").apply { mkdirs() }
        context = object : ContextWrapper(app) {
            override fun getCacheDir() = File(root, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "private").apply { mkdirs() }
            override fun getApplicationContext(): Context = this
        }
        accounts = WebDavAccounts(context, { key }, {})
        assertEquals("每项测试必须使用独立凭据文件", 0, accounts.read().size)
        repository = WebDavRepository(context, accounts)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/dav/") { exchange ->
            val bytes = if (exchange.requestMethod == "PROPFIND") {
                """<d:multistatus xmlns:d="DAV:"><d:response><d:href>/dav/book.cbz</d:href><d:propstat><d:prop><d:getcontentlength>${comic.size + if (brokenLength) 1 else 0}</d:getcontentlength>${if (missingRevision) "" else "<d:getetag>\"$etag\"</d:getetag><d:getlastmodified>Sun, 01 Nov 2026 10:00:00 GMT</d:getlastmodified>"}</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>""".toByteArray()
            } else { gets.incrementAndGet(); comic }
            exchange.sendResponseHeaders(if (exchange.requestMethod == "PROPFIND") 207 else 200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        val provider = WebDavDocumentsProvider(repository)
        provider.attachInfo(context, ProviderInfo().apply { authority = WebDavRepository.AUTHORITY; exported = true; grantUriPermissions = true
            readPermission = "android.permission.MANAGE_DOCUMENTS"; writePermission = readPermission })
        ShadowContentResolver.registerProviderInternal(WebDavRepository.AUTHORITY, provider)
        db = Room.inMemoryDatabaseBuilder(context, AsterDatabase::class.java).allowMainThreadQueries().build()
        library = LibraryRepository(context, db.asterDao(), repository)
    }

    @After fun close() = runBlocking { library.closeReadSession(); db.close(); server.stop(0) }
    private suspend fun connect() = repository.save(null, "测试连接", "http://127.0.0.1:${server.address.port}/dav/", "reader", "fixture-password".toCharArray(), true, true)

    @Test fun encryptedAccountsPersistWithoutPlaintextAndRejectWrongKey() = runBlocking {
        val connection = connect()
        val ciphertext = File(context.noBackupFilesDir, "webdav-accounts.gcm").readBytes()
        assertFalse(String(ciphertext).contains("fixture-password")); assertFalse(String(ciphertext).contains("reader"))
        assertEquals(connection, WebDavRepository(context, WebDavAccounts(context, { key }, {})).connections().single())
        val wrong = WebDavAccounts(context, { SecretKeySpec(ByteArray(32) { 99 }, "AES") }, {})
        assertTrue(runCatching { wrong.read() }.isFailure)
        assertArrayEquals(ciphertext, File(context.noBackupFilesDir, "webdav-accounts.gcm").readBytes())
    }

    @Test fun importsThroughDocumentSourceWithCoverProgressAndCachedReading() = runBlocking {
        val connection = connect()
        val entry = repository.list(connection.id, "").single()
        val uri = WebDavRepository.documentUri(connection.id, entry.path)
        val book = library.importDocument(uri, connection.name, persist = false)
        assertEquals(2, book.totalPages); assertEquals(uri.toString(), book.filePath)
        assertTrue(File(book.coverUrl).isFile)
        val bitmap = library.renderPage(book.id, 1, 1000)
        assertTrue(bitmap.width > 0)
        library.saveReadingPosition(book.id, 2, null, null)
        assertEquals(2, library.getBookById(book.id)?.currentPage)
        assertEquals(1, gets.get())
        assertTrue(repository.cached(connection.id, entry))
    }

    @Test fun rescanRetainsIdentityAndUsesEtagForRevision() = runBlocking {
        val connection = connect()
        val tree = WebDavRepository.treeUri(connection.id)
        assertEquals(1, library.scanRepository(tree, connection.name, LibraryRepository.DEFAULT_SCAN_RULE).imported)
        val first = db.asterDao().getAllBooks().single()
        assertEquals(0, first.totalPages)
        assertEquals(0, gets.get())
        assertEquals(2, library.ensureReadingIndex(first.id).totalPages)
        val shelf = db.asterDao().getAllShelves().single()
        assertTrue(first.shelfIdsCsv.split(',').contains(shelf.id))
        library.renameShelf(shelf.id, "我的云端漫画")
        library.saveReadingPosition(first.id, 2, null, null)
        library.updateMetadata(first.id, "自定义标题", "作者", "", "", "标签")
        etag = "v2"
        val secondReport = library.scanRepository(tree, connection.name, LibraryRepository.DEFAULT_SCAN_RULE)
        val second = db.asterDao().getAllBooks().single()
        assertEquals(first.id, second.id); assertEquals(2, second.currentPage)
        assertEquals("自定义标题", second.title); assertEquals("作者", second.author)
        assertNotEquals(first.contentFingerprint, second.contentFingerprint)
        assertEquals(1, secondReport.updated)
        assertEquals(0, second.totalPages)
        assertEquals(1, gets.get())
        assertEquals(2, library.ensureReadingIndex(second.id).totalPages)
        assertEquals(2, gets.get())
        assertEquals(shelf.id, db.asterDao().getAllShelves().single().id)
        assertEquals("我的云端漫画", db.asterDao().getAllShelves().single().name)
    }

    @Test fun multipleConnectionsKeepSeparateShelvesAndBooks() = runBlocking {
        val first = connect()
        val second = connect()
        library.scanRepository(WebDavRepository.treeUri(first.id), "漫画", LibraryRepository.DEFAULT_SCAN_RULE)
        library.scanRepository(WebDavRepository.treeUri(second.id), "漫画", LibraryRepository.DEFAULT_SCAN_RULE)
        assertEquals(2, repository.connections().size)
        val shelves = db.asterDao().getAllShelves()
        assertEquals(setOf("漫画", "漫画 (2)"), shelves.map { it.name }.toSet())
        assertEquals(2, db.asterDao().getAllBooks().size)
        assertEquals(2, db.asterDao().getAllBooks().map { it.shelfIdsCsv }.toSet().size)
        repository.remove(first.id)
        assertEquals(2, db.asterDao().getAllShelves().size)
        assertEquals(2, db.asterDao().getAllBooks().size)
    }

    @Test fun hiddenComicIsReportedButNotAddedToPublicCloudShelf() = runBlocking {
        val connection = connect()
        val entry = repository.list(connection.id, "").single()
        val book = library.importDocument(WebDavRepository.documentUri(connection.id, entry.path), persist = false)
        db.asterDao().setBookHidden(book.id, true)
        val report = library.scanRepository(WebDavRepository.treeUri(connection.id), "云端目录", LibraryRepository.DEFAULT_SCAN_RULE)
        assertEquals(1, report.hidden)
        val shelf = db.asterDao().getAllShelves().single()
        assertFalse(db.asterDao().getBookById(book.id)!!.shelfIdsCsv.split(',').contains(shelf.id))
    }

    @Test fun interruptedDownloadsLeaveNoCommittedCacheAndRemovalPreservesBooks() = runBlocking {
        val connection = connect()
        brokenLength = true
        val entry = repository.list(connection.id, "").single()
        assertTrue(runCatching { repository.open(WebDavRepository.documentId(connection.id, entry.path)) }.isFailure)
        assertTrue(File(context.cacheDir, "webdav").listFiles().orEmpty().isEmpty())
        brokenLength = false
        repository.list(connection.id, "")
        library.importDocument(WebDavRepository.documentUri(connection.id, entry.path), persist = false)
        repository.remove(connection.id)
        assertEquals(1, db.asterDao().getAllBooks().size)
        assertEquals("移除当前连接后不应残留凭据", 0, repository.connections().size)
        assertTrue(File(context.cacheDir, "webdav").listFiles().orEmpty().isEmpty())
    }

    @Test fun documentIdsRoundTripAndRejectInvalidConnectionIds() {
        val id = UUID.randomUUID().toString()
        assertEquals(id to "%E6%BC%AB%E7%94%BB/1.cbz", WebDavRepository.decodeId(WebDavRepository.documentId(id, "%E6%BC%AB%E7%94%BB/1.cbz")))
        assertTrue(runCatching { WebDavRepository.decodeId("../../elsewhere:abc") }.isFailure)
        for (path in listOf("folder/../outside.cbz", "folder/%2e%2e/outside.cbz", "/outside.cbz", "folder%2foutside.cbz"))
            assertTrue(runCatching { WebDavRepository.documentId(id, path) }.isFailure)
        val provider = WebDavDocumentsProvider(repository)
        assertTrue(provider.isChildDocument(WebDavRepository.documentId(id, "folder/"), WebDavRepository.documentId(id, "folder/1.cbz")))
        assertFalse(provider.isChildDocument(WebDavRepository.documentId(id, "folder/"), WebDavRepository.documentId(id, "folder-else/1.cbz")))
    }

    @Test fun completeCacheSurvivesRepositoryRestartAndNetworkLoss() = runBlocking {
        val connection = connect()
        val entry = repository.list(connection.id, "").single()
        val id = WebDavRepository.documentId(connection.id, entry.path)
        assertArrayEquals(comic, repository.open(id).readBytes())
        server.stop(0)
        val reopened = WebDavRepository(context, WebDavAccounts(context, { key }, {}))
        assertArrayEquals(comic, reopened.open(id).readBytes())
        assertEquals(1, gets.get())
    }

    @Test fun corruptCacheIsReplacedAndCredentialEditPersists() = runBlocking {
        val connection = connect()
        val entry = repository.list(connection.id, "").single()
        val id = WebDavRepository.documentId(connection.id, entry.path)
        val cached = repository.open(id)
        cached.writeBytes(byteArrayOf(1))
        assertArrayEquals(comic, repository.open(id).readBytes())
        assertEquals(2, gets.get())
        repository.save(connection.id, "改名", connection.address, "new-reader", CharArray(0), true, true, keepPassword = true)
        assertEquals("改名", repository.connections().single().name)
        assertEquals("fixture-password", accounts.read().single().password)
        repository.save(connection.id, "改名", connection.address, "", CharArray(0), true, true, keepPassword = false)
        assertEquals("", accounts.read().single().password)
    }

    @Test fun explicitRefreshDoesNotReuseUnversionedServerContentForever() = runBlocking {
        missingRevision = true
        val connection = connect()
        val first = repository.list(connection.id, "").single()
        val id = WebDavRepository.documentId(connection.id, first.path)
        repository.open(id)
        repository.open(id)
        assertEquals(1, gets.get())
        val refreshed = repository.list(connection.id, "").single()
        assertTrue(refreshed.etag.isEmpty())
        assertNotEquals(first.revision, refreshed.revision)
        repository.open(id)
        assertEquals(2, gets.get())
    }
}
