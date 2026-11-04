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
            format = "EPUB",
            coverUrl = "https://example.com/meshi.jpg",
            currentPage = 1,
            totalPages = 200,
            progressPercent = 0,
            currentChapterTitle = "序章",
            statusBadge = "未读",
            seriesBadge = "连载中",
            fileSizeText = "12 MB",
            filePath = "/storage/emulated/0/Books/meshi.epub",
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
}
