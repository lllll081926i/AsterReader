package com.example.data.local

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import com.asterreader.core.database.RepositoryEntity
import com.asterreader.core.database.ShelfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import java.util.zip.ZipFile

/** 所有文档访问都在数据层执行，来源始终保留为 SAF URI。 */
class LibraryRepository(
    private val context: Context,
    private val dao: AsterDao = AsterDatabase.getDatabase(context).asterDao()
) {
    private val resolver = context.contentResolver

    fun observeAllBooks() = dao.observeAllBooks()
    fun observePublicBooks() = dao.observePublicBooks()
    fun observeAccessibleBookById(id: String, unlocked: Boolean) = dao.observeAccessibleBookById(id, unlocked)
    fun observeAllShelves() = dao.observeAllShelves()
    fun observeAllAnnotations() = dao.observeAllAnnotations()
    fun observeVisibleAnnotations(unlocked: Boolean) = dao.observeVisibleAnnotations(unlocked)
    fun observeAllRepositories() = dao.observeAllRepositories()
    fun observeChaptersForBook(id: String) = dao.observeChapterSummaries(id)
    fun observeAnnotationsForBook(id: String) = dao.observeAnnotationsForBook(id)
    suspend fun getBookById(id: String) = dao.getBookById(id)
    suspend fun updateBook(book: BookEntity) = dao.updateBook(book)
    suspend fun toggleFavorite(id: String) = dao.toggleFavorite(id)
    suspend fun updateProgress(id: String, page: Int, percent: Int, badge: String, chapter: String?, time: Long) =
        dao.updateProgress(id, page, percent, badge, chapter, time)
    suspend fun insertShelf(shelf: ShelfEntity) = dao.insertShelf(shelf)
    suspend fun deleteShelfById(id: String) = dao.deleteShelfById(id)
    suspend fun updateShelf(shelf: ShelfEntity) = dao.updateShelf(shelf)
    suspend fun insertAnnotation(annotation: AnnotationEntity) = dao.insertAnnotation(annotation)
    suspend fun deleteAnnotationById(id: Int) = dao.deleteAnnotationById(id)
    suspend fun removeBookmark(id: String, page: Int) = dao.removeBookmark(id, page)
    suspend fun removeBooks(ids: List<String>) = dao.removeBooks(ids)
    suspend fun deleteRepository(id: String) = dao.deleteRepositoryById(id)

    /**
     * 重新生成所有书籍的封面缩略图
     */
    suspend fun refreshAllBookCovers(): Int = withContext(Dispatchers.IO) {
        val books = dao.observeAllBooks().first()
        var refreshed = 0
        books.forEach { book ->
            val coverCacheFile = File(context.cacheDir, "covers/${book.id}.jpg")
            coverCacheFile.parentFile?.mkdirs()
            try {
                val uri = Uri.parse(book.filePath)
                val format = book.format
                var generated = false
                if (format in setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "EPUB", "MOBI")) {
                    withTemporaryCopy(uri) { file ->
                        try {
                            ZipFile(file).use { zip ->
                                val firstEntry = imageEntries(zip).firstOrNull()
                                if (firstEntry != null) {
                                    val stream = zip.getInputStream(firstEntry)
                                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                                    if (bmp != null) {
                                        coverCacheFile.outputStream().use { out ->
                                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                        }
                                        bmp.recycle()
                                        generated = true
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } else if (format == "PDF") {
                    withTemporaryCopy(uri) { file ->
                        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = try { PdfRenderer(descriptor) } catch (f: Throwable) { descriptor.close(); throw f }
                        renderer.use {
                            if (it.pageCount > 0) {
                                it.openPage(0).use { page ->
                                    val bmp = Bitmap.createBitmap(page.width.coerceAtMost(720), page.height.coerceAtMost(1080), Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(android.graphics.Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    coverCacheFile.outputStream().use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    }
                                    bmp.recycle()
                                    generated = true
                                }
                            }
                        }
                    }
                }
                if (generated) {
                    dao.updateBook(book.copy(coverUrl = coverCacheFile.absolutePath))
                    refreshed++
                }
            } catch (_: Exception) {}
        }
        refreshed
    }
    suspend fun relocateBookSource(bookId: String, newUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val book = dao.getBookById(bookId) ?: return@withContext false
        try {
            resolver.takePersistableUriPermission(newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val updated = book.copy(
                filePath = newUri.toString(),
                sourceStatus = "AVAILABLE",
                updatedTimeText = "已重新关联"
            )
            dao.updateBook(updated)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 智能识别系列名称与卷序
     * 例如："葬送的芙莉莲 卷11" -> ("葬送的芙莉莲", 11)
     */
    fun parseSeriesAndVolume(fileName: String): Pair<String, Int> {
        val cleanName = fileName.substringBeforeLast('.', fileName).trim()
        val volumePattern = Pattern.compile("(.*?)(?:[vV][oO][lL]\\.?|卷|第|#|Vol\\.)?\\s*([0-9]{1,4})\\s*(?:卷|话|集|章)?$")
        val matcher = volumePattern.matcher(cleanName)
        return if (matcher.matches()) {
            val series = matcher.group(1)?.trim().orEmpty().ifBlank { cleanName }
            val volume = matcher.group(2)?.toIntOrNull() ?: 1
            series to volume
        } else {
            cleanName to 1
        }
    }

    suspend fun importDocument(
        uri: Uri,
        repositoryName: String = "本地文档",
        persist: Boolean = true,
        refreshExisting: Boolean = false
    ): BookEntity = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "请选择系统文件提供者中的文档" }
        if (persist) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {}
        }
        val existing = dao.getBookByUri(uri.toString())
        if (existing != null && !refreshExisting) return@withContext existing
        require(existing == null || (!existing.isVaultProtected && !existing.isVaultEncrypted)) { "请先解除访问保护后重新扫描" }
        val info = resolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null,
            null,
            null
        )?.use { cursor ->
            check(cursor.moveToFirst()) { "文档不可访问" }
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            Triple(
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)),
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let(cursor::getLong) ?: 0L,
                if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else 0L
            )
        } ?: error("无法读取文档信息")

        if (existing != null && info.third > 0 && existing.sizeBytes == info.second && existing.sourceModifiedAt == info.third) {
            return@withContext existing
        }
        require(info.second <= 512L * 1024 * 1024) { "当前版本单文件上限为 512 MB" }
        val format = info.first.substringAfterLast('.', "").uppercase(Locale.ROOT)
        require(format in SUPPORTED_FORMATS) { "此格式尚不支持读取" }
        val id = existing?.id ?: UUID.randomUUID().toString()
        val pages = when (format) {
            "CBZ", "ZIP" -> {
                withTemporaryCopy(uri) { file ->
                    ZipFile(file).use { imageEntries(it).size }
                }
            }
            "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "EPUB", "MOBI" -> {
                withTemporaryCopy(uri) { file ->
                    try {
                        ZipFile(file).use { imageEntries(it).size.coerceAtLeast(1) }
                    } catch (_: Exception) {
                        1
                    }
                }
            }
            "PDF" -> withTemporaryCopy(uri) { file ->
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = try {
                    PdfRenderer(descriptor)
                } catch (failure: Throwable) {
                    descriptor.close(); throw failure
                }
                renderer.use { it.pageCount }
            }
            else -> error("不支持的漫画格式")
        }
        require(pages > 0) { "文档没有可读取的页面" }
        val now = System.currentTimeMillis()
        val (seriesName, volOrder) = parseSeriesAndVolume(info.first)

        // 生成本地封面缓存文件，从第 1 页或首张图片生成轻量缩略图
        val coverCacheFile = File(context.cacheDir, "covers/${id}.jpg")
        var coverPath = existing?.coverUrl.orEmpty()
        if (coverPath.isBlank() || !File(coverPath).exists()) {
            try {
                coverCacheFile.parentFile?.mkdirs()
                if (format in setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "EPUB", "MOBI")) {
                    withTemporaryCopy(uri) { file ->
                        try {
                            ZipFile(file).use { zip ->
                                val firstEntry = imageEntries(zip).firstOrNull()
                                if (firstEntry != null) {
                                    val stream = zip.getInputStream(firstEntry)
                                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                                    if (bmp != null) {
                                        coverCacheFile.outputStream().use { out ->
                                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                        }
                                        bmp.recycle()
                                        coverPath = coverCacheFile.absolutePath
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } else if (format == "PDF") {
                    withTemporaryCopy(uri) { file ->
                        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = try { PdfRenderer(descriptor) } catch (f: Throwable) { descriptor.close(); throw f }
                        renderer.use {
                            if (it.pageCount > 0) {
                                it.openPage(0).use { page ->
                                    val bmp = Bitmap.createBitmap(page.width.coerceAtMost(720), page.height.coerceAtMost(1080), Bitmap.Config.ARGB_8888)
                                    bmp.eraseColor(android.graphics.Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    coverCacheFile.outputStream().use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    }
                                    bmp.recycle()
                                    coverPath = coverCacheFile.absolutePath
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val book = BookEntity(
            id = id,
            title = info.first.substringBeforeLast('.', info.first),
            author = "",
            series = seriesName,
            format = format,
            coverUrl = coverPath,
            currentPage = existing?.currentPage ?: 1,
            totalPages = pages,
            progressPercent = existing?.progressPercent ?: 0,
            currentChapterTitle = existing?.currentChapterTitle ?: "第 1 页",
            statusBadge = existing?.statusBadge ?: "未读",
            seriesBadge = if (volOrder > 1) "卷 $volOrder" else "",
            fileSizeText = formatSize(info.second),
            filePath = uri.toString(),
            repositoryName = repositoryName,
            updatedTimeText = "刚刚导入",
            synopsis = "",
            tagsCsv = "",
            shelfIdsCsv = existing?.shelfIdsCsv ?: "",
            sizeBytes = info.second,
            addedAt = existing?.addedAt ?: now,
            sourceModifiedAt = info.third,
            sourceStatus = "AVAILABLE",
            seriesOrder = volOrder
        )
        currentCoroutineContext().ensureActive()
        dao.importComic(book)
    }

    private val renderSession = DocumentRenderSession(context)

    suspend fun renderPage(id: String, page: Int): Bitmap = withContext(Dispatchers.IO) {
        val book = dao.getBookById(id) ?: error("作品已被移除")
        require(!book.isVaultProtected && !book.isVaultEncrypted) { "受保护文件不能通过普通文档源打开" }
        require(page in 1..book.totalPages) { "页码超出范围" }
        val uri = Uri.parse(book.filePath)
        renderSession.render(
            book,
            page,
            "${book.id}:${book.filePath}:${book.sizeBytes}:${book.sourceModifiedAt}",
            copySource = { copyDocument(uri) },
            listImages = { zip -> imageEntries(zip).map { it.name } }
        )
    }

    fun trimPageMemory() = renderSession.trimMemory()
    suspend fun closeReadSession() = renderSession.close()

    suspend fun scanRepository(uri: Uri, name: String, rule: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        require(rule == "每个文件作为作品" || rule == "单文件单作品") { "当前仅支持每个文件作为作品，请重新添加仓库" }
        require(DocumentsContract.isTreeUri(uri)) { "请选择仓库目录" }
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {}

        val imported = java.util.concurrent.atomic.AtomicInteger()
        val failed = java.util.concurrent.atomic.AtomicInteger()
        val repoId = UUID.nameUUIDFromBytes(uri.toString().toByteArray()).toString()
        val pendingRepo = RepositoryEntity(
            repoId, name.ifBlank { "本地目录" }, uri.toString(), "SMARTPHONE",
            0, "", "扫描中", rule, false
        )
        dao.insertRepository(pendingRepo)
        try {
            kotlinx.coroutines.coroutineScope {
                val pending = kotlinx.coroutines.channels.Channel<Uri>(capacity = 4)
                val workers = List(2) {
                    launch {
                        for (document in pending) {
                            currentCoroutineContext().ensureActive()
                            try {
                                importDocument(document, name, persist = false, refreshExisting = true)
                                imported.incrementAndGet()
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                failed.incrementAndGet()
                            }
                        }
                    }
                }
                try {
                    val queue = ArrayDeque<String>()
                    val visited = mutableSetOf<String>()
                    val documents = mutableSetOf<String>()
                    queue.add(DocumentsContract.getTreeDocumentId(uri))
                    var enumerated = 0
                    while (queue.isNotEmpty()) {
                        currentCoroutineContext().ensureActive()
                        val parent = queue.removeFirst()
                        if (!visited.add(parent)) continue
                        val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, parent)
                        resolver.query(
                            children,
                            arrayOf(
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_MIME_TYPE,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME
                            ),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            while (cursor.moveToNext()) {
                                currentCoroutineContext().ensureActive()
                                check(++enumerated <= 10_000) { "单次扫描最多处理 10000 项，请缩小授权范围" }
                                val childId = cursor.getString(0)
                                val mime = cursor.getString(1)
                                val displayName = cursor.getString(2) ?: ""
                                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                                    // 递归深入子文件夹（如按卷、按话划分的独立子目录）
                                    queue.add(childId)
                                } else if (documents.add(childId)) {
                                    val ext = displayName.substringAfterLast('.', "").uppercase(Locale.ROOT)
                                    if (ext in SUPPORTED_FORMATS || ext in IMAGE_EXTENSIONS) {
                                        pending.send(DocumentsContract.buildDocumentUriUsingTree(uri, childId))
                                    }
                                }
                            }
                        } ?: error("目录不可访问，请重新授权")
                    }
                } finally {
                    pending.close()
                }
                workers.forEach { it.join() }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            withContext(kotlinx.coroutines.NonCancellable) {
                dao.insertRepository(pendingRepo.copy(bookCount = imported.get(), lastScanText = "扫描已取消"))
            }
            throw cancelled
        } catch (failure: Exception) {
            dao.insertRepository(pendingRepo.copy(bookCount = imported.get(), lastScanText = "扫描未完成"))
            throw failure
        }
        currentCoroutineContext().ensureActive()
        dao.insertRepository(
            RepositoryEntity(
                repoId, name.ifBlank { "本地目录" }, uri.toString(), "SMARTPHONE",
                imported.get(), "", "${java.time.Instant.now()}", rule, failed.get() == 0
            )
        )
        imported.get() to failed.get()
    }

    private fun imageEntries(zip: ZipFile) = zip.entries().asSequence()
        .take(10_001).toList().also { require(it.size <= 10_000) { "压缩包条目过多" } }
        .filter { !it.isDirectory && it.name.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif") }
        .sortedWith { a, b -> naturalCompare(a.name, b.name) }

    private suspend fun <T> withTemporaryCopy(uri: Uri, block: (File) -> T): T {
        val file = copyDocument(uri)
        return try {
            currentCoroutineContext().ensureActive()
            block(file)
        } finally {
            file.delete()
        }
    }

    private suspend fun copyDocument(uri: Uri): File {
        require(uri.scheme == "content") { "源文件不可访问，请重新导入" }
        val file = File.createTempFile("document-", ".tmp", context.cacheDir)
        try {
            resolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var size = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= 512L * 1024 * 1024) { "当前版本单文件上限为 512 MB" }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("无法读取源文件，请重新授权")
            currentCoroutineContext().ensureActive()
            return file
        } catch (failure: Throwable) {
            file.delete()
            throw failure
        }
    }

    companion object {
        // 全量漫画与阅读格式支持：CBZ, ZIP, CBR, RAR, CB7, 7Z, CBT, TAR, PDF, EPUB, MOBI
        val SUPPORTED_FORMATS = setOf(
            "CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "PDF", "EPUB", "MOBI"
        )
        val IMAGE_EXTENSIONS = setOf(
            "JPG", "JPEG", "PNG", "WEBP", "BMP", "GIF", "AVIF"
        )
        fun formatSize(bytes: Long): String = if (bytes <= 0) "大小未知" else String.format(Locale.ROOT, "%.2f MB", bytes / 1048576.0)
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
