package com.example

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsterDatabase
import com.example.data.local.LibraryRepository
import com.example.data.local.LibraryBackup
import com.asterreader.core.database.AnnotationEntity
import com.asterreader.core.database.ComicPageEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** 对全量审查中真实复现的数据和导入故障做回归验证。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AuditRegressionTest {
    @Test fun duplicateShelfNameIsRejectedAndTagsOnlyChangePublicBooks() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            val shelf = com.asterreader.core.database.ShelfEntity("a", "书架", "", 0, "", "")
            dao.createUniqueShelf(shelf)
            assertTrue(runCatching { dao.createUniqueShelf(shelf.copy(id = "b")) }.isFailure)
            assertEquals(1, dao.getAllShelves().size)
            dao.insertBook(comicFixture("公开").copy(tagsCsv = "短篇,长篇,短篇集"))
            dao.insertBook(comicFixture("隐藏").copy(tagsCsv = "短篇", isVaultProtected = true))
            dao.removePublicTag("短篇")
            assertEquals("长篇,短篇集", dao.getBookById("公开")!!.tagsCsv)
            assertEquals("短篇", dao.getBookById("隐藏")!!.tagsCsv)
        } finally { database.close() }
    }

    @Test fun readingPositionRollsBackWhenViewportWriteFails() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            val book = comicFixture("事务进度")
            dao.insertBook(book)
            database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_viewport BEFORE INSERT ON reader_states BEGIN SELECT RAISE(ABORT, '测试失败'); END")
            assertTrue(runCatching { dao.saveReadingPosition(book.id, 20, "新章节", com.asterreader.core.model.ReaderViewport(20, scale = 2f), 100L) }.isFailure)
            assertEquals(book.currentPage, dao.getBookById(book.id)!!.currentPage)
            assertNull(dao.getReaderState(book.id))
        } finally { database.close() }
    }

    @Test fun readingPositionCommitsProgressAndViewportTogether() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            dao.insertBook(comicFixture("事务进度"))
            dao.saveReaderOptions("事务进度", "DUAL_PAGE", "原始比例", true, true)
            dao.saveReadingPosition("事务进度", 20, "章节", com.asterreader.core.model.ReaderViewport(20, scale = 2f, panX = 0.5f), 100L)
            assertEquals(20, dao.getBookById("事务进度")!!.currentPage)
            assertEquals(20, dao.getReaderState("事务进度")!!.page)
            assertEquals("DUAL_PAGE", dao.getReaderState("事务进度")!!.layoutMode)
            assertEquals(0.5f, dao.getReaderState("事务进度")!!.panX, 0f)
        } finally { database.close() }
    }

    @Test fun nativeDimensionsRemainStableAcrossResolutionChanges() = runBlocking {
        val database = db()
        try {
            val repository = LibraryRepository(app(), database.asterDao())
            val book = repository.importDocument(Uri.fromFile(image()), persist = false)
            val low = repository.renderPage(book.id, 1, 100)
            val high = repository.renderPage(book.id, 1, 1000)
            assertTrue(low.width < high.width)
            assertEquals(20 to 30, repository.nativePageSize(low))
            assertEquals(repository.nativePageSize(low), repository.nativePageSize(high))
            repository.closeReadSession()
        } finally { database.close() }
    }
    @Test fun looseFilesDoNotBecomeSingleVolumeSeries() {
        val database = db()
        try {
            val repository = LibraryRepository(app(), database.asterDao())
            assertEquals("" to 0, repository.parseSeriesAndVolume("随笔2026.cbz"))
            assertEquals("" to 0, repository.parseSeriesAndVolume("独立短篇.cbz"))
            assertEquals("长篇" to 2, repository.parseSeriesAndVolume("长篇 第02卷.cbz"))
            assertEquals("Series" to 11, repository.parseSeriesAndVolume("Series Vol.11.cbz"))
        } finally { database.close() }
    }
    private fun app() = ApplicationProvider.getApplicationContext<Application>()
    private fun db() = Room.inMemoryDatabaseBuilder(app(), AsterDatabase::class.java).allowMainThreadQueries().build()
    private fun image(): File = File.createTempFile("audit-", ".png", app().cacheDir).also { file ->
        val bitmap = Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun validImageImportsWithDecodableCover() = runBlocking {
        val database = db()
        try {
            val repository = LibraryRepository(app(), database.asterDao())
            val book = repository.importDocument(Uri.fromFile(image()), persist = false)
            assertEquals(1, book.totalPages)
            assertNotNull(android.graphics.BitmapFactory.decodeFile(book.coverUrl))
        } finally { database.close() }
    }

    @Test fun relocatePreservesIdentityWithoutDuplicate() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            dao.insertBook(comicFixture("原作品").copy(sourceStatus = "MISSING"))
            val repository = LibraryRepository(app(), dao)
            assertTrue(repository.relocateBookSource("原作品", Uri.fromFile(image())))
            assertEquals(1, dao.getBookCount())
            assertEquals("AVAILABLE", dao.getBookById("原作品")!!.sourceStatus)
        } finally { database.close() }
    }

    @Test fun ordinaryBackupExcludesHiddenTitleAndNote() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            dao.insertBook(comicFixture("隐藏作品").copy(title = "敏感标题审查样本", isVaultProtected = true))
            dao.insertAnnotation(AnnotationEntity(bookId = "隐藏作品", chapterTitle = "", pageNumber = 1,
                quoteText = "", noteText = "敏感笔记审查样本", colorName = "GREEN", type = "NOTE", createdAtText = ""))
            val bytes = ByteArrayOutputStream()
            LibraryBackup.export(dao, bytes)
            val json = bytes.toString("UTF-8")
            assertFalse(json.contains("敏感标题审查样本"))
            assertFalse(json.contains("敏感笔记审查样本"))
        } finally { database.close() }
    }

    @Test fun repeatedRestoreIsIdempotentAndKeepsNewerProgress() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            dao.insertBook(comicFixture("作品"))
            dao.insertAnnotation(AnnotationEntity(bookId = "作品", chapterTitle = "", pageNumber = 1,
                quoteText = "", noteText = "笔记", colorName = "GREEN", type = "NOTE", createdAtText = ""))
            val bytes = ByteArrayOutputStream()
            LibraryBackup.export(dao, bytes)
            dao.updateProgress("作品", 50, 50, "在读", null, 100L)
            LibraryBackup.import(dao, ByteArrayInputStream(bytes.toByteArray()))
            LibraryBackup.import(dao, ByteArrayInputStream(bytes.toByteArray()))
            assertEquals(50, dao.getBookById("作品")!!.currentPage)
            assertEquals(1, dao.getAllAnnotations().size)
        } finally { database.close() }
    }

    @Test fun failedRestoreLeavesDatabaseUnchanged() = runBlocking {
        val database = db()
        try {
            val text = """{"format":"aster-library","version":1,"books":[{"id":"部分恢复"}],"readerStates":[{"bookId":"不存在"}]}"""
            assertTrue(runCatching { LibraryBackup.import(database.asterDao(), ByteArrayInputStream(text.toByteArray())) }.isFailure)
            assertNull(database.asterDao().getBookById("部分恢复"))
        } finally { database.close() }
    }


    @Test fun sameUnreachableUriRemainsMissing() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            val uri = "content://nonexistent.audit.provider/file.cbz"
            dao.insertBook(comicFixture("失联").copy(filePath = uri, sourceStatus = "MISSING"))
            assertFalse(LibraryRepository(app(), dao).relocateBookSource("失联", Uri.parse(uri)))
            assertEquals("MISSING", dao.getBookById("失联")!!.sourceStatus)
        } finally { database.close() }
    }

    @Test fun folderBackupPreservesPageIndex() = runBlocking {
        val database = db()
        try {
            val dao = database.asterDao()
            dao.insertBook(comicFixture("目录").copy(format = "IMAGE_DIR", totalPages = 1, contentFingerprint = "签名"))
            dao.insertComicPages(listOf(ComicPageEntity("目录", 1, "content://audit/page.png", "1.png", 1, 0)))
            val bytes = ByteArrayOutputStream()
            LibraryBackup.export(dao, bytes)
            dao.removeBooks(listOf("目录"))
            LibraryBackup.import(dao, ByteArrayInputStream(bytes.toByteArray()))
            assertNotNull(dao.getBookById("目录"))
            assertEquals(1, dao.getComicPages("目录").size)
        } finally { database.close() }
    }
}
