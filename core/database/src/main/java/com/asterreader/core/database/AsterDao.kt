package com.asterreader.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AsterDao {
    // Books
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isVaultProtected = 0 AND isVaultEncrypted = 0 ORDER BY lastReadTimestamp DESC")
    fun observePublicBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun observeBookById(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId AND (:unlocked = 1 OR (isVaultProtected = 0 AND isVaultEncrypted = 0)) LIMIT 1")
    fun observeAccessibleBookById(bookId: String, unlocked: Boolean): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE books SET currentPage = :page, progressPercent = :percent, statusBadge = :badge, currentChapterTitle = COALESCE(:chapter, currentChapterTitle), lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, page: Int, percent: Int, badge: String, chapter: String?, timestamp: Long)

    @Query("DELETE FROM books WHERE id IN (:ids)")
    suspend fun deleteBooksByIds(ids: List<String>)

    @Query("DELETE FROM annotations WHERE bookId IN (:ids)")
    suspend fun deleteAnnotationsForBooks(ids: List<String>)

    @Query("DELETE FROM chapters WHERE bookId IN (:ids)")
    suspend fun deleteChaptersForBooks(ids: List<String>)

    @Transaction
    suspend fun removeBooks(ids: List<String>) {
        deleteAnnotationsForBooks(ids)
        deleteChaptersForBooks(ids)
        deleteBooksByIds(ids)
    }

    @Transaction
    suspend fun importPublication(book: BookEntity, chapters: List<ChapterEntity>) {
        insertBook(book)
        deleteChaptersForBooks(listOf(book.id))
        insertChapters(chapters)
    }

    // 在事务内重新读取用户状态，扫描不得覆盖并发收藏或阅读进度。
    @Transaction
    suspend fun importComic(book: BookEntity): BookEntity {
        val current = getBookByUri(book.filePath)
        if (current == null) {
            insertBook(book)
            return book
        }
        check(!current.isVaultProtected && !current.isVaultEncrypted) { "受保护作品不能通过普通扫描更新" }
        val page = current.currentPage.coerceIn(1, book.totalPages)
        val percent = if (current.progressPercent == 0) 0 else (page.toLong() * 100 / book.totalPages).toInt()
        val updated = current.copy(totalPages = book.totalPages, currentPage = page,
            progressPercent = percent,
            statusBadge = when { percent == 0 -> "未读"; percent >= 100 -> "已读完"; else -> "在读" },
            currentChapterTitle = if (page == current.currentPage) current.currentChapterTitle else "第 $page 页",
            sizeBytes = book.sizeBytes, sourceModifiedAt = book.sourceModifiedAt,
            fileSizeText = book.fileSizeText, repositoryName = book.repositoryName)
        updateBook(updated)
        return updated
    }

    @Query("SELECT * FROM books WHERE filePath = :uri LIMIT 1")
    suspend fun getBookByUri(uri: String): BookEntity?

    @Query("SELECT * FROM chapters WHERE bookId = :id ORDER BY chapterNumber")
    suspend fun getChapters(id: String): List<ChapterEntity>

    @Query("DELETE FROM annotations WHERE bookId = :bookId AND pageNumber = :page AND type = 'BOOKMARK'")
    suspend fun removeBookmark(bookId: String, page: Int)

    // Chapters
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterNumber ASC")
    fun observeChaptersForBook(bookId: String): Flow<List<ChapterEntity>>

    @Query("SELECT id, bookId, chapterNumber, title, startPage, endPage, isCompleted, isCurrent FROM chapters WHERE bookId = :bookId ORDER BY chapterNumber ASC")
    fun observeChapterSummaries(bookId: String): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    // Shelves
    @Query("SELECT * FROM shelves ORDER BY sortOrder ASC")
    fun observeAllShelves(): Flow<List<ShelfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelves(shelves: List<ShelfEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity)

    @Update
    suspend fun updateShelf(shelf: ShelfEntity)

    @Query("DELETE FROM shelves WHERE id = :shelfId")
    suspend fun deleteShelfById(shelfId: String)

    // Annotations
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY id DESC")
    fun observeAnnotationsForBook(bookId: String): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations ORDER BY id DESC")
    fun observeAllAnnotations(): Flow<List<AnnotationEntity>>

    @Query("SELECT annotations.* FROM annotations INNER JOIN books ON books.id = annotations.bookId WHERE :unlocked = 1 OR (books.isVaultProtected = 0 AND books.isVaultEncrypted = 0) ORDER BY annotations.id DESC")
    fun observeVisibleAnnotations(unlocked: Boolean): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotations(annotations: List<AnnotationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Int)

    // Repositories
    @Query("SELECT * FROM repositories")
    fun observeAllRepositories(): Flow<List<RepositoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepositories(repos: List<RepositoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: RepositoryEntity)

    @Update
    suspend fun updateRepository(repo: RepositoryEntity)

    @Query("DELETE FROM repositories WHERE id = :id")
    suspend fun deleteRepositoryById(id: String)
}
