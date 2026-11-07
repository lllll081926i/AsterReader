package com.example.data.local

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.asterreader.core.database.AnnotationEntity
import com.asterreader.core.database.AsterDao
import com.asterreader.core.database.AsterDatabase
import com.asterreader.core.database.BookEntity
import com.asterreader.core.database.ChapterEntity
import com.asterreader.core.database.ComicPageEntity
import com.asterreader.core.database.RepositoryEntity
import com.asterreader.core.database.ShelfEntity
import com.asterreader.core.model.ReaderViewport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

/** 一次扫描的结果：新增、更新、失败、失联。 */
data class ScanReport(
    val imported: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val missing: Int = 0
) {
    val success: Int get() = imported + updated
}

/** 所有文档访问都在数据层执行，来源始终保留为 SAF URI。 */
class LibraryRepository(
    private val context: Context,
    private val dao: AsterDao = AsterDatabase.getDatabase(context).asterDao()
) {
    private val resolver = context.contentResolver
    private val localCopyMutex = kotlinx.coroutines.sync.Mutex()

    fun observeAllBooks() = dao.observeAllBooks()
    fun observePublicBooks() = dao.observePublicBooks()
    fun observeAccessibleBookById(id: String, unlocked: Boolean) = dao.observeAccessibleBookById(id, unlocked)
    fun observeAllShelves() = dao.observeAllShelves()
    fun observeAllAnnotations() = dao.observeAllAnnotations()
    fun observeVisibleAnnotations(unlocked: Boolean) = dao.observeVisibleAnnotations(unlocked)
    fun observeAllRepositories() = dao.observeAllRepositories()
    fun observeChaptersForBook(id: String) = dao.observeChapterSummaries(id)
    fun observeAnnotationsForBook(id: String) = dao.observeAnnotationsForBook(id)
    fun observeReaderState(id: String) = dao.observeReaderState(id)
    suspend fun getBookById(id: String) = dao.getBookById(id)
    suspend fun updateBook(book: BookEntity) = dao.updateBook(book)
    suspend fun updateMetadata(id: String, title: String, author: String, series: String, synopsis: String, tags: String) =
        dao.updateMetadata(id, title, author, series, synopsis, tags)
    suspend fun addBooksToShelf(ids: List<String>, shelfId: String) = dao.addBooksToShelf(ids, shelfId)
    suspend fun moveShelf(id: String, offset: Int) = dao.moveShelf(id, offset)
    suspend fun toggleFavorite(id: String) = dao.toggleFavorite(id)
    suspend fun updateProgress(id: String, page: Int, percent: Int, badge: String, chapter: String?, time: Long) =
        dao.updateProgress(id, page, percent, badge, chapter, time)
    suspend fun insertShelf(shelf: ShelfEntity) = dao.insertShelf(shelf)
    suspend fun deleteShelfById(id: String) = dao.removeShelfAndMembership(id)
    suspend fun renameShelf(id: String, name: String) = dao.renameShelf(id, name)
    suspend fun renameTag(old: String, new: String) = dao.renamePublicTag(old, new)
    suspend fun updateShelf(shelf: ShelfEntity) = dao.updateShelf(shelf)
    suspend fun insertAnnotation(annotation: AnnotationEntity) = dao.insertAnnotation(annotation)
    suspend fun deleteAnnotationById(id: Int) = dao.deleteAnnotationById(id)
    suspend fun removeBookmark(id: String, page: Int) = dao.removeBookmark(id, page)
    suspend fun removeBooks(ids: List<String>) = dao.removeBooks(ids)
    suspend fun deleteRepository(id: String) = dao.deleteRepositoryById(id)

    // ------------------------------------------------------------------
    // 阅读状态：页码 + 页内偏移 + 缩放，按书持久化
    // ------------------------------------------------------------------

    suspend fun saveViewport(id: String, viewport: ReaderViewport) = dao.saveViewport(id, viewport)

    suspend fun saveReaderOptions(id: String, mode: String?, fit: String?, cover: Boolean?, rtl: Boolean? = null) =
        dao.saveReaderOptions(id, mode, fit, cover, rtl)

    suspend fun readerState(id: String) = dao.getReaderState(id)

    // ------------------------------------------------------------------
    // 系列
    // ------------------------------------------------------------------

    suspend fun seriesBooks(series: String): List<BookEntity> =
        if (series.isBlank()) emptyList() else dao.getSeriesBooks(series)

    suspend fun updateSeries(bookId: String, series: String, order: Int) = dao.updateSeries(bookId, series, order)

    /** 隐藏/取消隐藏：只改变书库中的可见性，文件字节保持原样。 */
    suspend fun setBookHidden(bookId: String, hidden: Boolean) = dao.setBookHidden(bookId, hidden)

    fun observeHiddenCount() = dao.observeHiddenCount()

    /**
     * 智能识别系列名称与卷序
     * 例如："葬送的芙莉莲 卷11" -> ("葬送的芙莉莲", 11)
     */
    fun parseSeriesAndVolume(fileName: String): Pair<String, Int> {
        val cleanName = fileName.substringBeforeLast('.', fileName).trim()
        val volumePattern = Pattern.compile("(.+?)(?:\\s+(?:vol\\.?|v)|\\s*卷|\\s*第|\\s*#)\\s*([0-9]{1,4})\\s*(?:卷|话|集|章)?$", Pattern.CASE_INSENSITIVE)
        val matcher = volumePattern.matcher(cleanName)
        return if (matcher.matches()) {
            val series = matcher.group(1)?.trim().orEmpty()
            val volume = matcher.group(2)?.toIntOrNull() ?: 1
            series to volume
        } else {
            "" to 0
        }
    }

    // ------------------------------------------------------------------
    // 导入
    // ------------------------------------------------------------------

    suspend fun importDocument(
        uri: Uri,
        repositoryName: String = "本地文档",
        repositoryId: String = "",
        persist: Boolean = true,
        refreshExisting: Boolean = false,
        coverPage: Int = 1,
        replaceId: String? = null
    ): BookEntity = withContext(Dispatchers.IO) {
        require(uri.scheme == "content" || uri.scheme == "file") { "请选择系统文件提供者中的文档" }
        if (persist) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
        }
        val linked = dao.getBookByUri(uri.toString())
        require(replaceId == null || linked == null || linked.id == replaceId) { "此来源已关联另一部漫画" }
        val existing = if (replaceId != null) dao.getBookById(replaceId) ?: error("作品已被移除") else linked
        if (replaceId == null && existing != null && !refreshExisting) return@withContext existing
        require(existing == null || !existing.isVaultEncrypted) { "该文件不受当前版本支持，无法重新扫描" }
        val info = queryDocument(uri)
        if (replaceId == null && existing != null && existing.sourceStatus == "AVAILABLE" && info.modifiedAt > 0 && existing.sizeBytes == info.size &&
            existing.sourceModifiedAt == info.modifiedAt) {
            return@withContext existing
        }
        require(info.size <= MAX_FILE_BYTES) { "当前版本单文件上限为 512 MB" }
        val format = detectFormat(info.name, info.mimeType)
        require(format in READABLE_FORMATS) { "此格式尚不支持读取" }
        val id = existing?.id ?: UUID.randomUUID().toString()
        val selectedCover = existing?.coverPage ?: coverPage
        val coverFile = File(context.cacheDir, "covers/${id}.jpg")
        coverFile.parentFile?.mkdirs()
        // 单次临时拷贝内完成「页数 + 章节 + 封面」，避免大文件被完整复制多次
        val scanned = when (format) {
            "PDF" -> withTemporaryCopy(uri) { file ->
                val (count, _) = readPdf(file)
                Triple(count, emptyList<Pair<String, Int>>(), generatePdfCover(file, selectedCover, coverFile))
            }
            "IMAGE" -> withTemporaryCopy(uri) { file ->
                Triple(1, emptyList<Pair<String, Int>>(), writeCover({ file.inputStream() }, coverFile))
            }
            else -> withTemporaryCopy(uri) { file ->
                ComicArchives.open(file, format).use { archive ->
                    val count = archive.pageCount
                    val cover = writeCover(
                        { archive.openPage(selectedCover.coerceIn(1, count.coerceAtLeast(1))) },
                        coverFile
                    )
                    Triple(count, chapterSpans(archive.pageNames), cover)
                }
            }
        }
        val pageCount = scanned.first
        val chapterSpans = scanned.second
        require(pageCount > 0) { "文档没有可读取的页面" }
        val now = System.currentTimeMillis()
        val (seriesName, volOrder) = parseSeriesAndVolume(info.name)
        val coverPath = scanned.third ?: existing?.coverUrl.orEmpty()

        val book = BookEntity(
            id = id,
            title = info.name.substringBeforeLast('.', info.name),
            author = existing?.author.orEmpty(),
            series = existing?.series?.ifBlank { seriesName } ?: seriesName,
            format = format,
            coverUrl = coverPath,
            currentPage = existing?.currentPage ?: 1,
            totalPages = pageCount,
            progressPercent = existing?.progressPercent ?: 0,
            currentChapterTitle = existing?.currentChapterTitle ?: "第 1 页",
            statusBadge = existing?.statusBadge ?: "未读",
            seriesBadge = if (volOrder > 1) "卷 $volOrder" else "",
            fileSizeText = formatSize(info.size),
            filePath = uri.toString(),
            repositoryName = repositoryName,
            updatedTimeText = "刚刚导入",
            synopsis = existing?.synopsis.orEmpty(),
            tagsCsv = existing?.tagsCsv.orEmpty(),
            shelfIdsCsv = existing?.shelfIdsCsv.orEmpty(),
            sizeBytes = info.size,
            addedAt = existing?.addedAt ?: now,
            sourceModifiedAt = info.modifiedAt,
            repositoryId = repositoryId,
            sourceStatus = "AVAILABLE",
            coverPage = selectedCover.coerceIn(1, pageCount),
            seriesOrder = if (existing != null && existing.series.isNotBlank()) existing.seriesOrder else volOrder
        )
        currentCoroutineContext().ensureActive()
        dao.commitParsedSource(book, buildChapters(book.id, chapterSpans), emptyList(), replaceId)
    }

    /** 以「一个文件夹 = 一部作品」的方式导入散图目录。 */
    suspend fun importImageFolder(
        treeUri: Uri,
        folderUri: Uri,
        repositoryName: String,
        repositoryId: String,
        refreshExisting: Boolean = false,
        replaceId: String? = null
    ): BookEntity = withContext(Dispatchers.IO) {
        val images = listImages(treeUri, folderUri)
        require(images.isNotEmpty()) { "目录内没有图片" }
        val linked = dao.getBookByUri(folderUri.toString())
        require(replaceId == null || linked == null || linked.id == replaceId) { "此来源已关联另一部漫画" }
        val existing = if (replaceId != null) dao.getBookById(replaceId) ?: error("作品已被移除") else linked
        if (replaceId == null && existing != null && !refreshExisting) return@withContext existing
        val signature = images.joinToString("|") { "${it.name}:${it.size}:${it.modifiedAt}" }
        if (replaceId == null && existing != null && existing.sourceStatus == "AVAILABLE" &&
            existing.contentFingerprint == signature && dao.getComicPages(existing.id).size == images.size) return@withContext existing
        val id = existing?.id ?: UUID.randomUUID().toString()
        val name = documentDisplayName(folderUri) ?: "未命名目录"
        val (seriesName, volOrder) = parseSeriesAndVolume(name)
        val coverFile = File(context.cacheDir, "covers/${id}.jpg")
        val selectedCover = (existing?.coverPage ?: 1).coerceIn(1, images.size)
        val coverPath = writeCover({ runCatching { resolver.openInputStream(images[selectedCover - 1].uri) }.getOrNull() }, coverFile)
            ?: existing?.coverUrl.orEmpty()
        val totalSize = images.sumOf { it.size }
        val book = BookEntity(
            id = id,
            title = name,
            author = existing?.author.orEmpty(),
            series = existing?.series?.ifBlank { seriesName } ?: seriesName,
            format = "IMAGE_DIR",
            coverUrl = coverPath,
            currentPage = existing?.currentPage ?: 1,
            totalPages = images.size,
            progressPercent = existing?.progressPercent ?: 0,
            currentChapterTitle = existing?.currentChapterTitle ?: "第 1 页",
            statusBadge = existing?.statusBadge ?: "未读",
            seriesBadge = if (volOrder > 1) "卷 $volOrder" else "",
            fileSizeText = formatSize(totalSize),
            filePath = folderUri.toString(),
            repositoryName = repositoryName,
            updatedTimeText = "刚刚导入",
            synopsis = existing?.synopsis.orEmpty(),
            tagsCsv = existing?.tagsCsv.orEmpty(),
            shelfIdsCsv = existing?.shelfIdsCsv.orEmpty(),
            sizeBytes = totalSize,
            addedAt = existing?.addedAt ?: System.currentTimeMillis(),
            sourceModifiedAt = images.maxOf { it.modifiedAt },
            repositoryId = repositoryId,
            sourceStatus = "AVAILABLE",
            contentFingerprint = signature,
            coverPage = selectedCover,
            seriesOrder = if (existing != null && existing.series.isNotBlank()) existing.seriesOrder else volOrder
        )
        val pages = images.mapIndexed { index, image ->
            ComicPageEntity(book.id, index + 1, image.uri.toString(), image.name, image.size, image.modifiedAt)
        }
        currentCoroutineContext().ensureActive()
        val spans = chapterSpans(images.map { it.name })
        dao.commitParsedSource(book, buildChapters(book.id, spans), pages, replaceId)
    }

    // ------------------------------------------------------------------
    // 扫描
    // ------------------------------------------------------------------

    suspend fun scanRepository(
        uri: Uri,
        name: String,
        rule: String,
        onProgress: (processed: Int, discovered: Int) -> Unit = { _, _ -> }
    ): ScanReport = withContext(Dispatchers.IO) {
        require(rule in SUPPORTED_SCAN_RULES) { "当前仅支持每个文件作为作品，请重新添加仓库" }
        require(DocumentsContract.isTreeUri(uri)) { "请选择仓库目录" }
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {}
        val repoId = UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
        val repoName = name.ifBlank { "本地目录" }
        dao.insertRepository(RepositoryEntity(repoId, repoName, uri.toString(), "SMARTPHONE", 0, "", "扫描中", rule, false))
        var imported = 0
        var updated = 0
        var failed = 0
        var processed = 0
        val seen = mutableSetOf<String>()
        try {
            kotlinx.coroutines.coroutineScope {
                val queue = ArrayDeque<String>()
                val visited = mutableSetOf<String>()
                var enumerated = 0
                queue.add(DocumentsContract.getTreeDocumentId(uri))
                while (queue.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    val parent = queue.removeFirst()
                    if (!visited.add(parent)) continue
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(uri, parent).toString()
                    val children = queryChildren(uri, parent, MAX_SCAN_ITEMS - enumerated)
                    enumerated += children.size
                    check(enumerated <= MAX_SCAN_ITEMS) { "单次扫描最多处理 $MAX_SCAN_ITEMS 项，请缩小授权范围" }
                    children.filter { it.isDirectory }.forEach { queue.add(it.documentId) }
                    val archives = children.filter { !it.isDirectory && it.extension in ARCHIVE_EXTENSIONS }
                    val images = children.filter { !it.isDirectory && it.extension in IMAGE_EXTENSIONS }
                    for (child in archives) {
                        currentCoroutineContext().ensureActive()
                        seen.add(child.uriString)
                        try {
                            val before = dao.getBookByUri(child.uriString)
                            val after = importDocument(child.uri, repoName, repoId, persist = false, refreshExisting = true)
                            when {
                                before == null -> imported++
                                after.totalPages != before.totalPages || after.sourceModifiedAt != before.sourceModifiedAt -> updated++
                            }
                            seen.add(child.uriString)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            failed++
                            dao.getBookByUri(child.uriString)?.let { dao.updateSourceStatus(it.id, "ERROR") }
                        }
                        onProgress(++processed, enumerated)
                    }
                    if (images.isNotEmpty()) {
                        currentCoroutineContext().ensureActive()
                        seen.add(parentUri)
                        try {
                            val before = dao.getBookByUri(parentUri)
                            val after = importImageFolder(uri, Uri.parse(parentUri), repoName, repoId, refreshExisting = true)
                            when {
                                before == null -> imported++
                                after.contentFingerprint != before.contentFingerprint -> updated++
                            }
                            seen.add(parentUri)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            failed++
                            dao.getBookByUri(parentUri)?.let { dao.updateSourceStatus(it.id, "ERROR") }
                        }
                        onProgress(++processed, enumerated)
                    }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            withContext(kotlinx.coroutines.NonCancellable) {
                dao.insertRepository(RepositoryEntity(repoId, repoName, uri.toString(), "SMARTPHONE",
                    imported, "", "扫描已取消", rule, false))
            }
            throw cancelled
        } catch (failure: Exception) {
            dao.insertRepository(RepositoryEntity(repoId, repoName, uri.toString(), "SMARTPHONE",
                imported, "", "扫描未完成", rule, false))
            throw failure
        }
        currentCoroutineContext().ensureActive()
        val missing = dao.reconcileSources(repoId, seen)
        dao.insertRepository(RepositoryEntity(repoId, repoName, uri.toString(), "SMARTPHONE",
            dao.getRepositoryBooks(repoId).size, "", java.time.Instant.now().toString(), rule, failed == 0 && missing == 0))
        ScanReport(imported, updated, failed, missing)
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    private val renderSession = DocumentRenderSession(context)

    private fun sourceKey(book: BookEntity): String = if (book.format == "IMAGE_DIR") {
        "${book.id}:${book.filePath}:${book.contentFingerprint}"
    } else {
        "${book.id}:${book.filePath}:${book.sizeBytes}:${book.sourceModifiedAt}"
    }

    /** 归档必须落到本地文件才能随机读；这里复用缓存副本，避免每次打开都整本复制。 */
    private suspend fun prepareSource(book: BookEntity): PageSource = if (book.format == "IMAGE_DIR") {
        val pages = dao.getComicPages(book.id).associateBy { it.number }
        PageSource.Directory(pages.size) { number ->
            pages[number]?.let { page ->
                runCatching { resolver.openInputStream(Uri.parse(page.uri)) }.getOrNull()
            }
        }
    } else {
        PageSource.Document(localCopy(book), book.format, owned = false)
    }

    suspend fun renderPage(id: String, page: Int, maxPixels: Int = DEFAULT_PAGE_PIXELS): Bitmap =
        withContext(Dispatchers.IO) {
            val book = dao.getBookById(id) ?: error("作品已被移除")
            require(!book.isVaultEncrypted) { "该文件为加密容器，当前版本暂不支持" }
            require(page in 1..book.totalPages) { "页码超出范围" }
            renderSession.render(sourceKey(book), page, maxPixels) { prepareSource(book) }
        }

    /** 预热相邻页。失败不影响阅读，只让翻页少一次等待。 */
    suspend fun prefetchPages(id: String, pages: List<Int>, maxPixels: Int = DEFAULT_PAGE_PIXELS) =
        withContext(Dispatchers.IO) {
            val book = dao.getBookById(id) ?: return@withContext
            if (book.isVaultEncrypted) return@withContext
            renderSession.prefetch(sourceKey(book), pages, maxPixels) { prepareSource(book) }
        }

    /** 放大后按可见纵向条带取原始分辨率；不支持时返回 null，UI 退回整页位图。 */
    suspend fun renderPageRegion(
        id: String,
        page: Int,
        topFraction: Float,
        bottomFraction: Float,
        maxPixels: Int = ZOOMED_PAGE_PIXELS
    ): Bitmap? = withContext(Dispatchers.IO) {
        val book = dao.getBookById(id) ?: return@withContext null
        if (book.isVaultEncrypted) return@withContext null
        renderSession.renderRegion(sourceKey(book), page, topFraction, bottomFraction, maxPixels) {
            prepareSource(book)
        }
    }

    fun trimPageMemory() = renderSession.trimMemory()

    suspend fun closeReadSession() = renderSession.close()

    // ------------------------------------------------------------------
    // 封面
    // ------------------------------------------------------------------

    /** 重新生成全部封面（缓存被系统清空后可一键重建）。返回成功数量。 */
    suspend fun refreshAllBookCovers(): Int = withContext(Dispatchers.IO) {
        val books = dao.observeAllBooks().first()
        var refreshed = 0
        books.forEach { book ->
            if (book.isVaultEncrypted) return@forEach
            currentCoroutineContext().ensureActive()
            val target = File(context.cacheDir, "covers/${book.id}.jpg")
            val path = runCatching {
                if (book.format == "IMAGE_DIR") regenerateDirectoryCover(book, target)
                else generateCover(Uri.parse(book.filePath), book.format, book.coverPage, target)
            }.getOrNull()
            if (path != null) {
                dao.updateCover(book.id, path, book.coverPage)
                refreshed++
            }
        }
        refreshed
    }

    /** 只补全封面文件缺失的作品，避免每次刷新都重算全部封面。 */
    suspend fun refreshMissingCovers(): Int = withContext(Dispatchers.IO) {
        val books = dao.observeAllBooks().first()
        var refreshed = 0
        books.forEach { book ->
            if (book.isVaultEncrypted) return@forEach
            val existing = book.coverUrl.takeIf { it.isNotBlank() }?.let { File(it) }
            if (existing != null && existing.exists() && existing.length() > 0) return@forEach
            currentCoroutineContext().ensureActive()
            val target = File(context.cacheDir, "covers/${book.id}.jpg")
            val path = runCatching {
                if (book.format == "IMAGE_DIR") regenerateDirectoryCover(book, target)
                else generateCover(Uri.parse(book.filePath), book.format, book.coverPage, target)
            }.getOrNull() ?: return@forEach
            dao.updateCover(book.id, path, book.coverPage)
            refreshed++
        }
        refreshed
    }

    suspend fun setCoverPage(bookId: String, page: Int): Boolean = withContext(Dispatchers.IO) {
        val book = dao.getBookById(bookId) ?: return@withContext false
        if (page !in 1..book.totalPages) return@withContext false
        val target = File(context.cacheDir, "covers/${book.id}.jpg")
        val path = if (book.format == "IMAGE_DIR") {
            val pages = dao.getComicPages(book.id).associateBy { it.number }
            val uri = pages[page]?.uri ?: return@withContext false
            writeCover({ runCatching { resolver.openInputStream(Uri.parse(uri)) }.getOrNull() }, target)
        } else {
            generateCover(Uri.parse(book.filePath), book.format, page, target)
        } ?: return@withContext false
        dao.updateCover(book.id, path, page)
        true
    }

    /** 清理自建封面缓存，返回释放的字节数。 */
    suspend fun clearCoverCache(): Long = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "covers")
        val files = dir.listFiles() ?: return@withContext 0L
        val released = files.sumOf { it.length() }
        files.forEach { it.delete() }
        released
    }

    suspend fun clearRegenerableDiskCache(): Long = withContext(Dispatchers.IO) {
        closeReadSession()
        localCopyMutex.withLock {
            val files = listOf("covers", "read").flatMap { File(context.cacheDir, it).listFiles()?.toList().orEmpty() }
            files.filter { it.isFile }.sumOf { file -> val size = file.length(); if (file.delete()) size else 0L }
        }
    }

    private suspend fun regenerateDirectoryCover(book: BookEntity, target: File): String? {
        val uri = dao.getComicPage(book.id, book.coverPage)?.uri
            ?: dao.getComicPage(book.id, 1)?.uri
            ?: return null
        return writeCover({ runCatching { resolver.openInputStream(Uri.parse(uri)) }.getOrNull() }, target)
    }

    // ------------------------------------------------------------------
    // 来源修复
    // ------------------------------------------------------------------

    suspend fun relocateBookSource(bookId: String, newUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val book = dao.getBookById(bookId) ?: return@withContext false
        try {
            resolver.takePersistableUriPermission(newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {}
        if (book.format == "IMAGE_DIR" && DocumentsContract.isTreeUri(newUri)) {
            return@withContext relocateFolderSource(book, newUri)
        }
        try {
            importDocument(newUri, book.repositoryName, book.repositoryId, persist = false,
                refreshExisting = true, replaceId = book.id)
            closeReadSession()
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) { false }
    }

    private suspend fun relocateFolderSource(book: BookEntity, treeUri: Uri): Boolean {
        return try {
            val folder = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
            importImageFolder(treeUri, folder, book.repositoryName, book.repositoryId,
                refreshExisting = true, replaceId = book.id)
            closeReadSession()
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) { false }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private data class DocumentInfo(val name: String, val size: Long, val modifiedAt: Long, val mimeType: String)

    private data class ChildEntry(
        val documentId: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedAt: Long,
        val uriString: String
    ) {
        val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val extension: String get() = name.substringAfterLast('.', "").uppercase(Locale.ROOT)
        val uri: Uri get() = Uri.parse(uriString)
    }

    private data class ImagePage(val uri: Uri, val name: String, val size: Long, val modifiedAt: Long)

    private fun detectFormat(name: String, mimeType: String): String {
        val extension = name.substringAfterLast('.', "").uppercase(Locale.ROOT)
        if (extension in ARCHIVE_EXTENSIONS || extension == "PDF") return extension
        if (extension in IMAGE_EXTENSIONS) return "IMAGE"
        return when (mimeType) {
            "application/pdf" -> "PDF"
            "application/vnd.comicbook+zip", "application/zip" -> "CBZ"
            "application/vnd.comicbook-rar", "application/vnd.rar", "application/x-rar-compressed" -> "CBR"
            "application/x-7z-compressed" -> "CB7"
            "application/x-tar" -> "CBT"
            else -> extension
        }
    }

    private fun queryDocument(uri: Uri): DocumentInfo {
        // 外部「打开方式」偶尔会给 file:// URI：ContentResolver 查不到元数据，直接读文件属性
        if (uri.scheme == "file") {
            val file = File(uri.path.orEmpty())
            require(file.isFile) { "文件不可访问，请重新选择" }
            return DocumentInfo(file.name, file.length(), file.lastModified(), resolver.getType(uri).orEmpty())
        }
        return resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null, null, null
        )?.use { cursor ->
            check(cursor.moveToFirst()) { "文档不可访问" }
            val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            DocumentInfo(
                name = cursor.getString(nameIndex) ?: "未命名",
                size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L,
                modifiedAt = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L,
                mimeType = resolver.getType(uri).orEmpty()
            )
        } ?: error("无法读取文档信息")
    }

    private fun documentDisplayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private suspend fun queryChildren(treeUri: Uri, parentId: String, limit: Int = MAX_SCAN_ITEMS): List<ChildEntry> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val result = ArrayList<ChildEntry>()
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                check(result.size < limit) { "单次扫描最多处理 $MAX_SCAN_ITEMS 项，请缩小授权范围" }
                val documentId = if (idIndex >= 0) cursor.getString(idIndex) else null
                if (documentId == null) continue
                val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex).orEmpty() else ""
                val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                val modified = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L
                result.add(
                    ChildEntry(
                        documentId = documentId,
                        name = name,
                        mimeType = mime,
                        size = size,
                        modifiedAt = modified,
                        uriString = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString()
                    )
                )
            }
        } ?: error("目录不可访问，请重新授权")
        return result
    }

    private suspend fun listImages(treeUri: Uri, folderUri: Uri): List<ImagePage> {
        val folderId = DocumentsContract.getDocumentId(folderUri)
        return queryChildren(treeUri, folderId)
            .filter { !it.isDirectory && it.extension in IMAGE_EXTENSIONS }
            .sortedWith { a, b -> naturalCompare(a.name, b.name) }
            .map { ImagePage(it.uri, it.name, it.size, it.modifiedAt) }
    }

    private fun readPdf(file: File): Pair<Int, List<Pair<String, Int>>> {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try { PdfRenderer(descriptor) } catch (failure: Throwable) {
            descriptor.close(); throw failure
        }
        return renderer.use { it.pageCount to emptyList() }
    }

    /** 连续同名目录合并成一「话」，返回 (标题, 页数)；无层级时返回空表示「全本」。 */
    private fun chapterSpans(names: List<String>): List<Pair<String, Int>> {
        if (names.isEmpty()) return emptyList()
        val spans = ArrayList<Pair<String, Int>>()
        var currentKey: String? = null
        var count = 0
        names.forEach { name ->
            val folder = name.substringBeforeLast('/', "")
            val key = folder.substringAfterLast('/').ifBlank { "" }
            if (key != currentKey) {
                if (currentKey != null) spans.add((currentKey ?: "").ifBlank { "正篇" } to count)
                currentKey = key
                count = 0
            }
            count++
        }
        if (currentKey != null) spans.add((currentKey ?: "").ifBlank { "正篇" } to count)
        return if (spans.size <= 1) emptyList() else spans
    }

    private fun buildChapters(bookId: String, spans: List<Pair<String, Int>>): List<ChapterEntity> {
        var start = 1
        return spans.mapIndexed { index, (title, count) ->
            val end = start + count - 1
            val chapter = ChapterEntity(
                id = "${bookId}_ch_${index + 1}",
                bookId = bookId,
                chapterNumber = index + 1,
                title = title,
                startPage = start,
                endPage = end,
                isCompleted = false,
                isCurrent = index == 0
            )
            start = end + 1
            chapter
        }
    }

    private fun generateCover(uri: Uri, format: String, page: Int, target: File): String? = try {
        when {
            format == "IMAGE_DIR" -> null
            format == "PDF" -> withTemporaryCopySync(uri) { file -> generatePdfCover(file, page, target) }
            else -> withTemporaryCopySync(uri) { file ->
                ComicArchives.open(file, format).use { archive ->
                    val index = page.coerceIn(1, archive.pageCount.coerceAtLeast(1))
                    writeCover({ archive.openPage(index) }, target)
                }
            }
        }
    } catch (_: Exception) { null }

    private fun generatePdfCover(file: File, page: Int, target: File): String? {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try { PdfRenderer(descriptor) } catch (failure: Throwable) {
            descriptor.close(); throw failure
        }
        return renderer.use {
            val index = (page - 1).coerceIn(0, (it.pageCount - 1).coerceAtLeast(0))
            it.openPage(index).use { pdfPage ->
                val scale = minOf(720f / pdfPage.width, 1280f / pdfPage.height, 1f)
                val bitmap = Bitmap.createBitmap(
                    (pdfPage.width * scale).toInt().coerceAtLeast(1),
                    (pdfPage.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                writeBitmap(bitmap, target)
            }
        }
    }

    private fun writeCover(open: () -> java.io.InputStream?, target: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = open() ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        var sample = 1
        while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 1280) sample *= 2
        val bitmap = open()?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        return writeBitmap(bitmap, target)
    }

    /** 把封面限制在 720×1280 以内，避免极端长图生成巨大的 JPEG。 */
    private fun scaleDown(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        if (source.width <= maxWidth && source.height <= maxHeight) return source
        val ratio = minOf(maxWidth.toFloat() / source.width, maxHeight.toFloat() / source.height)
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== source) source.recycle()
        return scaled
    }

    private fun writeBitmap(bitmap: Bitmap, target: File): String? = try {
        target.parentFile?.mkdirs()
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 86, it) }
        target.absolutePath
    } finally {
        bitmap.recycle()
    }

    private suspend fun <T> withTemporaryCopy(uri: Uri, block: (File) -> T): T {
        val file = copyDocument(uri)
        return try {
            currentCoroutineContext().ensureActive()
            block(file)
        } finally {
            file.delete()
        }
    }

    private fun <T> withTemporaryCopySync(uri: Uri, block: (File) -> T): T {
        val file = File.createTempFile("cover-", ".tmp", context.cacheDir)
        try {
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            } ?: error("无法读取源文件")
            return block(file)
        } finally {
            file.delete()
        }
    }

    private suspend fun copyDocument(uri: Uri): File {
        val file = File.createTempFile("document-", ".tmp", context.cacheDir)
        try {
            copyInto(uri, file)
            return file
        } catch (failure: Throwable) {
            file.delete()
            throw failure
        }
    }

    /**
     * 阅读用的本地副本：按「作品 id + 大小 + 修改时间」命名并长期保留，
     * 同一本书再次打开时直接命中，不再整本复制；总量超限时按最后使用时间淘汰。
     */
    private suspend fun localCopy(book: BookEntity): File = localCopyMutex.withLock {
        val dir = File(context.cacheDir, "read").apply { mkdirs() }
        val identity = "${book.id}:${book.filePath}:${book.sizeBytes}:${book.sourceModifiedAt}:${book.contentFingerprint}"
        val name = java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) } + ".arc"
        val target = File(dir, name)
        if (book.sourceModifiedAt > 0 && book.sizeBytes > 0 && target.isFile && target.length() > 0) {
            target.setLastModified(System.currentTimeMillis())
            // 上次复制中断留下的 .part 不该一直占着缓存额度
            dir.listFiles()?.filter {
                it.name.endsWith(".part") && System.currentTimeMillis() - it.lastModified() > 3_600_000L
            }?.forEach { it.delete() }
            return@withLock target
        }
        val temp = File(dir, "$name.part")
        try {
            copyInto(Uri.parse(book.filePath), temp)
            if (!temp.renameTo(target)) throw java.io.IOException("无法写入本地副本")
        } catch (failure: Throwable) {
            temp.delete()
            throw failure
        }
        pruneLocalCopies(dir, keep = target)
        target
    }

    private fun pruneLocalCopies(dir: File, keep: File) {
        val files = dir.listFiles()?.filter { it != keep && it.isFile } ?: return
        var total = files.sumOf { it.length() } + keep.length()
        if (total <= MAX_LOCAL_COPY_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_LOCAL_COPY_BYTES) return@forEach
            total -= file.length()
            file.delete()
        }
    }

    private suspend fun copyInto(uri: Uri, target: File) {
        require(uri.scheme == "content" || uri.scheme == "file") { "源文件不可访问，请重新导入" }
        try {
            val sourceStream: InputStream? = if (uri.scheme == "file") {
                runCatching { FileInputStream(File(uri.path.orEmpty())) }.getOrNull()
            } else {
                resolver.openInputStream(uri)
            }
            sourceStream?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= MAX_FILE_BYTES) { "当前版本单文件上限为 512 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("无法读取源文件，请重新授权")
        } catch (failure: Throwable) {
            target.delete()
            throw failure
        }
    }

    // ------------------------------------------------------------------
    // 书库备份 / 恢复（只含元数据，见 LibraryBackup）
    // ------------------------------------------------------------------

    suspend fun exportLibraryTo(stream: OutputStream) = withContext(Dispatchers.IO) {
        LibraryBackup.export(dao, stream)
    }

    suspend fun importLibraryFrom(stream: InputStream): LibraryBackup.ImportResult =
        withContext(Dispatchers.IO) { LibraryBackup.import(dao, stream) }

    companion object {
        const val DEFAULT_PAGE_PIXELS = 2_500_000
        const val ZOOMED_PAGE_PIXELS = 6_000_000
        private const val MAX_FILE_BYTES = 512L * 1024 * 1024
        private const val MAX_LOCAL_COPY_BYTES = 1024L * 1024 * 1024
        private const val MAX_SCAN_ITEMS = 10_000

        /** UI 与扫描实现共享，避免两处硬编码规则字符串不一致。 */
        const val DEFAULT_SCAN_RULE = "每个文件作为作品"
        val SUPPORTED_SCAN_RULES = setOf(DEFAULT_SCAN_RULE, "单文件单作品")

        /** 真正可读的容器：ZIP/CBZ、TAR/CBT、7Z/CB7、RAR/CBR、EPUB、PDF、单图、散图目录。 */
        val READABLE_FORMATS = setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "PDF", "EPUB", "IMAGE", "IMAGE_DIR")
        private val ARCHIVE_EXTENSIONS = setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "EPUB", "PDF")
        val IMAGE_EXTENSIONS = setOf("JPG", "JPEG", "PNG", "WEBP", "BMP", "GIF", "AVIF", "HEIC", "HEIF")

        fun formatSize(bytes: Long): String =
            if (bytes <= 0) "大小未知" else String.format(Locale.ROOT, "%.2f MB", bytes / 1048576.0)

        fun naturalCompare(left: String, right: String): Int {
            var a = 0
            var b = 0
            while (a < left.length && b < right.length) {
                if (left[a] in '0'..'9' && right[b] in '0'..'9') {
                    var aEnd = a
                    var bEnd = b
                    while (aEnd < left.length && left[aEnd] in '0'..'9') aEnd++
                    while (bEnd < right.length && right[bEnd] in '0'..'9') bEnd++
                    var aNumber = a
                    var bNumber = b
                    while (aNumber < aEnd && left[aNumber] == '0') aNumber++
                    while (bNumber < bEnd && right[bNumber] == '0') bNumber++
                    val lengthOrder = (aEnd - aNumber).compareTo(bEnd - bNumber)
                    if (lengthOrder != 0) return lengthOrder
                    while (aNumber < aEnd && bNumber < bEnd) {
                        val order = left[aNumber++].compareTo(right[bNumber++])
                        if (order != 0) return order
                    }
                    val zerosOrder = (aEnd - a).compareTo(bEnd - b)
                    if (zerosOrder != 0) return zerosOrder
                    a = aEnd
                    b = bEnd
                } else {
                    val order = left[a].lowercaseChar().compareTo(right[b].lowercaseChar())
                    if (order != 0) return order
                    a++
                    b++
                }
            }
            return (left.length - a).compareTo(right.length - b)
        }
    }
}
