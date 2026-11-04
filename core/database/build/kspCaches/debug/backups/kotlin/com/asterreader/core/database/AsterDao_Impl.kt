package com.asterreader.core.database

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AsterDao_Impl(
  __db: RoomDatabase,
) : AsterDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBookEntity: EntityInsertAdapter<BookEntity>

  private val __insertAdapterOfChapterEntity: EntityInsertAdapter<ChapterEntity>

  private val __insertAdapterOfShelfEntity: EntityInsertAdapter<ShelfEntity>

  private val __insertAdapterOfAnnotationEntity: EntityInsertAdapter<AnnotationEntity>

  private val __insertAdapterOfRepositoryEntity: EntityInsertAdapter<RepositoryEntity>

  private val __updateAdapterOfBookEntity: EntityDeleteOrUpdateAdapter<BookEntity>

  private val __updateAdapterOfChapterEntity: EntityDeleteOrUpdateAdapter<ChapterEntity>

  private val __updateAdapterOfShelfEntity: EntityDeleteOrUpdateAdapter<ShelfEntity>

  private val __updateAdapterOfRepositoryEntity: EntityDeleteOrUpdateAdapter<RepositoryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBookEntity = object : EntityInsertAdapter<BookEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `books` (`id`,`title`,`author`,`series`,`format`,`coverUrl`,`altCoverUrl`,`currentPage`,`totalPages`,`progressPercent`,`currentChapterTitle`,`statusBadge`,`seriesBadge`,`fileSizeText`,`filePath`,`repositoryName`,`updatedTimeText`,`synopsis`,`tagsCsv`,`shelfIdsCsv`,`isFavorite`,`isVaultProtected`,`isVaultEncrypted`,`lastReadTimestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BookEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.author)
        statement.bindText(4, entity.series)
        statement.bindText(5, entity.format)
        statement.bindText(6, entity.coverUrl)
        statement.bindText(7, entity.altCoverUrl)
        statement.bindLong(8, entity.currentPage.toLong())
        statement.bindLong(9, entity.totalPages.toLong())
        statement.bindLong(10, entity.progressPercent.toLong())
        statement.bindText(11, entity.currentChapterTitle)
        statement.bindText(12, entity.statusBadge)
        statement.bindText(13, entity.seriesBadge)
        statement.bindText(14, entity.fileSizeText)
        statement.bindText(15, entity.filePath)
        statement.bindText(16, entity.repositoryName)
        statement.bindText(17, entity.updatedTimeText)
        statement.bindText(18, entity.synopsis)
        statement.bindText(19, entity.tagsCsv)
        statement.bindText(20, entity.shelfIdsCsv)
        val _tmp: Int = if (entity.isFavorite) 1 else 0
        statement.bindLong(21, _tmp.toLong())
        val _tmp_1: Int = if (entity.isVaultProtected) 1 else 0
        statement.bindLong(22, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.isVaultEncrypted) 1 else 0
        statement.bindLong(23, _tmp_2.toLong())
        statement.bindLong(24, entity.lastReadTimestamp)
      }
    }
    this.__insertAdapterOfChapterEntity = object : EntityInsertAdapter<ChapterEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `chapters` (`id`,`bookId`,`chapterNumber`,`title`,`startPage`,`endPage`,`isCompleted`,`isCurrent`,`textContent`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChapterEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.bookId)
        statement.bindLong(3, entity.chapterNumber.toLong())
        statement.bindText(4, entity.title)
        statement.bindLong(5, entity.startPage.toLong())
        statement.bindLong(6, entity.endPage.toLong())
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        val _tmp_1: Int = if (entity.isCurrent) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        statement.bindText(9, entity.textContent)
      }
    }
    this.__insertAdapterOfShelfEntity = object : EntityInsertAdapter<ShelfEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `shelves` (`id`,`name`,`subtitle`,`itemCountDisplay`,`updatedLabel`,`coverUrlsCsv`,`isProtected`,`sortOrder`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ShelfEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.subtitle)
        statement.bindLong(4, entity.itemCountDisplay.toLong())
        statement.bindText(5, entity.updatedLabel)
        statement.bindText(6, entity.coverUrlsCsv)
        val _tmp: Int = if (entity.isProtected) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindLong(8, entity.sortOrder.toLong())
      }
    }
    this.__insertAdapterOfAnnotationEntity = object : EntityInsertAdapter<AnnotationEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `annotations` (`id`,`bookId`,`chapterTitle`,`pageNumber`,`quoteText`,`noteText`,`colorName`,`type`,`createdAtText`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AnnotationEntity) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.bookId)
        statement.bindText(3, entity.chapterTitle)
        statement.bindLong(4, entity.pageNumber.toLong())
        statement.bindText(5, entity.quoteText)
        statement.bindText(6, entity.noteText)
        statement.bindText(7, entity.colorName)
        statement.bindText(8, entity.type)
        statement.bindText(9, entity.createdAtText)
      }
    }
    this.__insertAdapterOfRepositoryEntity = object : EntityInsertAdapter<RepositoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `repositories` (`id`,`name`,`pathUri`,`storageType`,`bookCount`,`sizeText`,`lastScanText`,`scanRule`,`isHealthy`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RepositoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.pathUri)
        statement.bindText(4, entity.storageType)
        statement.bindLong(5, entity.bookCount.toLong())
        statement.bindText(6, entity.sizeText)
        statement.bindText(7, entity.lastScanText)
        statement.bindText(8, entity.scanRule)
        val _tmp: Int = if (entity.isHealthy) 1 else 0
        statement.bindLong(9, _tmp.toLong())
      }
    }
    this.__updateAdapterOfBookEntity = object : EntityDeleteOrUpdateAdapter<BookEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `books` SET `id` = ?,`title` = ?,`author` = ?,`series` = ?,`format` = ?,`coverUrl` = ?,`altCoverUrl` = ?,`currentPage` = ?,`totalPages` = ?,`progressPercent` = ?,`currentChapterTitle` = ?,`statusBadge` = ?,`seriesBadge` = ?,`fileSizeText` = ?,`filePath` = ?,`repositoryName` = ?,`updatedTimeText` = ?,`synopsis` = ?,`tagsCsv` = ?,`shelfIdsCsv` = ?,`isFavorite` = ?,`isVaultProtected` = ?,`isVaultEncrypted` = ?,`lastReadTimestamp` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BookEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.author)
        statement.bindText(4, entity.series)
        statement.bindText(5, entity.format)
        statement.bindText(6, entity.coverUrl)
        statement.bindText(7, entity.altCoverUrl)
        statement.bindLong(8, entity.currentPage.toLong())
        statement.bindLong(9, entity.totalPages.toLong())
        statement.bindLong(10, entity.progressPercent.toLong())
        statement.bindText(11, entity.currentChapterTitle)
        statement.bindText(12, entity.statusBadge)
        statement.bindText(13, entity.seriesBadge)
        statement.bindText(14, entity.fileSizeText)
        statement.bindText(15, entity.filePath)
        statement.bindText(16, entity.repositoryName)
        statement.bindText(17, entity.updatedTimeText)
        statement.bindText(18, entity.synopsis)
        statement.bindText(19, entity.tagsCsv)
        statement.bindText(20, entity.shelfIdsCsv)
        val _tmp: Int = if (entity.isFavorite) 1 else 0
        statement.bindLong(21, _tmp.toLong())
        val _tmp_1: Int = if (entity.isVaultProtected) 1 else 0
        statement.bindLong(22, _tmp_1.toLong())
        val _tmp_2: Int = if (entity.isVaultEncrypted) 1 else 0
        statement.bindLong(23, _tmp_2.toLong())
        statement.bindLong(24, entity.lastReadTimestamp)
        statement.bindText(25, entity.id)
      }
    }
    this.__updateAdapterOfChapterEntity = object : EntityDeleteOrUpdateAdapter<ChapterEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `chapters` SET `id` = ?,`bookId` = ?,`chapterNumber` = ?,`title` = ?,`startPage` = ?,`endPage` = ?,`isCompleted` = ?,`isCurrent` = ?,`textContent` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChapterEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.bookId)
        statement.bindLong(3, entity.chapterNumber.toLong())
        statement.bindText(4, entity.title)
        statement.bindLong(5, entity.startPage.toLong())
        statement.bindLong(6, entity.endPage.toLong())
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        val _tmp_1: Int = if (entity.isCurrent) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        statement.bindText(9, entity.textContent)
        statement.bindText(10, entity.id)
      }
    }
    this.__updateAdapterOfShelfEntity = object : EntityDeleteOrUpdateAdapter<ShelfEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `shelves` SET `id` = ?,`name` = ?,`subtitle` = ?,`itemCountDisplay` = ?,`updatedLabel` = ?,`coverUrlsCsv` = ?,`isProtected` = ?,`sortOrder` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ShelfEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.subtitle)
        statement.bindLong(4, entity.itemCountDisplay.toLong())
        statement.bindText(5, entity.updatedLabel)
        statement.bindText(6, entity.coverUrlsCsv)
        val _tmp: Int = if (entity.isProtected) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindLong(8, entity.sortOrder.toLong())
        statement.bindText(9, entity.id)
      }
    }
    this.__updateAdapterOfRepositoryEntity = object :
        EntityDeleteOrUpdateAdapter<RepositoryEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `repositories` SET `id` = ?,`name` = ?,`pathUri` = ?,`storageType` = ?,`bookCount` = ?,`sizeText` = ?,`lastScanText` = ?,`scanRule` = ?,`isHealthy` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: RepositoryEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.pathUri)
        statement.bindText(4, entity.storageType)
        statement.bindLong(5, entity.bookCount.toLong())
        statement.bindText(6, entity.sizeText)
        statement.bindText(7, entity.lastScanText)
        statement.bindText(8, entity.scanRule)
        val _tmp: Int = if (entity.isHealthy) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindText(10, entity.id)
      }
    }
  }

  public override suspend fun insertBooks(books: List<BookEntity>): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfBookEntity.insert(_connection, books)
  }

  public override suspend fun insertBook(book: BookEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfBookEntity.insert(_connection, book)
  }

  public override suspend fun insertChapters(chapters: List<ChapterEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfChapterEntity.insert(_connection, chapters)
  }

  public override suspend fun insertShelves(shelves: List<ShelfEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfShelfEntity.insert(_connection, shelves)
  }

  public override suspend fun insertShelf(shelf: ShelfEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfShelfEntity.insert(_connection, shelf)
  }

  public override suspend fun insertAnnotations(annotations: List<AnnotationEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAnnotationEntity.insert(_connection, annotations)
  }

  public override suspend fun insertAnnotation(`annotation`: AnnotationEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAnnotationEntity.insert(_connection, annotation)
  }

  public override suspend fun insertRepositories(repos: List<RepositoryEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRepositoryEntity.insert(_connection, repos)
  }

  public override suspend fun insertRepository(repo: RepositoryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRepositoryEntity.insert(_connection, repo)
  }

  public override suspend fun updateBook(book: BookEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfBookEntity.handle(_connection, book)
  }

  public override suspend fun updateChapter(chapter: ChapterEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfChapterEntity.handle(_connection, chapter)
  }

  public override suspend fun updateShelf(shelf: ShelfEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfShelfEntity.handle(_connection, shelf)
  }

  public override suspend fun updateRepository(repo: RepositoryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfRepositoryEntity.handle(_connection, repo)
  }

  public override fun observeAllBooks(): Flow<List<BookEntity>> {
    val _sql: String = "SELECT * FROM books ORDER BY lastReadTimestamp DESC"
    return createFlow(__db, false, arrayOf("books")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfSeries: Int = getColumnIndexOrThrow(_stmt, "series")
        val _columnIndexOfFormat: Int = getColumnIndexOrThrow(_stmt, "format")
        val _columnIndexOfCoverUrl: Int = getColumnIndexOrThrow(_stmt, "coverUrl")
        val _columnIndexOfAltCoverUrl: Int = getColumnIndexOrThrow(_stmt, "altCoverUrl")
        val _columnIndexOfCurrentPage: Int = getColumnIndexOrThrow(_stmt, "currentPage")
        val _columnIndexOfTotalPages: Int = getColumnIndexOrThrow(_stmt, "totalPages")
        val _columnIndexOfProgressPercent: Int = getColumnIndexOrThrow(_stmt, "progressPercent")
        val _columnIndexOfCurrentChapterTitle: Int = getColumnIndexOrThrow(_stmt,
            "currentChapterTitle")
        val _columnIndexOfStatusBadge: Int = getColumnIndexOrThrow(_stmt, "statusBadge")
        val _columnIndexOfSeriesBadge: Int = getColumnIndexOrThrow(_stmt, "seriesBadge")
        val _columnIndexOfFileSizeText: Int = getColumnIndexOrThrow(_stmt, "fileSizeText")
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfRepositoryName: Int = getColumnIndexOrThrow(_stmt, "repositoryName")
        val _columnIndexOfUpdatedTimeText: Int = getColumnIndexOrThrow(_stmt, "updatedTimeText")
        val _columnIndexOfSynopsis: Int = getColumnIndexOrThrow(_stmt, "synopsis")
        val _columnIndexOfTagsCsv: Int = getColumnIndexOrThrow(_stmt, "tagsCsv")
        val _columnIndexOfShelfIdsCsv: Int = getColumnIndexOrThrow(_stmt, "shelfIdsCsv")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfIsVaultProtected: Int = getColumnIndexOrThrow(_stmt, "isVaultProtected")
        val _columnIndexOfIsVaultEncrypted: Int = getColumnIndexOrThrow(_stmt, "isVaultEncrypted")
        val _columnIndexOfLastReadTimestamp: Int = getColumnIndexOrThrow(_stmt, "lastReadTimestamp")
        val _result: MutableList<BookEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpSeries: String
          _tmpSeries = _stmt.getText(_columnIndexOfSeries)
          val _tmpFormat: String
          _tmpFormat = _stmt.getText(_columnIndexOfFormat)
          val _tmpCoverUrl: String
          _tmpCoverUrl = _stmt.getText(_columnIndexOfCoverUrl)
          val _tmpAltCoverUrl: String
          _tmpAltCoverUrl = _stmt.getText(_columnIndexOfAltCoverUrl)
          val _tmpCurrentPage: Int
          _tmpCurrentPage = _stmt.getLong(_columnIndexOfCurrentPage).toInt()
          val _tmpTotalPages: Int
          _tmpTotalPages = _stmt.getLong(_columnIndexOfTotalPages).toInt()
          val _tmpProgressPercent: Int
          _tmpProgressPercent = _stmt.getLong(_columnIndexOfProgressPercent).toInt()
          val _tmpCurrentChapterTitle: String
          _tmpCurrentChapterTitle = _stmt.getText(_columnIndexOfCurrentChapterTitle)
          val _tmpStatusBadge: String
          _tmpStatusBadge = _stmt.getText(_columnIndexOfStatusBadge)
          val _tmpSeriesBadge: String
          _tmpSeriesBadge = _stmt.getText(_columnIndexOfSeriesBadge)
          val _tmpFileSizeText: String
          _tmpFileSizeText = _stmt.getText(_columnIndexOfFileSizeText)
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpRepositoryName: String
          _tmpRepositoryName = _stmt.getText(_columnIndexOfRepositoryName)
          val _tmpUpdatedTimeText: String
          _tmpUpdatedTimeText = _stmt.getText(_columnIndexOfUpdatedTimeText)
          val _tmpSynopsis: String
          _tmpSynopsis = _stmt.getText(_columnIndexOfSynopsis)
          val _tmpTagsCsv: String
          _tmpTagsCsv = _stmt.getText(_columnIndexOfTagsCsv)
          val _tmpShelfIdsCsv: String
          _tmpShelfIdsCsv = _stmt.getText(_columnIndexOfShelfIdsCsv)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpIsVaultProtected: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsVaultProtected).toInt()
          _tmpIsVaultProtected = _tmp_1 != 0
          val _tmpIsVaultEncrypted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsVaultEncrypted).toInt()
          _tmpIsVaultEncrypted = _tmp_2 != 0
          val _tmpLastReadTimestamp: Long
          _tmpLastReadTimestamp = _stmt.getLong(_columnIndexOfLastReadTimestamp)
          _item =
              BookEntity(_tmpId,_tmpTitle,_tmpAuthor,_tmpSeries,_tmpFormat,_tmpCoverUrl,_tmpAltCoverUrl,_tmpCurrentPage,_tmpTotalPages,_tmpProgressPercent,_tmpCurrentChapterTitle,_tmpStatusBadge,_tmpSeriesBadge,_tmpFileSizeText,_tmpFilePath,_tmpRepositoryName,_tmpUpdatedTimeText,_tmpSynopsis,_tmpTagsCsv,_tmpShelfIdsCsv,_tmpIsFavorite,_tmpIsVaultProtected,_tmpIsVaultEncrypted,_tmpLastReadTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeBookById(bookId: String): Flow<BookEntity?> {
    val _sql: String = "SELECT * FROM books WHERE id = ? LIMIT 1"
    return createFlow(__db, false, arrayOf("books")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, bookId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfSeries: Int = getColumnIndexOrThrow(_stmt, "series")
        val _columnIndexOfFormat: Int = getColumnIndexOrThrow(_stmt, "format")
        val _columnIndexOfCoverUrl: Int = getColumnIndexOrThrow(_stmt, "coverUrl")
        val _columnIndexOfAltCoverUrl: Int = getColumnIndexOrThrow(_stmt, "altCoverUrl")
        val _columnIndexOfCurrentPage: Int = getColumnIndexOrThrow(_stmt, "currentPage")
        val _columnIndexOfTotalPages: Int = getColumnIndexOrThrow(_stmt, "totalPages")
        val _columnIndexOfProgressPercent: Int = getColumnIndexOrThrow(_stmt, "progressPercent")
        val _columnIndexOfCurrentChapterTitle: Int = getColumnIndexOrThrow(_stmt,
            "currentChapterTitle")
        val _columnIndexOfStatusBadge: Int = getColumnIndexOrThrow(_stmt, "statusBadge")
        val _columnIndexOfSeriesBadge: Int = getColumnIndexOrThrow(_stmt, "seriesBadge")
        val _columnIndexOfFileSizeText: Int = getColumnIndexOrThrow(_stmt, "fileSizeText")
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfRepositoryName: Int = getColumnIndexOrThrow(_stmt, "repositoryName")
        val _columnIndexOfUpdatedTimeText: Int = getColumnIndexOrThrow(_stmt, "updatedTimeText")
        val _columnIndexOfSynopsis: Int = getColumnIndexOrThrow(_stmt, "synopsis")
        val _columnIndexOfTagsCsv: Int = getColumnIndexOrThrow(_stmt, "tagsCsv")
        val _columnIndexOfShelfIdsCsv: Int = getColumnIndexOrThrow(_stmt, "shelfIdsCsv")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfIsVaultProtected: Int = getColumnIndexOrThrow(_stmt, "isVaultProtected")
        val _columnIndexOfIsVaultEncrypted: Int = getColumnIndexOrThrow(_stmt, "isVaultEncrypted")
        val _columnIndexOfLastReadTimestamp: Int = getColumnIndexOrThrow(_stmt, "lastReadTimestamp")
        val _result: BookEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpSeries: String
          _tmpSeries = _stmt.getText(_columnIndexOfSeries)
          val _tmpFormat: String
          _tmpFormat = _stmt.getText(_columnIndexOfFormat)
          val _tmpCoverUrl: String
          _tmpCoverUrl = _stmt.getText(_columnIndexOfCoverUrl)
          val _tmpAltCoverUrl: String
          _tmpAltCoverUrl = _stmt.getText(_columnIndexOfAltCoverUrl)
          val _tmpCurrentPage: Int
          _tmpCurrentPage = _stmt.getLong(_columnIndexOfCurrentPage).toInt()
          val _tmpTotalPages: Int
          _tmpTotalPages = _stmt.getLong(_columnIndexOfTotalPages).toInt()
          val _tmpProgressPercent: Int
          _tmpProgressPercent = _stmt.getLong(_columnIndexOfProgressPercent).toInt()
          val _tmpCurrentChapterTitle: String
          _tmpCurrentChapterTitle = _stmt.getText(_columnIndexOfCurrentChapterTitle)
          val _tmpStatusBadge: String
          _tmpStatusBadge = _stmt.getText(_columnIndexOfStatusBadge)
          val _tmpSeriesBadge: String
          _tmpSeriesBadge = _stmt.getText(_columnIndexOfSeriesBadge)
          val _tmpFileSizeText: String
          _tmpFileSizeText = _stmt.getText(_columnIndexOfFileSizeText)
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpRepositoryName: String
          _tmpRepositoryName = _stmt.getText(_columnIndexOfRepositoryName)
          val _tmpUpdatedTimeText: String
          _tmpUpdatedTimeText = _stmt.getText(_columnIndexOfUpdatedTimeText)
          val _tmpSynopsis: String
          _tmpSynopsis = _stmt.getText(_columnIndexOfSynopsis)
          val _tmpTagsCsv: String
          _tmpTagsCsv = _stmt.getText(_columnIndexOfTagsCsv)
          val _tmpShelfIdsCsv: String
          _tmpShelfIdsCsv = _stmt.getText(_columnIndexOfShelfIdsCsv)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpIsVaultProtected: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsVaultProtected).toInt()
          _tmpIsVaultProtected = _tmp_1 != 0
          val _tmpIsVaultEncrypted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsVaultEncrypted).toInt()
          _tmpIsVaultEncrypted = _tmp_2 != 0
          val _tmpLastReadTimestamp: Long
          _tmpLastReadTimestamp = _stmt.getLong(_columnIndexOfLastReadTimestamp)
          _result =
              BookEntity(_tmpId,_tmpTitle,_tmpAuthor,_tmpSeries,_tmpFormat,_tmpCoverUrl,_tmpAltCoverUrl,_tmpCurrentPage,_tmpTotalPages,_tmpProgressPercent,_tmpCurrentChapterTitle,_tmpStatusBadge,_tmpSeriesBadge,_tmpFileSizeText,_tmpFilePath,_tmpRepositoryName,_tmpUpdatedTimeText,_tmpSynopsis,_tmpTagsCsv,_tmpShelfIdsCsv,_tmpIsFavorite,_tmpIsVaultProtected,_tmpIsVaultEncrypted,_tmpLastReadTimestamp)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBookById(bookId: String): BookEntity? {
    val _sql: String = "SELECT * FROM books WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, bookId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfAuthor: Int = getColumnIndexOrThrow(_stmt, "author")
        val _columnIndexOfSeries: Int = getColumnIndexOrThrow(_stmt, "series")
        val _columnIndexOfFormat: Int = getColumnIndexOrThrow(_stmt, "format")
        val _columnIndexOfCoverUrl: Int = getColumnIndexOrThrow(_stmt, "coverUrl")
        val _columnIndexOfAltCoverUrl: Int = getColumnIndexOrThrow(_stmt, "altCoverUrl")
        val _columnIndexOfCurrentPage: Int = getColumnIndexOrThrow(_stmt, "currentPage")
        val _columnIndexOfTotalPages: Int = getColumnIndexOrThrow(_stmt, "totalPages")
        val _columnIndexOfProgressPercent: Int = getColumnIndexOrThrow(_stmt, "progressPercent")
        val _columnIndexOfCurrentChapterTitle: Int = getColumnIndexOrThrow(_stmt,
            "currentChapterTitle")
        val _columnIndexOfStatusBadge: Int = getColumnIndexOrThrow(_stmt, "statusBadge")
        val _columnIndexOfSeriesBadge: Int = getColumnIndexOrThrow(_stmt, "seriesBadge")
        val _columnIndexOfFileSizeText: Int = getColumnIndexOrThrow(_stmt, "fileSizeText")
        val _columnIndexOfFilePath: Int = getColumnIndexOrThrow(_stmt, "filePath")
        val _columnIndexOfRepositoryName: Int = getColumnIndexOrThrow(_stmt, "repositoryName")
        val _columnIndexOfUpdatedTimeText: Int = getColumnIndexOrThrow(_stmt, "updatedTimeText")
        val _columnIndexOfSynopsis: Int = getColumnIndexOrThrow(_stmt, "synopsis")
        val _columnIndexOfTagsCsv: Int = getColumnIndexOrThrow(_stmt, "tagsCsv")
        val _columnIndexOfShelfIdsCsv: Int = getColumnIndexOrThrow(_stmt, "shelfIdsCsv")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfIsVaultProtected: Int = getColumnIndexOrThrow(_stmt, "isVaultProtected")
        val _columnIndexOfIsVaultEncrypted: Int = getColumnIndexOrThrow(_stmt, "isVaultEncrypted")
        val _columnIndexOfLastReadTimestamp: Int = getColumnIndexOrThrow(_stmt, "lastReadTimestamp")
        val _result: BookEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpAuthor: String
          _tmpAuthor = _stmt.getText(_columnIndexOfAuthor)
          val _tmpSeries: String
          _tmpSeries = _stmt.getText(_columnIndexOfSeries)
          val _tmpFormat: String
          _tmpFormat = _stmt.getText(_columnIndexOfFormat)
          val _tmpCoverUrl: String
          _tmpCoverUrl = _stmt.getText(_columnIndexOfCoverUrl)
          val _tmpAltCoverUrl: String
          _tmpAltCoverUrl = _stmt.getText(_columnIndexOfAltCoverUrl)
          val _tmpCurrentPage: Int
          _tmpCurrentPage = _stmt.getLong(_columnIndexOfCurrentPage).toInt()
          val _tmpTotalPages: Int
          _tmpTotalPages = _stmt.getLong(_columnIndexOfTotalPages).toInt()
          val _tmpProgressPercent: Int
          _tmpProgressPercent = _stmt.getLong(_columnIndexOfProgressPercent).toInt()
          val _tmpCurrentChapterTitle: String
          _tmpCurrentChapterTitle = _stmt.getText(_columnIndexOfCurrentChapterTitle)
          val _tmpStatusBadge: String
          _tmpStatusBadge = _stmt.getText(_columnIndexOfStatusBadge)
          val _tmpSeriesBadge: String
          _tmpSeriesBadge = _stmt.getText(_columnIndexOfSeriesBadge)
          val _tmpFileSizeText: String
          _tmpFileSizeText = _stmt.getText(_columnIndexOfFileSizeText)
          val _tmpFilePath: String
          _tmpFilePath = _stmt.getText(_columnIndexOfFilePath)
          val _tmpRepositoryName: String
          _tmpRepositoryName = _stmt.getText(_columnIndexOfRepositoryName)
          val _tmpUpdatedTimeText: String
          _tmpUpdatedTimeText = _stmt.getText(_columnIndexOfUpdatedTimeText)
          val _tmpSynopsis: String
          _tmpSynopsis = _stmt.getText(_columnIndexOfSynopsis)
          val _tmpTagsCsv: String
          _tmpTagsCsv = _stmt.getText(_columnIndexOfTagsCsv)
          val _tmpShelfIdsCsv: String
          _tmpShelfIdsCsv = _stmt.getText(_columnIndexOfShelfIdsCsv)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpIsVaultProtected: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsVaultProtected).toInt()
          _tmpIsVaultProtected = _tmp_1 != 0
          val _tmpIsVaultEncrypted: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsVaultEncrypted).toInt()
          _tmpIsVaultEncrypted = _tmp_2 != 0
          val _tmpLastReadTimestamp: Long
          _tmpLastReadTimestamp = _stmt.getLong(_columnIndexOfLastReadTimestamp)
          _result =
              BookEntity(_tmpId,_tmpTitle,_tmpAuthor,_tmpSeries,_tmpFormat,_tmpCoverUrl,_tmpAltCoverUrl,_tmpCurrentPage,_tmpTotalPages,_tmpProgressPercent,_tmpCurrentChapterTitle,_tmpStatusBadge,_tmpSeriesBadge,_tmpFileSizeText,_tmpFilePath,_tmpRepositoryName,_tmpUpdatedTimeText,_tmpSynopsis,_tmpTagsCsv,_tmpShelfIdsCsv,_tmpIsFavorite,_tmpIsVaultProtected,_tmpIsVaultEncrypted,_tmpLastReadTimestamp)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBookCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM books"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeChaptersForBook(bookId: String): Flow<List<ChapterEntity>> {
    val _sql: String = "SELECT * FROM chapters WHERE bookId = ? ORDER BY chapterNumber ASC"
    return createFlow(__db, false, arrayOf("chapters")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, bookId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBookId: Int = getColumnIndexOrThrow(_stmt, "bookId")
        val _columnIndexOfChapterNumber: Int = getColumnIndexOrThrow(_stmt, "chapterNumber")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfStartPage: Int = getColumnIndexOrThrow(_stmt, "startPage")
        val _columnIndexOfEndPage: Int = getColumnIndexOrThrow(_stmt, "endPage")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfIsCurrent: Int = getColumnIndexOrThrow(_stmt, "isCurrent")
        val _columnIndexOfTextContent: Int = getColumnIndexOrThrow(_stmt, "textContent")
        val _result: MutableList<ChapterEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChapterEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpBookId: String
          _tmpBookId = _stmt.getText(_columnIndexOfBookId)
          val _tmpChapterNumber: Int
          _tmpChapterNumber = _stmt.getLong(_columnIndexOfChapterNumber).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpStartPage: Int
          _tmpStartPage = _stmt.getLong(_columnIndexOfStartPage).toInt()
          val _tmpEndPage: Int
          _tmpEndPage = _stmt.getLong(_columnIndexOfEndPage).toInt()
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpIsCurrent: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsCurrent).toInt()
          _tmpIsCurrent = _tmp_1 != 0
          val _tmpTextContent: String
          _tmpTextContent = _stmt.getText(_columnIndexOfTextContent)
          _item =
              ChapterEntity(_tmpId,_tmpBookId,_tmpChapterNumber,_tmpTitle,_tmpStartPage,_tmpEndPage,_tmpIsCompleted,_tmpIsCurrent,_tmpTextContent)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllShelves(): Flow<List<ShelfEntity>> {
    val _sql: String = "SELECT * FROM shelves ORDER BY sortOrder ASC"
    return createFlow(__db, false, arrayOf("shelves")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfSubtitle: Int = getColumnIndexOrThrow(_stmt, "subtitle")
        val _columnIndexOfItemCountDisplay: Int = getColumnIndexOrThrow(_stmt, "itemCountDisplay")
        val _columnIndexOfUpdatedLabel: Int = getColumnIndexOrThrow(_stmt, "updatedLabel")
        val _columnIndexOfCoverUrlsCsv: Int = getColumnIndexOrThrow(_stmt, "coverUrlsCsv")
        val _columnIndexOfIsProtected: Int = getColumnIndexOrThrow(_stmt, "isProtected")
        val _columnIndexOfSortOrder: Int = getColumnIndexOrThrow(_stmt, "sortOrder")
        val _result: MutableList<ShelfEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ShelfEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpSubtitle: String
          _tmpSubtitle = _stmt.getText(_columnIndexOfSubtitle)
          val _tmpItemCountDisplay: Int
          _tmpItemCountDisplay = _stmt.getLong(_columnIndexOfItemCountDisplay).toInt()
          val _tmpUpdatedLabel: String
          _tmpUpdatedLabel = _stmt.getText(_columnIndexOfUpdatedLabel)
          val _tmpCoverUrlsCsv: String
          _tmpCoverUrlsCsv = _stmt.getText(_columnIndexOfCoverUrlsCsv)
          val _tmpIsProtected: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProtected).toInt()
          _tmpIsProtected = _tmp != 0
          val _tmpSortOrder: Int
          _tmpSortOrder = _stmt.getLong(_columnIndexOfSortOrder).toInt()
          _item =
              ShelfEntity(_tmpId,_tmpName,_tmpSubtitle,_tmpItemCountDisplay,_tmpUpdatedLabel,_tmpCoverUrlsCsv,_tmpIsProtected,_tmpSortOrder)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>> {
    val _sql: String = "SELECT * FROM annotations WHERE bookId = ? ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("annotations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, bookId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBookId: Int = getColumnIndexOrThrow(_stmt, "bookId")
        val _columnIndexOfChapterTitle: Int = getColumnIndexOrThrow(_stmt, "chapterTitle")
        val _columnIndexOfPageNumber: Int = getColumnIndexOrThrow(_stmt, "pageNumber")
        val _columnIndexOfQuoteText: Int = getColumnIndexOrThrow(_stmt, "quoteText")
        val _columnIndexOfNoteText: Int = getColumnIndexOrThrow(_stmt, "noteText")
        val _columnIndexOfColorName: Int = getColumnIndexOrThrow(_stmt, "colorName")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfCreatedAtText: Int = getColumnIndexOrThrow(_stmt, "createdAtText")
        val _result: MutableList<AnnotationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AnnotationEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpBookId: String
          _tmpBookId = _stmt.getText(_columnIndexOfBookId)
          val _tmpChapterTitle: String
          _tmpChapterTitle = _stmt.getText(_columnIndexOfChapterTitle)
          val _tmpPageNumber: Int
          _tmpPageNumber = _stmt.getLong(_columnIndexOfPageNumber).toInt()
          val _tmpQuoteText: String
          _tmpQuoteText = _stmt.getText(_columnIndexOfQuoteText)
          val _tmpNoteText: String
          _tmpNoteText = _stmt.getText(_columnIndexOfNoteText)
          val _tmpColorName: String
          _tmpColorName = _stmt.getText(_columnIndexOfColorName)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpCreatedAtText: String
          _tmpCreatedAtText = _stmt.getText(_columnIndexOfCreatedAtText)
          _item =
              AnnotationEntity(_tmpId,_tmpBookId,_tmpChapterTitle,_tmpPageNumber,_tmpQuoteText,_tmpNoteText,_tmpColorName,_tmpType,_tmpCreatedAtText)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllAnnotations(): Flow<List<AnnotationEntity>> {
    val _sql: String = "SELECT * FROM annotations ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("annotations")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBookId: Int = getColumnIndexOrThrow(_stmt, "bookId")
        val _columnIndexOfChapterTitle: Int = getColumnIndexOrThrow(_stmt, "chapterTitle")
        val _columnIndexOfPageNumber: Int = getColumnIndexOrThrow(_stmt, "pageNumber")
        val _columnIndexOfQuoteText: Int = getColumnIndexOrThrow(_stmt, "quoteText")
        val _columnIndexOfNoteText: Int = getColumnIndexOrThrow(_stmt, "noteText")
        val _columnIndexOfColorName: Int = getColumnIndexOrThrow(_stmt, "colorName")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfCreatedAtText: Int = getColumnIndexOrThrow(_stmt, "createdAtText")
        val _result: MutableList<AnnotationEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AnnotationEntity
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpBookId: String
          _tmpBookId = _stmt.getText(_columnIndexOfBookId)
          val _tmpChapterTitle: String
          _tmpChapterTitle = _stmt.getText(_columnIndexOfChapterTitle)
          val _tmpPageNumber: Int
          _tmpPageNumber = _stmt.getLong(_columnIndexOfPageNumber).toInt()
          val _tmpQuoteText: String
          _tmpQuoteText = _stmt.getText(_columnIndexOfQuoteText)
          val _tmpNoteText: String
          _tmpNoteText = _stmt.getText(_columnIndexOfNoteText)
          val _tmpColorName: String
          _tmpColorName = _stmt.getText(_columnIndexOfColorName)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpCreatedAtText: String
          _tmpCreatedAtText = _stmt.getText(_columnIndexOfCreatedAtText)
          _item =
              AnnotationEntity(_tmpId,_tmpBookId,_tmpChapterTitle,_tmpPageNumber,_tmpQuoteText,_tmpNoteText,_tmpColorName,_tmpType,_tmpCreatedAtText)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAllRepositories(): Flow<List<RepositoryEntity>> {
    val _sql: String = "SELECT * FROM repositories"
    return createFlow(__db, false, arrayOf("repositories")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfPathUri: Int = getColumnIndexOrThrow(_stmt, "pathUri")
        val _columnIndexOfStorageType: Int = getColumnIndexOrThrow(_stmt, "storageType")
        val _columnIndexOfBookCount: Int = getColumnIndexOrThrow(_stmt, "bookCount")
        val _columnIndexOfSizeText: Int = getColumnIndexOrThrow(_stmt, "sizeText")
        val _columnIndexOfLastScanText: Int = getColumnIndexOrThrow(_stmt, "lastScanText")
        val _columnIndexOfScanRule: Int = getColumnIndexOrThrow(_stmt, "scanRule")
        val _columnIndexOfIsHealthy: Int = getColumnIndexOrThrow(_stmt, "isHealthy")
        val _result: MutableList<RepositoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RepositoryEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpPathUri: String
          _tmpPathUri = _stmt.getText(_columnIndexOfPathUri)
          val _tmpStorageType: String
          _tmpStorageType = _stmt.getText(_columnIndexOfStorageType)
          val _tmpBookCount: Int
          _tmpBookCount = _stmt.getLong(_columnIndexOfBookCount).toInt()
          val _tmpSizeText: String
          _tmpSizeText = _stmt.getText(_columnIndexOfSizeText)
          val _tmpLastScanText: String
          _tmpLastScanText = _stmt.getText(_columnIndexOfLastScanText)
          val _tmpScanRule: String
          _tmpScanRule = _stmt.getText(_columnIndexOfScanRule)
          val _tmpIsHealthy: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsHealthy).toInt()
          _tmpIsHealthy = _tmp != 0
          _item =
              RepositoryEntity(_tmpId,_tmpName,_tmpPathUri,_tmpStorageType,_tmpBookCount,_tmpSizeText,_tmpLastScanText,_tmpScanRule,_tmpIsHealthy)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBooksByIds(ids: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM books WHERE id IN (")
    val _inputSize: Int = ids.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in ids) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteShelfById(shelfId: String) {
    val _sql: String = "DELETE FROM shelves WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, shelfId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAnnotationById(id: Int) {
    val _sql: String = "DELETE FROM annotations WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteRepositoryById(id: String) {
    val _sql: String = "DELETE FROM repositories WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
