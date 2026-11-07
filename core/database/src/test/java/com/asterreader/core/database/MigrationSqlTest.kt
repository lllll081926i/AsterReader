package com.asterreader.core.database

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 验证 v1→v2 迁移 SQL：装过 v1 的用户升级时 Room 会执行这份 SQL。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationSqlTest {

    @Test
    fun migrationCreatesAllNewColumnsTablesAndIndex() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = context.getDatabasePath("migration-check.db")
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            // 最小 v1 books 表（迁移只新增列，不重建表）
            db.execSQL("CREATE TABLE `books` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("INSERT INTO `books` (`id`, `title`) VALUES ('bk1', '旧版漫画')")

            AsterDatabase.migrationSql().forEach { db.execSQL(it) }

            val columns = mutableSetOf<String>()
            db.rawQuery("PRAGMA table_info(`books`)", null).use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) columns.add(cursor.getString(nameIndex))
            }
            assertTrue(
                columns.containsAll(
                    listOf("repositoryId", "sourceStatus", "contentFingerprint", "coverPage", "seriesOrder")
                )
            )

            val objects = mutableSetOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type IN ('table','index')", null).use { cursor ->
                while (cursor.moveToNext()) objects.add(cursor.getString(0))
            }
            assertTrue(objects.contains("reader_states"))
            assertTrue(objects.contains("comic_pages"))
            assertTrue(objects.contains("index_books_repositoryId"))

            // 旧数据必须保留，新增字段有默认值
            db.rawQuery(
                "SELECT title, sourceStatus, coverPage FROM `books` WHERE id = 'bk1'", null
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("旧版漫画", cursor.getString(0))
                assertEquals("AVAILABLE", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
            }
        } finally {
            db.close()
            file.delete()
        }
    }

    @Test
    fun registeredMigrationCoversV1ToV2() {
        assertEquals(1, AsterDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AsterDatabase.MIGRATION_1_2.endVersion)
    }
}
