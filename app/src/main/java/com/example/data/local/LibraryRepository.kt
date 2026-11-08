package com.example.data.local

import android.content.Context
import coil.imageLoader
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.data.local.vault.FileVaultStore
import com.example.data.local.vault.VaultRecord
import com.asterreader.core.database.ReaderStateEntity
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
import com.example.data.remote.WebDavRepository

/** 一次扫描的结果：新增、更新、失败、失联。 */
data class ScanReport(
    val imported: Int = 0,
    val updated: Int = 0,
    val failed: Int = 0,
    val missing: Int = 0,
    val unchanged: Int = 0,
    val hidden: Int = 0,
    val ignored: Int = 0,
    val failureReasons: List<String> = emptyList()
) {
    val success: Int get() = imported + updated

    fun summary(): String = buildString {
        append("扫描完成：新增 $imported，更新 $updated，已存在 $unchanged")
        if (hidden > 0) append("；包含隐藏作品，请验证后查看")
        if (ignored > 0) append("；忽略 $ignored 个非漫画文件")
        if (failed > 0) append("；失败 $failed：${failureReasons.joinToString("、")}")
        if (missing > 0) append("；失联 $missing")
    }
}

/** 所有文档访问都在数据层执行，来源始终保留为 SAF URI。 */
class LibraryRepository(
    private val context: Context,
    private val dao: AsterDao = AsterDatabase.getDatabase(context).asterDao(),
    private val remoteSource: WebDavRepository = WebDavRepository.get(context)
) {
    private val resolver = context.contentResolver
    private val localCopyMutex = kotlinx.coroutines.sync.Mutex()
    internal var fileVault = FileVaultStore.get(context)

    fun observeAllBooks() = combine(dao.observeAllBooks(), fileVault.records) { books, records ->
        val committed = fileVault.committedIds()
        val existing = books.mapTo(HashSet()) { it.id }
        (books.map { records[it.id]?.book ?: if (it.id in committed) vaultShell(it) else it } + records.values.filter { it.book.id !in existing }.map { it.book })
            .sortedByDescending { it.lastReadTimestamp }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observePublicBooks() = combine(dao.observePublicBooks(), fileVault.records) { books, _ ->
        val committed = fileVault.committedIds()
        books.filterNot { it.id in committed }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observeAccessibleBookById(id: String, unlocked: Boolean) =
        combine(dao.observeAccessibleBookById(id, unlocked), fileVault.records) { book, records ->
            if (unlocked) records[id]?.book ?: if (id in fileVault.committedIds()) book?.let { vaultShell(it) } else book
            else if (id in fileVault.committedIds()) null else book
        }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observeAllShelves() = dao.observeAllShelves()
    fun observeAllAnnotations() = combine(dao.observeAllAnnotations(), fileVault.records) { notes, records ->
        val committed = fileVault.committedIds()
        notes.filterNot { it.bookId in committed } + records.values.flatMap { it.annotations }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observeVisibleAnnotations(unlocked: Boolean) = combine(dao.observeVisibleAnnotations(unlocked), fileVault.records) { notes, records ->
        val committed = fileVault.committedIds()
        notes.filterNot { it.bookId in committed } + if (unlocked) records.values.flatMap { it.annotations } else emptyList()
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observeAllRepositories() = dao.observeAllRepositories()
    fun observeChaptersForBook(id: String) = combine(dao.observeChapterSummaries(id), fileVault.records) { chapters, records ->
        records[id]?.chapters ?: if (id in fileVault.committedIds()) emptyList() else chapters
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observeAnnotationsForBook(id: String) = combine(dao.observeAnnotationsForBook(id), fileVault.records) { notes, records ->
        records[id]?.annotations ?: if (id in fileVault.committedIds()) emptyList() else notes
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    fun observeReaderState(id: String) = combine(dao.observeReaderState(id), fileVault.records) { state, records ->
        records[id]?.reader ?: if (id in fileVault.committedIds()) null else state
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
    suspend fun getBookById(id: String) = fileVault.records.value[id]?.book ?: dao.getBookById(id)?.let {
        if (id in fileVault.committedIds()) vaultShell(it) else it
    }
    private suspend fun changePrivate(id: String, transform: (VaultRecord) -> VaultRecord): Boolean = withContext(Dispatchers.IO) {
        if (id !in fileVault.committedIds()) false else { fileVault.update(id, transform); true }
    }
    suspend fun updateBook(book: BookEntity) {
        if (!changePrivate(book.id) { it.copy(book = book) }) dao.updateBook(book)
    }
    suspend fun updateMetadata(id: String, title: String, author: String, series: String, synopsis: String, tags: String) {
        if (!changePrivate(id) { it.copy(book = it.book.copy(title = title.ifBlank { it.book.title }, author = author,
                series = series, synopsis = synopsis, tagsCsv = tags)) }) dao.updateMetadata(id, title, author, series, synopsis, tags)
    }
    suspend fun addBooksToShelf(ids: List<String>, shelfId: String) {
        require(dao.getAllShelves().any { it.id == shelfId })
        for (id in ids) if (!changePrivate(id) { it.copy(book = it.book.copy(shelfIdsCsv =
                (it.book.shelfIdsCsv.split(',').filter(String::isNotBlank) + shelfId).distinct().joinToString(","))) })
            dao.addBooksToShelf(listOf(id), shelfId)
    }
    suspend fun moveShelf(id: String, offset: Int) = dao.moveShelf(id, offset)
    suspend fun toggleFavorite(id: String) { if (!changePrivate(id) { it.copy(book = it.book.copy(isFavorite = !it.book.isFavorite)) }) dao.toggleFavorite(id) }
    suspend fun updateProgress(id: String, page: Int, percent: Int, badge: String, chapter: String?, time: Long) {
        if (!changePrivate(id) { it.copy(book = it.book.copy(currentPage = page, progressPercent = percent, statusBadge = badge,
                currentChapterTitle = chapter ?: it.book.currentChapterTitle, lastReadTimestamp = time)) }) dao.updateProgress(id, page, percent, badge, chapter, time)
    }
    suspend fun insertShelf(shelf: ShelfEntity) = dao.createUniqueShelf(shelf)
    suspend fun deleteShelfById(id: String) = dao.removeShelfAndMembership(id)
    suspend fun renameShelf(id: String, name: String) = dao.renameUniqueShelf(id, name)
    suspend fun renameTag(old: String, new: String) = dao.renamePublicTag(old, new)
    suspend fun deleteTag(tag: String) = dao.removePublicTag(tag)
    suspend fun updateShelf(shelf: ShelfEntity) = dao.updateShelf(shelf)
    suspend fun insertAnnotation(annotation: AnnotationEntity) {
        if (!changePrivate(annotation.bookId) { it.copy(annotations = it.annotations + annotation.copy(id = fileVault.nextAnnotationId())) }) dao.insertAnnotation(annotation)
    }
    suspend fun deleteAnnotationById(id: Int) {
        if (id < 0) {
            val record = fileVault.records.value.values.firstOrNull { r -> r.annotations.any { it.id == id } } ?: error("请先解锁文件保险库")
            changePrivate(record.book.id) { it.copy(annotations = it.annotations.filterNot { a -> a.id == id }) }
        } else dao.deleteAnnotationById(id)
    }
    suspend fun removeBookmark(id: String, page: Int) {
        if (!changePrivate(id) { it.copy(annotations = it.annotations.filterNot { a -> a.pageNumber == page && a.type == "BOOKMARK" }) }) dao.removeBookmark(id, page)
    }
    suspend fun removeBooks(ids: List<String>) {
        require(ids.none { it in fileVault.committedIds() }) { "请先导出加密作品；保险库密文不会通过移除书库记录删除" }
        dao.removeBooks(ids)
    }
    suspend fun deleteRepository(id: String) = dao.deleteRepositoryById(id)

    // ------------------------------------------------------------------
    // 阅读状态：页码 + 页内偏移 + 缩放，按书持久化
    // ------------------------------------------------------------------

    suspend fun saveViewport(id: String, viewport: ReaderViewport) {
        if (!changePrivate(id) { r -> val v = viewport.sanitized(r.book.totalPages)
                r.copy(reader = r.reader.copy(page = v.page, offsetFraction = v.offsetFraction, scale = v.scale, panX = v.panX, panY = v.panY)) }) dao.saveViewport(id, viewport)
    }
    suspend fun saveReadingPosition(id: String, page: Int, chapter: String?, viewport: ReaderViewport?) {
        if (!changePrivate(id) { r ->
            val next = page.coerceIn(1, r.book.totalPages)
            val done = r.book.statusBadge == "已读完"
            val v = viewport?.sanitized(r.book.totalPages)
            r.copy(book = r.book.copy(currentPage = next, progressPercent = if (done) 100 else (next * 100 / r.book.totalPages).coerceAtMost(99),
                statusBadge = if (done) "已读完" else "在读", currentChapterTitle = chapter ?: r.book.currentChapterTitle,
                lastReadTimestamp = System.currentTimeMillis()), reader = r.reader.copy(page = next,
                offsetFraction = v?.offsetFraction ?: r.reader.offsetFraction, scale = v?.scale ?: r.reader.scale,
                panX = v?.panX ?: r.reader.panX, panY = v?.panY ?: r.reader.panY))
        }) dao.saveReadingPosition(id, page, chapter, viewport, System.currentTimeMillis())
    }

    suspend fun saveReaderOptions(id: String, mode: String?, fit: String?, cover: Boolean?, rtl: Boolean? = null) {
        if (!changePrivate(id) { it.copy(reader = it.reader.copy(layoutMode = mode, fitMode = fit, coverSingle = cover, rightToLeft = rtl)) }) dao.saveReaderOptions(id, mode, fit, cover, rtl)
    }

    suspend fun readerState(id: String) = fileVault.records.value[id]?.reader ?: dao.getReaderState(id)

    // ------------------------------------------------------------------
    // 系列
    // ------------------------------------------------------------------

    suspend fun seriesBooks(series: String): List<BookEntity> =
        if (series.isBlank()) emptyList() else observeAllBooks().first().filter { it.series == series }.sortedBy { it.seriesOrder }

    suspend fun updateSeries(bookId: String, series: String, order: Int) {
        if (!changePrivate(bookId) { it.copy(book = it.book.copy(series = series, seriesOrder = order)) }) dao.updateSeries(bookId, series, order)
    }

    /** 隐藏/取消隐藏：只改变书库中的可见性，文件字节保持原样。 */
    suspend fun setBookHidden(bookId: String, hidden: Boolean) {
        require(bookId !in fileVault.committedIds()) { "加密作品仅在保险库解锁后显示" }
        dao.setBookHidden(bookId, hidden)
    }
    suspend fun markReadingCompleted(bookId: String) {
        if (!changePrivate(bookId) { it.copy(book = it.book.copy(progressPercent = 100, statusBadge = "已读完")) }) dao.markReadingCompleted(bookId)
    }

    fun observeHiddenCount() = dao.observeHiddenCount()
    fun nativePageSize(bitmap: Bitmap): Pair<Int, Int>? = renderSession.nativeSize(bitmap)

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
        replaceId: String? = null,
        includePdfCover: Boolean = true
    ): BookEntity = withContext(Dispatchers.IO) {
        require(uri.scheme == "content" || uri.scheme == "file") { "请选择系统文件提供者中的文档" }
        if (fileVault.configured) {
            val tag = fileVault.sourceTag(uri.toString())
            dao.getBookByUri("vault-source://$tag")?.let { shell ->
                return@withContext fileVault.records.value[shell.id]?.book ?: shell
            }
        }
        val linked = findDocumentSource(uri)
        if (persist && uri.scheme == "content" && !WebDavRepository.isRemote(uri)) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                if (linked == null) return@withContext importManagedDocument(uri, repositoryName, repositoryId, coverPage, replaceId)
            }
        }
        require(replaceId == null || linked == null || linked.id == replaceId) { "此来源已关联另一部漫画" }
        val existing = if (replaceId != null) dao.getBookById(replaceId) ?: error("作品已被移除") else linked
        if (replaceId == null && existing != null && !refreshExisting) return@withContext existing
        require(existing == null || !existing.isVaultEncrypted) { "该文件不受当前版本支持，无法重新扫描" }
        val info = queryDocument(uri)
        if (replaceId == null && existing != null && existing.sourceStatus == "AVAILABLE" && info.modifiedAt > 0 && existing.sizeBytes == info.size &&
            existing.sourceModifiedAt == info.modifiedAt && existing.contentFingerprint == info.fingerprint) {
            if (repositoryId.isNotBlank() && existing.repositoryId != repositoryId) {
                dao.attachSourceRepository(existing.id, repositoryId, repositoryName)
                return@withContext existing.copy(repositoryId = repositoryId, repositoryName = repositoryName)
            }
            return@withContext existing
        }
        val format = resolveFormat(info.name, info.mimeType, uri)
        require(format in READABLE_FORMATS) { "此格式尚不支持读取" }
        val id = existing?.id ?: UUID.randomUUID().toString()
        val selectedCover = existing?.coverPage ?: coverPage
        val coverFile = File(context.cacheDir, "covers/${id}.jpg")
        coverFile.parentFile?.mkdirs()
        // 单次临时拷贝内完成「页数 + 章节 + 封面」，避免大文件被完整复制多次
        val scanned = when (format) {
            "PDF" -> withTemporaryCopy(uri) { file ->
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = try { PdfRenderer(descriptor) } catch (failure: Throwable) {
                    descriptor.close(); throw failure
                }
                renderer.use {
                    Triple(it.pageCount, emptyList<Pair<String, Int>>(),
                        if (includePdfCover) generatePdfCover(it, selectedCover, coverFile) else null)
                }
            }
            "IMAGE" -> withTemporaryCopy(uri) { file ->
                Triple(1, emptyList<Pair<String, Int>>(), writeCover({ file.inputStream() }, coverFile))
            }
            else -> withTemporaryCopy(uri) { file ->
                ComicArchives.open(file, format).use { archive ->
                    val count = archive.pageCount
                    require(count > 0) { "归档内没有可读取的图片页面" }
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
            title = existing?.title ?: info.name.substringBeforeLast('.', info.name),
            author = existing?.author.orEmpty(),
            series = existing?.series?.ifBlank { seriesName } ?: seriesName,
            format = format,
            coverUrl = coverPath,
            currentPage = (existing?.currentPage ?: 1).coerceIn(1, pageCount),
            totalPages = pageCount,
            progressPercent = existing?.progressPercent ?: 0,
            currentChapterTitle = existing?.currentChapterTitle ?: "第 1 页",
            statusBadge = existing?.statusBadge ?: "未读",
            seriesBadge = if (volOrder > 1) "卷 $volOrder" else "",
            fileSizeText = formatSize(info.size),
            filePath = if (replaceId == null && existing != null) existing.filePath else uri.toString(),
            repositoryName = repositoryName,
            updatedTimeText = "刚刚导入",
            synopsis = existing?.synopsis.orEmpty(),
            tagsCsv = existing?.tagsCsv.orEmpty(),
            shelfIdsCsv = existing?.shelfIdsCsv.orEmpty(),
            sizeBytes = info.size,
            addedAt = existing?.addedAt ?: now,
            sourceModifiedAt = info.modifiedAt,
            contentFingerprint = info.fingerprint,
            repositoryId = repositoryId,
            sourceStatus = "AVAILABLE",
            coverPage = selectedCover.coerceIn(1, pageCount),
            seriesOrder = if (existing != null && existing.series.isNotBlank()) existing.seriesOrder else volOrder
        )
        currentCoroutineContext().ensureActive()
        dao.commitParsedSource(book, buildChapters(book.id, chapterSpans), emptyList(), replaceId)
    }

    /** 临时授权来源转为应用持有的副本，成功后不依赖外部提供者继续授权。 */
    private suspend fun importManagedDocument(uri: Uri, name: String, repositoryId: String, coverPage: Int, replaceId: String?): BookEntity {
        val info = queryDocument(uri)
        val directory = File(context.filesDir, "imports/${UUID.randomUUID()}").apply { check(mkdirs()) }
        val filename = info.name.substringAfterLast('/').substringAfterLast('\\').take(180)
            .takeUnless { it.isBlank() || it == "." || it == ".." } ?: "document"
        val target = File(directory, filename)
        try {
            require(target.canonicalFile.parentFile == directory.canonicalFile) { "文件名称无效" }
            copyInto(uri, target)
            return importDocument(Uri.parse(target.toURI().toString()), name, repositoryId, persist = false,
                refreshExisting = true, coverPage = coverPage, replaceId = replaceId)
        } catch (failure: Throwable) {
            target.delete()
            directory.delete()
            throw failure
        }
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
        if (fileVault.configured) {
            val tag = fileVault.sourceTag(folderUri.toString())
            dao.getBookByUri("vault-source://$tag")?.let { shell ->
                return@withContext fileVault.records.value[shell.id]?.book ?: shell
            }
        }
        val images = listImages(treeUri, folderUri)
        require(images.isNotEmpty()) { "目录内没有图片" }
        val linked = dao.getBookByUri(folderUri.toString())
        require(replaceId == null || linked == null || linked.id == replaceId) { "此来源已关联另一部漫画" }
        val existing = if (replaceId != null) dao.getBookById(replaceId) ?: error("作品已被移除") else linked
        if (replaceId == null && existing != null && !refreshExisting) return@withContext existing
        val signature = images.joinToString("|") { "${it.name}:${it.size}:${it.modifiedAt}" + if (it.fingerprint.isBlank()) "" else ":${it.fingerprint}" }
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
            title = existing?.title ?: name,
            author = existing?.author.orEmpty(),
            series = existing?.series?.ifBlank { seriesName } ?: seriesName,
            format = "IMAGE_DIR",
            coverUrl = coverPath,
            currentPage = (existing?.currentPage ?: 1).coerceIn(1, images.size),
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
            if (!WebDavRepository.isRemote(uri)) {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (_: SecurityException) { error("此目录不支持长期授权，请选择其他本地目录") }
        val repoId = UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
        val repoName = name.ifBlank { "本地目录" }
        dao.insertRepository(RepositoryEntity(repoId, repoName, uri.toString(), "SMARTPHONE", 0, "", "扫描中", rule, false))
        var imported = 0
        var updated = 0
        var failed = 0
        var processed = 0
        var discoveredWorks = 0
        var unchanged = 0
        var hidden = 0
        var ignored = 0
        val failureReasons = linkedSetOf<String>()
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
                    onProgress(processed, discoveredWorks)
                    check(enumerated <= MAX_SCAN_ITEMS) { "单次扫描最多处理 $MAX_SCAN_ITEMS 项，请缩小授权范围" }
                    children.filter { it.isDirectory }.forEach { queue.add(it.documentId) }
                    val classified = children.filter { !it.isDirectory }.map { child ->
                        currentCoroutineContext().ensureActive()
                        val declared = detectFormat(child.name, child.mimeType)
                        child to if (declared in READABLE_FORMATS && declared != "IMAGE") declared else try {
                            resolveFormat(child.name, child.mimeType, child.uri)
                        } catch (failure: Exception) {
                            failed++
                            failureReasons.add(scanFailureReason(failure))
                            "ERROR"
                        }
                    }
                    val archives = classified.filter { it.second in ARCHIVE_EXTENSIONS }.map { it.first }
                    val images = classified.filter { it.second == "IMAGE" }.map { it.first }
                    discoveredWorks += archives.size + if (images.isEmpty()) 0 else 1
                    onProgress(processed, discoveredWorks)
                    ignored += classified.count { it.second !in ARCHIVE_EXTENSIONS && it.second != "IMAGE" && it.second != "ERROR" }
                    for (child in archives) {
                        currentCoroutineContext().ensureActive()
                        seen.add(child.uriString)
                        onProgress(processed, discoveredWorks)
                        try {
                            val before = findDocumentSource(child.uri)
                            val after = importDocument(child.uri, repoName, repoId, persist = false, refreshExisting = true, includePdfCover = false)
                            when {
                                before == null -> imported++
                                after.totalPages != before.totalPages || after.sourceModifiedAt != before.sourceModifiedAt ||
                                    after.contentFingerprint != before.contentFingerprint -> updated++
                                else -> unchanged++
                            }
                            if (after.isVaultProtected || after.isVaultEncrypted) hidden++
                            seen.add(after.filePath)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            failed++
                            failureReasons.add(scanFailureReason(failure))
                            findDocumentSource(child.uri)?.let { seen.add(it.filePath); dao.updateSourceStatus(it.id, "ERROR") }
                        }
                        onProgress(++processed, discoveredWorks)
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
                                else -> unchanged++
                            }
                            if (after.isVaultProtected || after.isVaultEncrypted) hidden++
                            seen.add(parentUri)
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            failed++
                            failureReasons.add(scanFailureReason(failure))
                            dao.getBookByUri(parentUri)?.let { dao.updateSourceStatus(it.id, "ERROR") }
                        }
                        onProgress(++processed, discoveredWorks)
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
        if (WebDavRepository.isRemote(uri)) {
            val shelfId = UUID.nameUUIDFromBytes("webdav-shelf:$uri".toByteArray()).toString()
            dao.attachRepositoryShelf(repoId, shelfId, repoName)
        }
        ScanReport(imported, updated, failed, missing, unchanged, hidden, ignored, failureReasons.toList())
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    private val renderSession = DocumentRenderSession(context)

    private fun sourceKey(book: BookEntity): String = if (book.isVaultEncrypted) {
        "secure:${book.id}:${fileVault.sessionToken()}"
    } else if (book.format == "IMAGE_DIR") {
        "${book.id}:${book.filePath}:${book.contentFingerprint}"
    } else {
        "${book.id}:${book.filePath}:${book.sizeBytes}:${book.sourceModifiedAt}:${book.contentFingerprint}"
    }

    /** 普通本地来源直接随机读取；仅不可寻址的提供者需要兼容副本。 */
    private suspend fun prepareSource(book: BookEntity): PageSource = if (book.isVaultEncrypted) {
        val record = fileVault.records.value[book.id] ?: error("请先解锁文件保险库")
        if (record.pageSizes.isNotEmpty()) {
            val offsets = record.pageSizes.runningFold(0L, Long::plus)
            PageSource.Directory(record.pageSizes.size) { number ->
                require(number in 1..record.pageSizes.size)
                val reader = fileVault.openReader(book.id)
                object : java.io.FilterInputStream(reader.stream(offsets[number - 1], record.pageSizes[number - 1])) {
                    override fun close() { try { super.close() } finally { fileVault.release(reader) } }
                }
            }
        } else if (book.format == "PDF") {
            val proxy = com.example.data.local.vault.VaultProxySource(context, fileVault, book.id)
            PageSource.SecurePdf(ParcelFileDescriptor.dup(proxy.descriptor.fileDescriptor), proxy::close)
        } else PageSource.SecureArchive(com.example.data.local.vault.VaultComicArchive(fileVault, record))
    } else if (book.format == "IMAGE_DIR") {
        val pages = dao.getComicPages(book.id).associateBy { it.number }
        PageSource.Directory(pages.size) { number ->
            pages[number]?.let { page ->
                runCatching { resolver.openInputStream(Uri.parse(page.uri)) }.getOrNull()
            }
        }
    } else {
        val direct = openLocalDocument(Uri.parse(book.filePath))
        if (direct != null) PageSource.Document(direct.file, book.format, owned = false, release = direct::close)
        else PageSource.Document(localCopy(book), book.format, owned = false)
    }

    suspend fun renderPage(id: String, page: Int, maxPixels: Int = DEFAULT_PAGE_PIXELS): Bitmap =
        withContext(Dispatchers.IO) {
            val token = fileVault.sessionToken()
            val book = getBookById(id) ?: error("作品已被移除")
            require(page in 1..book.totalPages) { "页码超出范围" }
            renderSession.render(sourceKey(book), page, maxPixels) { prepareSource(book) }.also {
                if (book.isVaultEncrypted) check(fileVault.isSessionValid(token)) { "保险库已锁定" }
            }
        }

    /** 预热相邻页。失败不影响阅读，只让翻页少一次等待。 */
    suspend fun prefetchPages(id: String, pages: List<Int>, maxPixels: Int = DEFAULT_PAGE_PIXELS) =
        withContext(Dispatchers.IO) {
            val book = getBookById(id) ?: return@withContext
            if (book.isVaultEncrypted && !fileVault.unlocked) return@withContext
            renderSession.prefetch(sourceKey(book), pages, if (book.format == "PDF") minOf(maxPixels, PDF_PREVIEW_PIXELS) else maxPixels) { prepareSource(book) }
        }

    /** 放大后按可见纵向条带取原始分辨率；不支持时返回 null，UI 退回整页位图。 */
    suspend fun renderPageRegion(
        id: String,
        page: Int,
        topFraction: Float,
        bottomFraction: Float,
        maxPixels: Int = ZOOMED_PAGE_PIXELS
    ): Bitmap? = withContext(Dispatchers.IO) {
        val token = fileVault.sessionToken()
        val book = getBookById(id) ?: return@withContext null
        if (book.isVaultEncrypted && !fileVault.unlocked) return@withContext null
        renderSession.renderRegion(sourceKey(book), page, topFraction, bottomFraction, maxPixels) {
            prepareSource(book)
        }.also { if (book.isVaultEncrypted) check(fileVault.isSessionValid(token)) { "保险库已锁定" } }
    }

    fun trimPageMemory() = renderSession.trimMemory()

    suspend fun closeReadSession(bookId: String? = null) {
        val expectedKey = if (bookId == null) null else getBookById(bookId)?.let(::sourceKey) ?: return
        renderSession.close(expectedKey)
    }

    suspend fun initializeOrUnlockFileVault(password: CharArray, create: Boolean) = withContext(Dispatchers.IO) {
        val coroutine = currentCoroutineContext()
        fileVault.open(password, create) { coroutine.ensureActive() }
        // 元数据提交后、数据库去敏前崩溃时，解锁后重放去敏操作。
        fileVault.records.value.values.forEach { record ->
            val existing = dao.getBookById(record.book.id)
            if (existing != null && !existing.isVaultEncrypted) dao.commitVaultShell(vaultShell(record.book,
                "vault-source://${fileVault.sourceTag(existing.filePath)}"))
        }
        if (fileVault.records.value.isNotEmpty()) {
            purgeDeletedMetadata()
            File(context.cacheDir, "read").listFiles()?.forEach { it.delete() }
            remoteSource.clearCache()
            fileVault.committedIds().forEach { File(context.cacheDir, "covers/$it.jpg").delete() }
            fileVault.records.value.values.forEach { cleanupManagedVaultOriginal(it) }
        }
    }

    private fun vaultShell(book: BookEntity, sourceRef: String = "vault://${book.id}") = book.copy(
        title = "加密作品", author = "", series = "", format = "VAULT", coverUrl = "", altCoverUrl = "",
        currentPage = 1, totalPages = 1, progressPercent = 0, currentChapterTitle = "", statusBadge = "已加密",
        seriesBadge = "", fileSizeText = "", filePath = sourceRef, repositoryName = "保险库", updatedTimeText = "",
        synopsis = "", tagsCsv = "", shelfIdsCsv = "", isFavorite = false, isVaultProtected = true,
        isVaultEncrypted = true, lastReadTimestamp = 0, sizeBytes = 0, addedAt = 0, sourceModifiedAt = 0,
        repositoryId = "", sourceStatus = "AVAILABLE", contentFingerprint = "", coverPage = 1, seriesOrder = 0)

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun encryptBookFile(id: String) = withContext(Dispatchers.IO) {
        closeReadSession()
        localCopyMutex.withLock {
            val book = dao.getBookById(id) ?: error("作品已被移除")
            require(!book.isVaultEncrypted) { "作品已经加密" }
            val coroutine = currentCoroutineContext()
            val active = { coroutine.ensureActive() }
            val pages = if (book.format == "IMAGE_DIR") dao.getComicPages(id) else emptyList()
            val pageSizes = pages.map { page ->
                require(page.sizeBytes > 0) { "图片大小未知，请重新扫描后加密" }; page.sizeBytes
            }
            val pageNames = if (pages.isEmpty() && book.format !in setOf("PDF", "IMAGE"))
                ComicArchives.open(localCopyLocked(book), book.format).use { it.pageNames } else emptyList()
            val length = if (pages.isNotEmpty()) pageSizes.sum() else queryDocument(Uri.parse(book.filePath)).size
            require(length > 0) { "文件大小未知或为空，请重新选择来源" }
            val cover = File(book.coverUrl.removePrefix("file://")).takeIf { it.isFile && it.length() <= 1024 * 1024 }
                ?.readBytes()?.let { bytes -> try { android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP) }
                    finally { bytes.fill(0) } }.orEmpty()
            val record = VaultRecord(book, "", dao.getChapters(id), dao.getAnnotationsForBook(id)
                .map { it.copy(id = fileVault.nextAnnotationId()) }, dao.getReaderState(id) ?: ReaderStateEntity(id, page = book.currentPage), pageSizes, cover, pageNames, book.filePath)
            val input = {
                if (pages.isEmpty()) openSourceStream(book.filePath) else object : InputStream() {
                    var index = 0
                    var current: InputStream? = null
                    var remaining = 0L
                    override fun read(): Int { val one = ByteArray(1); return if (read(one) < 0) -1 else one[0].toInt() and 255 }
                    override fun read(bytes: ByteArray, offset: Int, count: Int): Int {
                        if (count == 0) return 0
                        while (true) {
                            active()
                            if (current == null) {
                                if (index >= pages.size) return -1
                                current = openSourceStream(pages[index].uri)
                                remaining = pageSizes[index++]
                            }
                            if (remaining == 0L) {
                                require(current!!.read() == -1) { "图片在加密时发生变化" }
                                current!!.close(); current = null; continue
                            }
                            val read = current!!.read(bytes, offset, minOf(count.toLong(), remaining).toInt())
                            require(read > 0) { "图片在加密时发生变化" }
                            remaining -= read
                            return read
                        }
                    }
                    override fun close() { current?.close() }
                }
            }
            val saved = fileVault.create(record, length, input, active)
            // 密文及元数据均已提交后，普通库仅保留随机标识与来源 HMAC。
            dao.commitVaultShell(vaultShell(saved.book, "vault-source://${fileVault.sourceTag(book.filePath)}"))
            purgeDeletedMetadata()
            cleanupManagedVaultOriginal(saved)
            File(context.cacheDir, "covers/$id.jpg").delete()
            // 阅读副本均为可再生缓存，清理防止原明文副本继续留在应用缓存。
            File(context.cacheDir, "read").listFiles()?.forEach { it.delete() }
            context.imageLoader.memoryCache?.clear()
            context.imageLoader.diskCache?.clear()
        }
    }

    private fun openSourceStream(uri: String): InputStream = resolver.openInputStream(Uri.parse(uri)) ?: error("来源无法读取，请重新授权")
    private suspend fun cleanupManagedVaultOriginal(record: VaultRecord) {
        val uri = Uri.parse(record.originalUri)
        if (uri.scheme != "file") return
        val source = File(uri.path.orEmpty()).canonicalFile
        val ownedRoot = File(context.filesDir, "imports").canonicalFile
        if (!source.toPath().startsWith(ownedRoot.toPath()) || !source.isFile) return
        val coroutine = currentCoroutineContext()
        val active = { coroutine.ensureActive(); check(fileVault.unlocked) }
        val verified = fileVault.openReader(record.book.id).let { reader ->
            try { reader.stream().use { digestStream(it, active) } } finally { fileVault.release(reader) }
        }
        val sourceDigest = source.inputStream().use { digestStream(it, active) }
        require(java.security.MessageDigest.isEqual(verified, sourceDigest)) { "应用持有副本已变化，保留源文件" }
        active()
        require(source.delete()) { "应用持有明文副本清理失败" }
    }
    private fun purgeDeletedMetadata() {
        val database = AsterDatabase.getDatabase(context).openHelper.writableDatabase
        database.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            require(cursor.moveToFirst() && cursor.getInt(0) == 0) { "数据库整理未完成，请保持原文件并重试" }
        }
        database.execSQL("VACUUM")
        database.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
    }

    suspend fun changeFileVaultPassword(current: CharArray, password: CharArray) = withContext(Dispatchers.IO) { fileVault.changePassword(current, password) }
    fun lockFileVault() { fileVault.lock(); trimPageMemory() }
    suspend fun verifyFileVault() = withContext(Dispatchers.IO) {
        val coroutine = currentCoroutineContext()
        for (record in fileVault.records.value.values) {
            val reader = fileVault.openReader(record.book.id)
            try {
                var offset = 0L
                while (offset < reader.length) {
                    coroutine.ensureActive()
                    val bytes = reader.read(offset, minOf(com.asterreader.core.vault.VaultCryptography.CHUNK_SIZE.toLong(), reader.length - offset).toInt())
                    offset += bytes.size; bytes.fill(0)
                }
            } finally { fileVault.release(reader) }
        }
    }

    suspend fun exportEncryptedBook(id: String, target: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val coroutine = currentCoroutineContext()
        val active = { coroutine.ensureActive(); check(fileVault.unlocked) { "保险库已锁定" } }
        val staging = File(context.noBackupFilesDir, "${UUID.randomUUID()}.astervault-part")
        try {
            java.io.FileOutputStream(staging).use { output -> fileVault.export(id, password, output, active); output.fd.sync() }
            val expected = staging.inputStream().use { input -> digestStream(input, active) }
            resolver.openOutputStream(target, "wt")?.use { output ->
                staging.inputStream().use { input -> copyStreamChecked(input, output, active) }
            } ?: error("目标不可写")
            val actual = resolver.openInputStream(target)?.use { digestStream(it, active) } ?: error("目标不能重读验证")
            require(java.security.MessageDigest.isEqual(expected, actual)) { "导出校验失败，请重新导出" }
        } finally { staging.delete() }
    }

    suspend fun restoreEncryptedBook(source: Uri, password: CharArray): BookEntity = withContext(Dispatchers.IO) {
        val coroutine = currentCoroutineContext()
        val saved = openSourceStream(source.toString()).use { fileVault.restore(it, password) { coroutine.ensureActive() } }
        dao.insertBook(vaultShell(saved.book))
        saved.book
    }

    private fun digestStream(input: InputStream, active: () -> Unit): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val sink = object : OutputStream() {
            override fun write(value: Int) { digest.update(value.toByte()) }
            override fun write(bytes: ByteArray, off: Int, len: Int) { digest.update(bytes, off, len) }
        }
        copyStreamChecked(input, sink, active)
        return digest.digest()
    }
    private fun copyStreamChecked(input: InputStream, output: OutputStream, active: () -> Unit) {
        val buffer = ByteArray(65536)
        try {
            while (true) { active(); val count = input.read(buffer); if (count < 0) break; require(count > 0); output.write(buffer, 0, count) }
        } finally { buffer.fill(0) }
    }

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
            currentCoroutineContext().ensureActive()
            dao.updateCover(book.id, path, book.coverPage)
            refreshed++
        }
        refreshed
    }

    suspend fun setCoverPage(bookId: String, page: Int): Boolean = withContext(Dispatchers.IO) {
        if (bookId in fileVault.committedIds()) {
            val book = getBookById(bookId) ?: return@withContext false
            if (page !in 1..book.totalPages) return@withContext false
            val bitmap = renderPage(bookId, page, 300 * 450)
            val memory = java.io.ByteArrayOutputStream()
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 86, memory)
                val bytes = memory.toByteArray()
                try { fileVault.update(bookId) { it.copy(book = it.book.copy(coverPage = page),
                    cover = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)) } }
                finally { bytes.fill(0) }
            } finally { memory.reset() }
            return@withContext true
        }
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
            files.filter { it.isFile }.sumOf { file -> val size = file.length(); if (file.delete()) size else 0L } +
                remoteSource.clearCache()
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
        if (book.format == "IMAGE_DIR" && DocumentsContract.isTreeUri(newUri)) {
            try {
                resolver.takePersistableUriPermission(newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { return@withContext false }
            return@withContext relocateFolderSource(book, newUri)
        }
        try {
            importDocument(newUri, book.repositoryName, book.repositoryId, persist = true,
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

    private data class DocumentInfo(val name: String, val size: Long, val modifiedAt: Long, val mimeType: String, val fingerprint: String = "")

    private data class ChildEntry(
        val documentId: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val modifiedAt: Long,
        val uriString: String,
        val fingerprint: String = ""
    ) {
        val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
        val extension: String get() = name.substringAfterLast('.', "").uppercase(Locale.ROOT)
        val uri: Uri get() = Uri.parse(uriString)
    }

    private data class ImagePage(val uri: Uri, val name: String, val size: Long, val modifiedAt: Long, val fingerprint: String = "")

    /** SAF 的单文档 URI 与目录授权 URI 可以指向同一 documentId。 */
    private suspend fun findDocumentSource(uri: Uri): BookEntity? {
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: return dao.getBookByUri(uri.toString())
        fun escape(value: String) = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val pattern = escape("content://${uri.authority}/") + "%document/" + escape(Uri.encode(documentId))
        return dao.getDocumentSourceCandidates(uri.toString(), pattern).filter { candidate ->
            val source = Uri.parse(candidate.filePath)
            source.authority == uri.authority && runCatching { DocumentsContract.getDocumentId(source) }.getOrNull() == documentId
        }.sortedByDescending { it.isVaultProtected || it.isVaultEncrypted }.firstOrNull()
    }

    private fun scanFailureReason(failure: Exception): String = when (failure) {
        is SecurityException -> "读取授权失效"
        is java.io.FileNotFoundException -> "来源文件不可读取"
        is java.util.zip.ZipException -> "ZIP 损坏、加密或压缩方式不支持"
        is IllegalArgumentException -> when {
            failure.message?.contains("页面") == true -> "归档内没有可读取的图片"
            else -> "文件类型或内容无效"
        }
        else -> "解析失败，请尝试单文件导入查看原因"
    }

    private fun resolveFormat(name: String, mimeType: String, uri: Uri): String {
        val declared = detectFormat(name, mimeType)
        val signature = sniffFormat(uri)
        return when {
            signature == "ZIP" && declared in setOf("CBZ", "ZIP", "EPUB") -> declared
            signature == "TAR" && declared in setOf("CBT", "TAR") -> declared
            signature == "RAR" && declared in setOf("CBR", "RAR") -> declared
            signature == "7Z" && declared in setOf("CB7", "7Z") -> declared
            else -> signature ?: declared
        }
    }

    /** 只读固定长度文件头，不解压、不按真实路径猜测 SAF 来源。 */
    private fun sniffFormat(uri: Uri): String? = resolver.openInputStream(uri)?.use { input ->
        val header = ByteArray(512)
        var size = 0
        while (size < header.size) {
            val read = input.read(header, size, header.size - size)
            if (read <= 0) break
            size += read
        }
        fun starts(vararg bytes: Int) = size >= bytes.size && bytes.indices.all { (header[it].toInt() and 255) == bytes[it] }
        when {
            starts(0x50, 0x4b, 3, 4) || starts(0x50, 0x4b, 5, 6) || starts(0x50, 0x4b, 7, 8) -> "ZIP"
            starts(0x52, 0x61, 0x72, 0x21, 0x1a, 7) -> "RAR"
            starts(0x37, 0x7a, 0xbc, 0xaf, 0x27, 0x1c) -> "7Z"
            starts(0x25, 0x50, 0x44, 0x46, 0x2d) -> "PDF"
            starts(0xff, 0xd8, 0xff) || starts(0x89, 0x50, 0x4e, 0x47) || starts(0x47, 0x49, 0x46, 0x38) || starts(0x42, 0x4d) -> "IMAGE"
            size >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" && String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "IMAGE"
            size >= 262 && String(header, 257, 5, Charsets.US_ASCII) == "ustar" -> "TAR"
            else -> null
        }
    }

    private fun detectFormat(name: String, mimeType: String): String {
        val extension = name.trim().substringAfterLast('.', "").uppercase(Locale.ROOT)
        if (extension in ARCHIVE_EXTENSIONS || extension == "PDF") return extension
        if (extension in IMAGE_EXTENSIONS) return "IMAGE"
        return when (mimeType.substringBefore(';').trim().lowercase(Locale.ROOT)) {
            "application/pdf" -> "PDF"
            "application/vnd.comicbook+zip", "application/zip", "application/x-zip-compressed" -> "CBZ"
            "application/vnd.comicbook-rar", "application/vnd.rar", "application/x-rar-compressed" -> "CBR"
            "application/x-7z-compressed" -> "CB7"
            "application/x-tar" -> "CBT"
            "application/epub+zip" -> "EPUB"
            "image/jpeg", "image/png", "image/webp", "image/bmp", "image/gif",
            "image/avif", "image/heic", "image/heif" -> "IMAGE"
            else -> extension
        }
    }

    private fun queryWithOptionalModified(uri: Uri, columns: Array<String>, signal: android.os.CancellationSignal? = null): android.database.Cursor? =
        try { if (WebDavRepository.isRemote(uri)) resolver.query(uri, columns, null as android.os.Bundle?, signal)
            else resolver.query(uri, columns, null, null, null, signal) }
        catch (_: IllegalArgumentException) {
            resolver.query(uri, columns.filter { it != DocumentsContract.Document.COLUMN_LAST_MODIFIED }.toTypedArray(), null, null, null, signal)
        }

    private fun queryDocument(uri: Uri): DocumentInfo {
        // 外部「打开方式」偶尔会给 file:// URI：ContentResolver 查不到元数据，直接读文件属性
        if (uri.scheme == "file") {
            val file = File(uri.path.orEmpty())
            require(file.isFile) { "文件不可访问，请重新选择" }
            return DocumentInfo(file.name, file.length(), file.lastModified(), resolver.getType(uri).orEmpty())
        }
        return queryWithOptionalModified(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED) +
                if (WebDavRepository.isRemote(uri)) arrayOf("webdav_etag") else emptyArray(),
        )?.use { cursor ->
            check(cursor.moveToFirst()) { "文档不可访问" }
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            DocumentInfo(
                name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex)
                    else uri.lastPathSegment.orEmpty().substringAfterLast('/').substringAfterLast(':').ifBlank { "未命名" },
                size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L,
                modifiedAt = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L,
                mimeType = resolver.getType(uri).orEmpty(),
                fingerprint = cursor.getColumnIndex("webdav_etag").takeIf { it >= 0 }?.let { cursor.getString(it) }.orEmpty()
            )
        } ?: error("无法读取文档信息")
    }

    private fun documentDisplayName(uri: Uri): String? =
        queryWithOptionalModified(uri, arrayOf(OpenableColumns.DISPLAY_NAME))?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private suspend fun queryChildren(treeUri: Uri, parentId: String, limit: Int = MAX_SCAN_ITEMS): List<ChildEntry> =
        if (WebDavRepository.isRemote(treeUri)) {
            val (id, path) = WebDavRepository.decodeId(parentId)
            val entries = remoteSource.list(id, path)
            check(entries.size <= limit) { "单次扫描项数超限，请缩小目录" }
            entries.map { entry -> ChildEntry(WebDavRepository.documentId(id, entry.path), entry.name,
                if (entry.directory) DocumentsContract.Document.MIME_TYPE_DIR else entry.mime, entry.size.coerceAtLeast(0), entry.modifiedAt,
                DocumentsContract.buildDocumentUriUsingTree(treeUri, WebDavRepository.documentId(id, entry.path)).toString(), entry.revision) }
        } else kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val signal = android.os.CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
                val result = ArrayList<ChildEntry>()
                queryWithOptionalModified(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    ),
                    signal
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    check(idIndex >= 0 || cursor.count == 0) { "文件提供者没有返回文件标识，无法扫描此目录" }
                    while (cursor.moveToNext()) {
                        continuation.context.ensureActive()
                        check(result.size < limit) { "单次扫描最多处理 $MAX_SCAN_ITEMS 项，请缩小授权范围" }
                        val documentId = if (idIndex >= 0) cursor.getString(idIndex) else null
                        if (documentId == null) continue
                        val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex).orEmpty() else ""
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty()
                            else documentId.substringAfterLast('/').substringAfterLast(':')
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
                continuation.resumeWith(Result.success(result))
            } catch (failure: Exception) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(failure))
            }
        }

    private suspend fun listImages(treeUri: Uri, folderUri: Uri): List<ImagePage> {
        val folderId = DocumentsContract.getDocumentId(folderUri)
        return queryChildren(treeUri, folderId)
            .filter { !it.isDirectory && resolveFormat(it.name, it.mimeType, it.uri) == "IMAGE" }
            .sortedWith { a, b -> naturalCompare(a.name, b.name) }
            .map { ImagePage(it.uri, it.name, it.size, it.modifiedAt, it.fingerprint) }
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
        return renderer.use { generatePdfCover(it, page, target) }
    }

    private fun generatePdfCover(renderer: PdfRenderer, page: Int, target: File): String? {
        return renderer.let {
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

    private class LocalDocument(val file: File, private val descriptor: ParcelFileDescriptor? = null) : java.io.Closeable {
        override fun close() { descriptor?.close() }
    }

    /** 保持描述符打开，使引擎能通过进程内 fd 随机读 SAF 原件，不解析外部真实路径。 */
    private fun openLocalDocument(uri: Uri): LocalDocument? {
        if (uri.scheme == "file") return LocalDocument(File(uri.path ?: error("源文件不可访问")))
        if (uri.scheme != "content" || WebDavRepository.isRemote(uri)) return null
        if (!File("/proc/self/fd").isDirectory) return null
        val descriptor = try { resolver.openFileDescriptor(uri, "r") }
            catch (_: java.io.IOException) { null }
            catch (_: UnsupportedOperationException) { null }
            ?: return null
        try {
            android.system.Os.lseek(descriptor.fileDescriptor, 0, android.system.OsConstants.SEEK_CUR)
            val file = File("/proc/self/fd/${descriptor.fd}")
            if (file.isFile && file.canRead()) return LocalDocument(file, descriptor)
        } catch (_: android.system.ErrnoException) {
            // 管道等来源不支持随机访问，交给流式兼容路径。
        } catch (failure: Throwable) {
            descriptor.close()
            throw failure
        }
        descriptor.close()
        return null
    }

    private suspend fun <T> withTemporaryCopy(uri: Uri, block: (File) -> T): T {
        openLocalDocument(uri)?.use { direct ->
            currentCoroutineContext().ensureActive()
            return block(direct.file)
        }
        val file = copyDocument(uri)
        return try {
            currentCoroutineContext().ensureActive()
            block(file)
        } finally {
            file.delete()
        }
    }

    private fun <T> withTemporaryCopySync(uri: Uri, block: (File) -> T): T {
        openLocalDocument(uri)?.use { return block(it.file) }
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
    private suspend fun localCopy(book: BookEntity): File = localCopyMutex.withLock { localCopyLocked(book) }

    suspend fun invalidateRemoteReadCopies(connectionId: String) = withContext(Dispatchers.IO) {
        closeReadSession()
        localCopyMutex.withLock {
            dao.getAllBooks().filter { book ->
                val uri = Uri.parse(book.filePath)
                WebDavRepository.isRemote(uri) && runCatching {
                    WebDavRepository.decodeId(DocumentsContract.getDocumentId(uri)).first == connectionId
                }.getOrDefault(false)
            }.forEach { book -> File(context.cacheDir, "read/${localCopyName(book)}").delete() }
        }
    }

    private fun localCopyName(book: BookEntity): String {
        val identity = "${book.id}:${book.filePath}:${book.sizeBytes}:${book.sourceModifiedAt}:${book.contentFingerprint}"
        return java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) } + ".arc"
    }

    /** 调用者必须持有 localCopyMutex，防止加密提交与明文缓存重建并发。 */
    private suspend fun localCopyLocked(book: BookEntity): File {
        val dir = File(context.cacheDir, "read").apply { mkdirs() }
        val name = localCopyName(book)
        val target = File(dir, name)
        if (book.sourceModifiedAt > 0 && book.sizeBytes > 0 && target.isFile && target.length() > 0) {
            target.setLastModified(System.currentTimeMillis())
            // 上次复制中断留下的 .part 不该一直占着缓存额度
            dir.listFiles()?.filter {
                it.name.endsWith(".part") && System.currentTimeMillis() - it.lastModified() > 3_600_000L
            }?.forEach { it.delete() }
            return target
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
        return target
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
            val sourceStream: InputStream? = if (WebDavRepository.isRemote(uri)) {
                ParcelFileDescriptor.AutoCloseInputStream(remoteSource.openDescriptor(DocumentsContract.getDocumentId(uri)))
            } else if (uri.scheme == "file") {
                runCatching { FileInputStream(File(uri.path.orEmpty())) }.getOrNull()
            } else {
                resolver.openInputStream(uri)
            }
            sourceStream?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
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

    suspend fun encryptedLibraryBytes(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val memory = object : java.io.ByteArrayOutputStream() {
            fun clear() { buf.fill(0); reset() }
        }
        try {
            LibraryBackup.export(dao, memory)
            val plaintext = memory.toByteArray()
            try { EncryptedLibraryBackup.encrypt(plaintext, password) } finally { plaintext.fill(0) }
        } finally { memory.clear() }
    }

    suspend fun restoreEncryptedLibrary(stream: InputStream, password: CharArray): LibraryBackup.ImportResult =
        withContext(Dispatchers.IO) {
            val plaintext = EncryptedLibraryBackup.decrypt(stream, password)
            try { LibraryBackup.import(dao, java.io.ByteArrayInputStream(plaintext)) }
            finally { plaintext.fill(0) }
        }

    suspend fun importLibraryFrom(stream: InputStream): LibraryBackup.ImportResult =
        withContext(Dispatchers.IO) { LibraryBackup.import(dao, stream) }

    companion object {
        const val PDF_PREVIEW_PIXELS = 600_000
        const val DEFAULT_PAGE_PIXELS = 2_500_000
        const val ZOOMED_PAGE_PIXELS = 6_000_000
        private const val MAX_LOCAL_COPY_BYTES = 1024L * 1024 * 1024
        private const val MAX_SCAN_ITEMS = 10_000

        /** UI 与扫描实现共享，避免两处硬编码规则字符串不一致。 */
        const val DEFAULT_SCAN_RULE = "每个文件作为作品"
        val SUPPORTED_SCAN_RULES = setOf(DEFAULT_SCAN_RULE, "单文件单作品")

        /** 真正可读的容器：ZIP/CBZ、TAR/CBT、7Z/CB7、RAR/CBR、EPUB、PDF、单图、散图目录。 */
        val READABLE_FORMATS = setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "PDF", "EPUB", "IMAGE", "IMAGE_DIR")
        private val ARCHIVE_EXTENSIONS = setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "EPUB", "PDF")
        val IMAGE_EXTENSIONS = setOf("JPG", "JPEG", "JPE", "JFIF", "PNG", "WEBP", "BMP", "GIF", "AVIF", "HEIC", "HEIF")

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
