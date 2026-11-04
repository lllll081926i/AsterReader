package com.asterreader.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AsterDatabase_Impl : AsterDatabase() {
  private val _asterDao: Lazy<AsterDao> = lazy {
    AsterDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "9fab5df7c3ce9d78b235a00258221556", "dada8ba2d36fae32eae13f7505aaf8dd") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `books` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT NOT NULL, `series` TEXT NOT NULL, `format` TEXT NOT NULL, `coverUrl` TEXT NOT NULL, `altCoverUrl` TEXT NOT NULL, `currentPage` INTEGER NOT NULL, `totalPages` INTEGER NOT NULL, `progressPercent` INTEGER NOT NULL, `currentChapterTitle` TEXT NOT NULL, `statusBadge` TEXT NOT NULL, `seriesBadge` TEXT NOT NULL, `fileSizeText` TEXT NOT NULL, `filePath` TEXT NOT NULL, `repositoryName` TEXT NOT NULL, `updatedTimeText` TEXT NOT NULL, `synopsis` TEXT NOT NULL, `tagsCsv` TEXT NOT NULL, `shelfIdsCsv` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `isVaultProtected` INTEGER NOT NULL, `isVaultEncrypted` INTEGER NOT NULL, `lastReadTimestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `chapters` (`id` TEXT NOT NULL, `bookId` TEXT NOT NULL, `chapterNumber` INTEGER NOT NULL, `title` TEXT NOT NULL, `startPage` INTEGER NOT NULL, `endPage` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `isCurrent` INTEGER NOT NULL, `textContent` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `shelves` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `itemCountDisplay` INTEGER NOT NULL, `updatedLabel` TEXT NOT NULL, `coverUrlsCsv` TEXT NOT NULL, `isProtected` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `annotations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `bookId` TEXT NOT NULL, `chapterTitle` TEXT NOT NULL, `pageNumber` INTEGER NOT NULL, `quoteText` TEXT NOT NULL, `noteText` TEXT NOT NULL, `colorName` TEXT NOT NULL, `type` TEXT NOT NULL, `createdAtText` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `repositories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `pathUri` TEXT NOT NULL, `storageType` TEXT NOT NULL, `bookCount` INTEGER NOT NULL, `sizeText` TEXT NOT NULL, `lastScanText` TEXT NOT NULL, `scanRule` TEXT NOT NULL, `isHealthy` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9fab5df7c3ce9d78b235a00258221556')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `books`")
        connection.execSQL("DROP TABLE IF EXISTS `chapters`")
        connection.execSQL("DROP TABLE IF EXISTS `shelves`")
        connection.execSQL("DROP TABLE IF EXISTS `annotations`")
        connection.execSQL("DROP TABLE IF EXISTS `repositories`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsBooks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBooks.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("author", TableInfo.Column("author", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("series", TableInfo.Column("series", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("format", TableInfo.Column("format", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("coverUrl", TableInfo.Column("coverUrl", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("altCoverUrl", TableInfo.Column("altCoverUrl", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("currentPage", TableInfo.Column("currentPage", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("totalPages", TableInfo.Column("totalPages", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("progressPercent", TableInfo.Column("progressPercent", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("currentChapterTitle", TableInfo.Column("currentChapterTitle", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("statusBadge", TableInfo.Column("statusBadge", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("seriesBadge", TableInfo.Column("seriesBadge", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("fileSizeText", TableInfo.Column("fileSizeText", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("filePath", TableInfo.Column("filePath", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("repositoryName", TableInfo.Column("repositoryName", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("updatedTimeText", TableInfo.Column("updatedTimeText", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("synopsis", TableInfo.Column("synopsis", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("tagsCsv", TableInfo.Column("tagsCsv", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("shelfIdsCsv", TableInfo.Column("shelfIdsCsv", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("isFavorite", TableInfo.Column("isFavorite", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("isVaultProtected", TableInfo.Column("isVaultProtected", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("isVaultEncrypted", TableInfo.Column("isVaultEncrypted", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBooks.put("lastReadTimestamp", TableInfo.Column("lastReadTimestamp", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBooks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBooks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBooks: TableInfo = TableInfo("books", _columnsBooks, _foreignKeysBooks,
            _indicesBooks)
        val _existingBooks: TableInfo = read(connection, "books")
        if (!_infoBooks.equals(_existingBooks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |books(com.asterreader.core.database.BookEntity).
              | Expected:
              |""".trimMargin() + _infoBooks + """
              |
              | Found:
              |""".trimMargin() + _existingBooks)
        }
        val _columnsChapters: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsChapters.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("bookId", TableInfo.Column("bookId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("chapterNumber", TableInfo.Column("chapterNumber", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("startPage", TableInfo.Column("startPage", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("endPage", TableInfo.Column("endPage", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("isCompleted", TableInfo.Column("isCompleted", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("isCurrent", TableInfo.Column("isCurrent", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsChapters.put("textContent", TableInfo.Column("textContent", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysChapters: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesChapters: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoChapters: TableInfo = TableInfo("chapters", _columnsChapters, _foreignKeysChapters,
            _indicesChapters)
        val _existingChapters: TableInfo = read(connection, "chapters")
        if (!_infoChapters.equals(_existingChapters)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |chapters(com.asterreader.core.database.ChapterEntity).
              | Expected:
              |""".trimMargin() + _infoChapters + """
              |
              | Found:
              |""".trimMargin() + _existingChapters)
        }
        val _columnsShelves: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsShelves.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("subtitle", TableInfo.Column("subtitle", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("itemCountDisplay", TableInfo.Column("itemCountDisplay", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("updatedLabel", TableInfo.Column("updatedLabel", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("coverUrlsCsv", TableInfo.Column("coverUrlsCsv", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("isProtected", TableInfo.Column("isProtected", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsShelves.put("sortOrder", TableInfo.Column("sortOrder", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysShelves: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesShelves: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoShelves: TableInfo = TableInfo("shelves", _columnsShelves, _foreignKeysShelves,
            _indicesShelves)
        val _existingShelves: TableInfo = read(connection, "shelves")
        if (!_infoShelves.equals(_existingShelves)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |shelves(com.asterreader.core.database.ShelfEntity).
              | Expected:
              |""".trimMargin() + _infoShelves + """
              |
              | Found:
              |""".trimMargin() + _existingShelves)
        }
        val _columnsAnnotations: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAnnotations.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("bookId", TableInfo.Column("bookId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("chapterTitle", TableInfo.Column("chapterTitle", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("pageNumber", TableInfo.Column("pageNumber", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("quoteText", TableInfo.Column("quoteText", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("noteText", TableInfo.Column("noteText", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("colorName", TableInfo.Column("colorName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAnnotations.put("createdAtText", TableInfo.Column("createdAtText", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAnnotations: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAnnotations: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAnnotations: TableInfo = TableInfo("annotations", _columnsAnnotations,
            _foreignKeysAnnotations, _indicesAnnotations)
        val _existingAnnotations: TableInfo = read(connection, "annotations")
        if (!_infoAnnotations.equals(_existingAnnotations)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |annotations(com.asterreader.core.database.AnnotationEntity).
              | Expected:
              |""".trimMargin() + _infoAnnotations + """
              |
              | Found:
              |""".trimMargin() + _existingAnnotations)
        }
        val _columnsRepositories: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRepositories.put("id", TableInfo.Column("id", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("pathUri", TableInfo.Column("pathUri", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("storageType", TableInfo.Column("storageType", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("bookCount", TableInfo.Column("bookCount", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("sizeText", TableInfo.Column("sizeText", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("lastScanText", TableInfo.Column("lastScanText", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("scanRule", TableInfo.Column("scanRule", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRepositories.put("isHealthy", TableInfo.Column("isHealthy", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRepositories: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRepositories: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRepositories: TableInfo = TableInfo("repositories", _columnsRepositories,
            _foreignKeysRepositories, _indicesRepositories)
        val _existingRepositories: TableInfo = read(connection, "repositories")
        if (!_infoRepositories.equals(_existingRepositories)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |repositories(com.asterreader.core.database.RepositoryEntity).
              | Expected:
              |""".trimMargin() + _infoRepositories + """
              |
              | Found:
              |""".trimMargin() + _existingRepositories)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "books", "chapters", "shelves",
        "annotations", "repositories")
  }

  public override fun clearAllTables() {
    super.performClear(false, "books", "chapters", "shelves", "annotations", "repositories")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(AsterDao::class, AsterDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun asterDao(): AsterDao = _asterDao.value
}
