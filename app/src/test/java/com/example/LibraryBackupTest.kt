package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AsterDao
import com.example.data.local.AsterDatabase
import com.example.data.local.LibraryBackup
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** 书库导出 / 恢复的往返验证：换机后进度不丢，失效来源标记 MISSING。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibraryBackupTest {

    private lateinit var database: AsterDatabase
    private lateinit var dao: AsterDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
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
    fun exportThenImportRestoresProgressAndMarksSourceMissing() = runBlocking {
        dao.insertBook(
            comicFixture("bk1").copy(
                currentPage = 42, progressPercent = 42, isFavorite = true,
                tagsCsv = "奇幻,冒险", currentChapterTitle = "第 42 页"
            )
        )
        val buffer = ByteArrayOutputStream()
        LibraryBackup.export(dao, buffer)
        val json = String(buffer.toByteArray(), Charsets.UTF_8)
        assertTrue(json.contains("aster-library"))
        assertTrue(json.contains("bk1"))
        assertTrue(!json.contains("pinHash")) // 绝不能把 PIN 写进备份

        // 模拟换机：本机书库为空
        dao.removeBooks(listOf("bk1"))
        val result = LibraryBackup.import(dao, ByteArrayInputStream(buffer.toByteArray()))
        assertEquals(1, result.restored)
        assertEquals(0, result.updated)

        val restored = dao.getBookById("bk1") ?: error("作品未恢复")
        assertEquals(42, restored.currentPage)
        assertEquals(42, restored.progressPercent)
        assertTrue(restored.isFavorite)
        assertEquals("奇幻,冒险", restored.tagsCsv)
        assertEquals("MISSING", restored.sourceStatus)
    }

    @Test
    fun importKeepsLocalCoverAndSourceStatus() = runBlocking {
        dao.insertBook(comicFixture("bk2").copy(currentPage = 1, coverUrl = "local-cover.jpg"))
        val buffer = ByteArrayOutputStream()
        LibraryBackup.export(dao, buffer)
        // 导出后再推进本机进度；导入旧备份不应回退进度，但仍保留本机封面
        dao.updateProgress("bk2", 20, 20, "在读", null, System.currentTimeMillis())
        LibraryBackup.import(dao, ByteArrayInputStream(buffer.toByteArray()))
        val book = dao.getBookById("bk2") ?: error("作品丢失")
        assertEquals("local-cover.jpg", book.coverUrl)
        assertEquals("AVAILABLE", book.sourceStatus)
    }

    @Test
    fun arbitraryJsonIsRejected() = runBlocking {
        val outcome = runCatching {
            LibraryBackup.import(dao, ByteArrayInputStream("{}".toByteArray(Charsets.UTF_8)))
        }
        assertTrue(outcome.isFailure)
    }
}
