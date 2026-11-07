package com.asterreader.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ShelfEntity::class,
        AnnotationEntity::class,
        RepositoryEntity::class,
        ReaderStateEntity::class,
        ComicPageEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AsterDatabase : RoomDatabase() {
    abstract fun asterDao(): AsterDao

    companion object {
        @Volatile
        private var INSTANCE: AsterDatabase? = null

        /**
         * v1 → v2：新增阅读状态表、散图页索引表，以及 books 的 5 个来源/系列字段。
         * SQL 与 schemas/2.json 完全一致；缺了它，装过 v1 的用户升级会直接崩溃。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrationSql().forEach(db::execSQL)
            }
        }

        /** 迁移 SQL 单独导出，便于单元测试直接验证表结构与索引。 */
        fun migrationSql(): List<String> = listOf(
            "ALTER TABLE `books` ADD COLUMN `repositoryId` TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE `books` ADD COLUMN `sourceStatus` TEXT NOT NULL DEFAULT 'AVAILABLE'",
            "ALTER TABLE `books` ADD COLUMN `contentFingerprint` TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE `books` ADD COLUMN `coverPage` INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE `books` ADD COLUMN `seriesOrder` INTEGER NOT NULL DEFAULT 0",
            "CREATE INDEX IF NOT EXISTS `index_books_repositoryId` ON `books` (`repositoryId`)",
            "CREATE TABLE IF NOT EXISTS `reader_states` (`bookId` TEXT NOT NULL, `page` INTEGER NOT NULL, " +
                "`offsetFraction` REAL NOT NULL, `scale` REAL NOT NULL, `panX` REAL NOT NULL, `panY` REAL NOT NULL, " +
                "`layoutMode` TEXT, `fitMode` TEXT, `coverSingle` INTEGER, `rightToLeft` INTEGER, " +
                "PRIMARY KEY(`bookId`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `comic_pages` (`bookId` TEXT NOT NULL, `number` INTEGER NOT NULL, " +
                "`uri` TEXT NOT NULL, `name` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `modifiedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`bookId`, `number`), FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        fun getDatabase(context: Context): AsterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsterDatabase::class.java,
                    "aster_comics_v2.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
