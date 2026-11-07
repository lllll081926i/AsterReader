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

    @Query("UPDATE books SET title = CASE WHEN :title = '' THEN title ELSE :title END, author = :author, series = :series, synopsis = :synopsis, tagsCsv = :tags WHERE id = :id")
    suspend fun updateMetadata(id: String, title: String, author: String, series: String, synopsis: String, tags: String)

    @Transaction
    suspend fun addBooksToShelf(ids: List<String>, shelfId: String) {
        check(getAllShelves().any { it.id == shelfId }) { "书架不存在" }
        ids.forEach { id ->
            val book = getBookById(id) ?: return@forEach
            val membership = book.shelfIdsCsv.split(',').filter(String::isNotBlank).toMutableSet()
            membership.add(shelfId)
            updateShelfMembership(id, membership.joinToString(","))
        }
    }

    @Query("UPDATE shelves SET sortOrder = :position WHERE id = :id")
    suspend fun updateShelfPosition(id: String, position: Int)

    @Transaction
    suspend fun moveShelf(id: String, offset: Int) {
        val ordered = getAllShelves().sortedWith(compareBy<ShelfEntity> { it.sortOrder }.thenBy { it.id }).toMutableList()
        val index = ordered.indexOfFirst { it.id == id }
        val target = index + offset
        if (index < 0 || target !in ordered.indices) return
        ordered.add(target, ordered.removeAt(index))
        ordered.forEachIndexed { position, shelf -> updateShelfPosition(shelf.id, position) }
    }

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
        check(!current.isVaultEncrypted) { "加密容器不受当前版本支持" }
        val page = current.currentPage.coerceIn(1, book.totalPages)
        val percent = if (current.progressPercent == 0) 0 else (page.toLong() * 100 / book.totalPages).toInt()
        val updated = current.copy(totalPages = book.totalPages, currentPage = page,
            progressPercent = percent,
            statusBadge = when { percent == 0 -> "未读"; percent >= 100 -> "已读完"; else -> "在读" },
            currentChapterTitle = if (page == current.currentPage) current.currentChapterTitle else "第 $page 页",
            sizeBytes = book.sizeBytes, sourceModifiedAt = book.sourceModifiedAt,
            fileSizeText = book.fileSizeText, repositoryName = book.repositoryName,
            repositoryId = book.repositoryId.ifBlank { current.repositoryId }, sourceStatus = "AVAILABLE",
            contentFingerprint = book.contentFingerprint,
            coverPage = book.coverPage,
            coverUrl = book.coverUrl.ifBlank { current.coverUrl },
            series = current.series.ifBlank { book.series })
        updateBook(updated)
        return updated
    }

    @Query("SELECT * FROM books WHERE filePath = :uri LIMIT 1")
    suspend fun getBookByUri(uri: String): BookEntity?

    @Query("""SELECT * FROM books WHERE filePath = :uri OR filePath LIKE :documentPattern ESCAPE '\'""")
    suspend fun getDocumentSourceCandidates(uri: String, documentPattern: String): List<BookEntity>

    @Query("UPDATE books SET repositoryId = :repositoryId, repositoryName = :name WHERE id = :id")
    suspend fun attachSourceRepository(id: String, repositoryId: String, name: String)

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

    @Transaction
    suspend fun replaceChapters(bookId: String, chapters: List<ChapterEntity>) {
        deleteChaptersForBooks(listOf(bookId))
        if (chapters.isNotEmpty()) insertChapters(chapters)
    }

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

    @Query("UPDATE books SET shelfIdsCsv = :value WHERE id = :id")
    suspend fun updateShelfMembership(id: String, value: String)

    @Query("UPDATE shelves SET name = :name WHERE id = :id")
    suspend fun renameShelf(id: String, name: String)

    @Transaction
    suspend fun createUniqueShelf(shelf: ShelfEntity) {
        require(shelf.name.isNotBlank() && shelf.name.length <= 80 && ',' !in shelf.id) { "书架名称无效" }
        require(getAllShelves().none { it.name == shelf.name }) { "书架名称已存在" }
        insertShelf(shelf)
    }

    @Transaction
    suspend fun renameUniqueShelf(id: String, name: String) {
        require(name.isNotBlank() && name.length <= 80) { "书架名称无效" }
        require(getAllShelves().none { it.id != id && it.name == name }) { "书架名称已存在" }
        renameShelf(id, name)
    }

    @Query("UPDATE books SET tagsCsv = :tags WHERE id = :id")
    suspend fun updateTags(id: String, tags: String)

    @Transaction
    suspend fun removeShelfAndMembership(id: String) {
        getAllBooks().forEach { book ->
            val ids = book.shelfIdsCsv.split(',').filter(String::isNotBlank)
            if (id in ids) updateShelfMembership(book.id, ids.filter { it != id }.joinToString(","))
        }
        deleteShelfById(id)
    }

    @Transaction
    suspend fun renamePublicTag(old: String, new: String) {
        getAllBooks().filter { !it.isVaultProtected && !it.isVaultEncrypted }.forEach { book ->
            val tags = book.tagsCsv.split(',').map(String::trim).filter(String::isNotBlank)
            if (old in tags) updateTags(book.id, tags.map { if (it == old) new else it }.distinct().joinToString(","))
        }
    }

    @Transaction
    suspend fun removePublicTag(tag: String) {
        getAllBooks().filter { !it.isVaultProtected && !it.isVaultEncrypted }.forEach { book ->
            val tags = book.tagsCsv.split(',').map(String::trim).filter(String::isNotBlank)
            if (tag in tags) updateTags(book.id, tags.filter { it != tag }.joinToString(","))
        }
    }

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

    @Query("SELECT * FROM reader_states WHERE bookId = :id")
    fun observeReaderState(id: String): Flow<ReaderStateEntity?>

    @Query("SELECT * FROM reader_states WHERE bookId = :id")
    suspend fun getReaderState(id: String): ReaderStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReaderState(state: ReaderStateEntity)

    @Transaction
    suspend fun saveReadingPosition(id: String, page: Int, chapter: String?, viewport: com.asterreader.core.model.ReaderViewport?, timestamp: Long) {
        val book = getBookById(id) ?: return
        if (book.isVaultEncrypted) return
        val position = (viewport ?: com.asterreader.core.model.ReaderViewport(page)).copy(page = page).sanitized(book.totalPages)
        val percent = if (book.statusBadge == "已读完") 100 else
            (position.page.toLong() * 100 / book.totalPages.coerceAtLeast(1)).toInt().coerceIn(0, 99)
        updateProgress(id, position.page, percent, if (percent == 100) "已读完" else "在读", chapter, timestamp)
        val previous = getReaderState(id) ?: ReaderStateEntity(id)
        saveReaderState(previous.copy(page = position.page, offsetFraction = position.offsetFraction,
            scale = position.scale, panX = position.panX, panY = position.panY))
    }

    @Query("UPDATE books SET progressPercent = 100, statusBadge = '已读完' WHERE id = :id AND isVaultEncrypted = 0")
    suspend fun markReadingCompleted(id: String)

    @Transaction
    suspend fun saveViewport(id: String, position: com.asterreader.core.model.ReaderViewport) {
        val book = getBookById(id) ?: return
        if (book.isVaultEncrypted) return
        val safe = position.sanitized(book.totalPages)
        val previous = getReaderState(id) ?: ReaderStateEntity(id)
        saveReaderState(previous.copy(page = safe.page, offsetFraction = safe.offsetFraction,
            scale = safe.scale, panX = safe.panX, panY = safe.panY))
    }

    @Transaction
    suspend fun saveReaderOptions(id: String, mode: String?, fit: String?, cover: Boolean?, rtl: Boolean?) {
        val book = getBookById(id) ?: return
        if (book.isVaultEncrypted) return
        val previous = getReaderState(id) ?: ReaderStateEntity(id, page = book.currentPage)
        saveReaderState(previous.copy(layoutMode = mode, fitMode = fit, coverSingle = cover, rightToLeft = rtl))
    }

    @Query("SELECT * FROM comic_pages WHERE bookId = :id AND number = :number")
    suspend fun getComicPage(id: String, number: Int): ComicPageEntity?

    @Query("SELECT * FROM comic_pages WHERE bookId = :id ORDER BY number ASC")
    suspend fun getComicPages(id: String): List<ComicPageEntity>

    @Query("SELECT * FROM books WHERE series = :series AND series <> '' ORDER BY seriesOrder ASC, addedAt ASC")
    suspend fun getSeriesBooks(series: String): List<BookEntity>

    @Query("SELECT * FROM books WHERE sourceStatus = 'MISSING'")
    fun observeMissingBooks(): Flow<List<BookEntity>>

    @Query("DELETE FROM comic_pages WHERE bookId = :id")
    suspend fun deleteComicPages(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicPages(pages: List<ComicPageEntity>)

    @Transaction
    suspend fun importDirectory(book: BookEntity, pages: List<ComicPageEntity>): BookEntity {
        val saved = importComic(book)
        deleteComicPages(saved.id)
        insertComicPages(pages.map { it.copy(bookId = saved.id) })
        return saved
    }

    @Transaction
    suspend fun commitParsedSource(book: BookEntity, chapters: List<ChapterEntity>, pages: List<ComicPageEntity>, replaceId: String?): BookEntity {
        val saved = if (replaceId == null) importComic(book) else replaceSource(replaceId, book)
        deleteComicPages(saved.id)
        if (pages.isNotEmpty()) insertComicPages(pages.map { it.copy(bookId = saved.id) })
        replaceChapters(saved.id, chapters.map { it.copy(bookId = saved.id) })
        return saved
    }

    @Query("SELECT * FROM books WHERE repositoryId = :id")
    suspend fun getRepositoryBooks(id: String): List<BookEntity>

    @Query("UPDATE books SET sourceStatus = :status WHERE id = :id")
    suspend fun updateSourceStatus(id: String, status: String)

    @Query("UPDATE books SET coverUrl = :cover, coverPage = :page WHERE id = :id AND isVaultEncrypted = 0")
    suspend fun updateCover(id: String, cover: String, page: Int)

    @Query("UPDATE books SET series = :series, seriesOrder = :position WHERE id = :id")
    suspend fun updateSeries(id: String, series: String, position: Int)

    /** 「隐藏」只改可见性，不触碰文件字节；隐藏作品仍然可读、可扫描、可保存进度。 */
    @Query("UPDATE books SET isVaultProtected = :hidden WHERE id = :id AND isVaultEncrypted = 0")
    suspend fun setBookHidden(id: String, hidden: Boolean)

    @Query("SELECT COUNT(*) FROM books WHERE isVaultProtected = 1")
    fun observeHiddenCount(): Flow<Int>

    @Transaction
    suspend fun reconcileSources(repositoryId: String, seen: Set<String>): Int {
        var missing = 0
        getRepositoryBooks(repositoryId).forEach { book ->
            val reachable = book.filePath in seen
            if (book.isVaultEncrypted) return@forEach
            if (!reachable && book.sourceStatus != "MISSING") {
                updateSourceStatus(book.id, "MISSING")
                missing++
            } else if (reachable && book.sourceStatus == "MISSING") {
                updateSourceStatus(book.id, "AVAILABLE")
            }
        }
        return missing
    }

    @Transaction
    suspend fun replaceSource(id: String, source: BookEntity): BookEntity {
        val current = getBookById(id) ?: error("作品已被移除")
        check(!current.isVaultEncrypted) { "加密容器不受当前版本支持" }
        val duplicate = getBookByUri(source.filePath)
        check(duplicate == null || duplicate.id == id) { "此来源已关联另一部漫画" }
        val page = current.currentPage.coerceIn(1, source.totalPages)
        val percent = if (current.progressPercent == 0) 0 else (page.toLong() * 100 / source.totalPages).toInt()
        val updated = current.copy(filePath = source.filePath, format = source.format,
            totalPages = source.totalPages, currentPage = page, progressPercent = percent,
            statusBadge = when { percent == 0 -> "未读"; percent >= 100 -> "已读完"; else -> "在读" },
            currentChapterTitle = "第 $page 页", sourceStatus = "AVAILABLE", sizeBytes = source.sizeBytes,
            sourceModifiedAt = source.sourceModifiedAt, contentFingerprint = source.contentFingerprint,
            fileSizeText = source.fileSizeText, coverUrl = source.coverUrl, coverPage = source.coverPage)
        updateBook(updated)
        deleteComicPages(id)
        return updated
    }

    // ------------------------------------------------------------------
    // 书库备份 / 恢复（只导出元数据，SAF 源文件授权需在新设备上重新选择）
    // ------------------------------------------------------------------

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM shelves")
    suspend fun getAllShelves(): List<ShelfEntity>

    @Query("SELECT * FROM annotations")
    suspend fun getAllAnnotations(): List<AnnotationEntity>

    @Query("SELECT * FROM reader_states")
    suspend fun getAllReaderStates(): List<ReaderStateEntity>

    @Query("SELECT * FROM repositories")
    suspend fun getAllRepositories(): List<RepositoryEntity>

    @Query("SELECT * FROM chapters")
    suspend fun getAllChapters(): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun restoreReaderStates(states: List<ReaderStateEntity>)

    /** 默认按阅读时间合并；整个恢复失败时回滚所有表。 */
    @Transaction
    suspend fun restoreBackup(books: List<BookEntity>, shelves: List<ShelfEntity>,
        annotations: List<AnnotationEntity>, states: List<ReaderStateEntity>,
        chapters: List<ChapterEntity>, repositories: List<RepositoryEntity>, pages: List<ComicPageEntity>) {
        val accepted = mutableSetOf<String>()
        val added = mutableSetOf<String>()
        books.forEach { incoming ->
            val current = getBookById(incoming.id)
            if (current == null) {
                insertBook(incoming)
                accepted.add(incoming.id)
                added.add(incoming.id)
            } else if (incoming.lastReadTimestamp > current.lastReadTimestamp) {
                val page = incoming.currentPage.coerceIn(1, current.totalPages)
                updateBook(current.copy(currentPage = page,
                    progressPercent = incoming.progressPercent, currentChapterTitle = incoming.currentChapterTitle,
                    lastReadTimestamp = incoming.lastReadTimestamp, statusBadge = incoming.statusBadge))
                accepted.add(incoming.id)
            }
        }
        val existingShelves = getAllShelves().map { it.id }.toSet()
        insertShelves(shelves.filter { it.id !in existingShelves })
        val noteKeys = getAllAnnotations().map { it.copy(id = 0) }.toMutableSet()
        val newNotes = annotations.map { it.copy(id = 0) }.filter { noteKeys.add(it) }
        if (newNotes.isNotEmpty()) insertAnnotations(newNotes)
        states.filter { it.bookId in accepted }.forEach { state ->
            val total = getBookById(state.bookId)?.totalPages ?: error("恢复作品不存在")
            saveReaderState(state.copy(page = state.page.coerceIn(1, total)))
        }
        insertChapters(chapters.filter { it.bookId in added })
        insertComicPages(pages.filter { it.bookId in added })
        val existingRepos = getAllRepositories().map { it.id }.toSet()
        insertRepositories(repositories.filter { it.id !in existingRepos })
    }
}
