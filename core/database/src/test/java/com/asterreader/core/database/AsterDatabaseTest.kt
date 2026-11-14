package com.asterreader.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AsterDatabaseTest {

    private lateinit var database: AsterDatabase
    private lateinit var dao: AsterDao

    @Test fun scrollingWithinPageSavesViewportWithoutUpdatingTheLibrary() = runBlocking {
        val book = comicFixture().copy(totalPages = 100)
        dao.insertBook(book)
        dao.saveReadingPosition(book.id, 5, "正文", null, 1_000L)
        database.openHelper.writableDatabase.execSQL("CREATE TABLE book_writes (count INTEGER NOT NULL)")
        database.openHelper.writableDatabase.execSQL("INSERT INTO book_writes VALUES (0)")
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER count_book_writes AFTER UPDATE ON books BEGIN UPDATE book_writes SET count = count + 1; END")
        repeat(20) { index ->
            dao.saveReadingPosition(book.id, 5, null,
                com.asterreader.core.model.ReaderViewport(5, offsetFraction = (index + 1) / 25f), 2_000L + index * 900L)
        }
        database.openHelper.readableDatabase.query("SELECT count FROM book_writes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        assertEquals(0.8f, dao.getReaderState(book.id)!!.offsetFraction, 0f)
        assertEquals(1_000L, dao.getBookById(book.id)!!.lastReadTimestamp)
        dao.saveReadingPosition(book.id, 5, null, null, 31_000L)
        assertEquals(31_000L, dao.getBookById(book.id)!!.lastReadTimestamp)
        dao.saveReadingPosition(book.id, 6, "下一章", null, 31_001L)
        val next = dao.getBookById(book.id)!!
        assertEquals(6, next.currentPage)
        assertEquals("下一章", next.currentChapterTitle)
        assertEquals(31_001L, next.lastReadTimestamp)
    }

    @Test fun chapterChangesAndClockRollbackStillUpdateRecentReading() = runBlocking {
        val book = comicFixture().copy(totalPages = 100)
        dao.insertBook(book)
        dao.saveReadingPosition(book.id, 5, "正文", null, 50_000L)
        dao.saveReadingPosition(book.id, 5, "新章节", null, 50_001L)
        assertEquals("新章节", dao.getBookById(book.id)!!.currentChapterTitle)
        assertEquals(50_001L, dao.getBookById(book.id)!!.lastReadTimestamp)
        dao.saveReadingPosition(book.id, 5, null, null, 10_000L)
        assertEquals(10_000L, dao.getBookById(book.id)!!.lastReadTimestamp)
    }

    @Test fun completedStatusSurvivesSavingTheFirstPageOfTheLastSpreadAndReopening() = runBlocking {
        val book = comicFixture().copy(totalPages = 4)
        dao.insertBook(book)
        dao.markReadingCompleted(book.id)
        dao.saveReadingPosition(book.id, 3, null, null, 10L)
        val saved = dao.getBookById(book.id)!!
        assertEquals(3, saved.currentPage)
        assertEquals(100, saved.progressPercent)
        assertEquals("已读完", saved.statusBadge)
        dao.saveReadingPosition(book.id, 1, null, null, 11L)
        assertEquals("已读完", dao.getBookById(book.id)!!.statusBadge)
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AsterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.asterDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryBooks() = runBlocking {
        val book = BookEntity(
            id = "test_book_1",
            title = "芙莉莲第 1 卷",
            author = "山田钟人",
            series = "葬送的芙莉莲",
            format = "CBZ",
            coverUrl = "https://example.com/cover.jpg",
            currentPage = 10,
            totalPages = 100,
            progressPercent = 10,
            currentChapterTitle = "第 1 话",
            statusBadge = "在读",
            seriesBadge = "卷 1",
            fileSizeText = "50 MB",
            filePath = "/storage/emulated/0/Comics/frieren_1.cbz",
            repositoryName = "内部存储 / Books",
            updatedTimeText = "刚刚",
            synopsis = "冒险的起点",
            tagsCsv = "奇幻,日漫",
            shelfIdsCsv = "shelf_1",
            isFavorite = true,
            isVaultProtected = false,
            isVaultEncrypted = false,
            lastReadTimestamp = 1000L
        )

        dao.insertBook(book)
        val count = dao.getBookCount()
        assertEquals(1, count)

        val retrieved = dao.getBookById("test_book_1")
        assertNotNull(retrieved)
        assertEquals("芙莉莲第 1 卷", retrieved?.title)
        assertTrue(retrieved?.isFavorite == true)

        val list = dao.observeAllBooks().first()
        assertEquals(1, list.size)
        assertEquals("test_book_1", list[0].id)
    }

    @Test
    fun updateAndDeleteBooks() = runBlocking {
        val book = BookEntity(
            id = "test_book_2",
            title = "迷宫饭",
            author = "九井谅子",
            series = "迷宫饭",
            format = "CBZ",
            coverUrl = "https://example.com/meshi.jpg",
            currentPage = 1,
            totalPages = 200,
            progressPercent = 0,
            currentChapterTitle = "序章",
            statusBadge = "未读",
            seriesBadge = "连载中",
            fileSizeText = "12 MB",
            filePath = "/storage/emulated/0/Comics/meshi.cbz",
            repositoryName = "内部存储 / Books",
            updatedTimeText = "昨天",
            synopsis = "舌尖上的地下城",
            tagsCsv = "美食,奇幻",
            shelfIdsCsv = ""
        )

        dao.insertBook(book)
        dao.updateBook(book.copy(progressPercent = 50, statusBadge = "在读"))

        val updated = dao.getBookById("test_book_2")
        assertEquals(50, updated?.progressPercent)
        assertEquals("在读", updated?.statusBadge)

        dao.deleteBooksByIds(listOf("test_book_2"))
        val deleted = dao.getBookById("test_book_2")
        assertNull(deleted)
    }

    @Test
    fun lockedSearchExcludesProtectedAndOrphanAnnotations() = runBlocking {
        val publicBook = comicFixture().copy(id = "公开", isVaultProtected = false, isVaultEncrypted = false)
        val protectedBook = publicBook.copy(id = "私密", isVaultProtected = true)
        dao.insertBooks(listOf(publicBook, protectedBook))
        listOf("公开", "私密", "孤立").forEach { id ->
            dao.insertAnnotation(AnnotationEntity(bookId = id, chapterTitle = "章节", pageNumber = 1,
                quoteText = "敏感文字", noteText = "笔记", colorName = "YELLOW", type = "NOTE", createdAtText = "现在"))
        }
        assertEquals(listOf("公开"), dao.observeVisibleAnnotations(false).first().map { it.bookId })
        assertEquals(setOf("公开", "私密"), dao.observeVisibleAnnotations(true).first().map { it.bookId }.toSet())
        assertEquals(listOf("公开"), dao.observePublicBooks().first().map { it.id })
    }

    @Test
    fun removingBookRemovesChaptersAndBookmarksInSameTransaction() = runBlocking {
        val book = comicFixture().copy(id = "待移除")
        dao.importPublication(book, listOf(ChapterEntity("章节", book.id, 1, "正文", 1, 2, false, true)))
        dao.insertAnnotation(AnnotationEntity(bookId = book.id, chapterTitle = "正文", pageNumber = 2,
            quoteText = "书签", noteText = "", colorName = "YELLOW", type = "BOOKMARK", createdAtText = "现在"))
        dao.removeBooks(listOf(book.id))
        assertNull(dao.getBookById(book.id))
        assertTrue(dao.getChapters(book.id).isEmpty())
        assertTrue(dao.observeAllAnnotations().first().isEmpty())
    }

    @Test
    fun rescanningUsesLatestUserStateAndDeduplicatesSourceInsideTransaction() = runBlocking {
        val original = comicFixture().copy(id = "扫描", filePath = "content://comics/one", totalPages = 20)
        dao.importComic(original)
        dao.toggleFavorite(original.id)
        dao.updateProgress(original.id, 8, 40, "在读", "第 8 页", 42)
        val refreshed = dao.importComic(original.copy(id = "并发导入", totalPages = 40, sizeBytes = 128))
        assertEquals(original.id, refreshed.id)
        assertTrue(refreshed.isFavorite)
        assertEquals(8, refreshed.currentPage)
        assertEquals(20, refreshed.progressPercent)
        assertEquals(42L, refreshed.lastReadTimestamp)
        assertEquals(128L, refreshed.sizeBytes)
        assertEquals(1, dao.getBookCount())
        val shortened = dao.importComic(original.copy(totalPages = 2))
        assertEquals(2, shortened.currentPage)
        assertEquals("第 2 页", shortened.currentChapterTitle)
        assertEquals("已读完", shortened.statusBadge)
        val extended = dao.importComic(original.copy(totalPages = 40))
        assertEquals(5, extended.progressPercent)
        assertEquals("在读", extended.statusBadge)
    }

    @Test
    fun progressUpdateDoesNotOverwriteConcurrentFavorite() = runBlocking {
        val book = comicFixture().copy(id = "并发", isFavorite = false)
        dao.insertBook(book)
        dao.toggleFavorite(book.id)
        dao.updateProgress(book.id, 8, 50, "在读", "新章节", 42)
        assertTrue(dao.getBookById(book.id)!!.isFavorite)
        assertEquals(8, dao.getBookById(book.id)!!.currentPage)
        dao.insertAnnotation(AnnotationEntity(bookId = book.id, chapterTitle = "正文", pageNumber = 8,
            quoteText = "书签", noteText = "", colorName = "YELLOW", type = "BOOKMARK", createdAtText = "现在"))
        dao.removeBookmark(book.id, 8)
        dao.removeBookmark(book.id, 8)
        assertTrue(dao.observeAnnotationsForBook(book.id).first().isEmpty())
    }

    @Test
    fun freshComicSchemaIncludesQueryIndexes() {
        val names = mutableSetOf<String>()
        database.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { cursor ->
            while (cursor.moveToNext()) names.add(cursor.getString(0))
        }
        assertTrue(names.contains("index_books_filePath"))
        assertTrue(names.contains("index_annotations_bookId_pageNumber_type"))
    }
}
