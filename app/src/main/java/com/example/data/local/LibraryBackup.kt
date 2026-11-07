package com.example.data.local

import com.asterreader.core.database.AsterDao
import com.asterreader.core.database.AnnotationEntity
import com.asterreader.core.database.BookEntity
import com.asterreader.core.database.ChapterEntity
import com.asterreader.core.database.ReaderStateEntity
import com.asterreader.core.database.RepositoryEntity
import com.asterreader.core.database.ShelfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

/**
 * 书库备份 / 恢复：只导出元数据（书名、进度、标签、书架、批注、阅读状态），
 * **不导出** PIN、隐藏内容的访问凭据，也不复制漫画文件本身。
 *
 * 换机后 SAF 的 content:// 授权不会跟着迁移，所以恢复进来的作品一律标记 MISSING，
 * 用户在详情页用「重新定位来源」重新选一次文件即可继续读，进度不会丢。
 */
object LibraryBackup {

    private const val FORMAT = "aster-library"
    private const val VERSION = 1

    data class ImportResult(
        val restored: Int,
        val updated: Int,
        val annotations: Int,
        val shelves: Int
    )

    suspend fun export(dao: AsterDao, output: OutputStream) = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("notice", "仅含书库元数据，不含漫画文件与应用 PIN；恢复后需重新授权来源目录。")

        root.put("books", JSONArray().also { array ->
            dao.getAllBooks().forEach { book ->
                array.put(
                    JSONObject()
                        .put("id", book.id)
                        .put("title", book.title)
                        .put("author", book.author)
                        .put("series", book.series)
                        .put("format", book.format)
                        .put("currentPage", book.currentPage)
                        .put("totalPages", book.totalPages)
                        .put("progressPercent", book.progressPercent)
                        .put("currentChapterTitle", book.currentChapterTitle)
                        .put("statusBadge", book.statusBadge)
                        .put("seriesBadge", book.seriesBadge)
                        .put("fileSizeText", book.fileSizeText)
                        .put("filePath", book.filePath)
                        .put("repositoryName", book.repositoryName)
                        .put("updatedTimeText", book.updatedTimeText)
                        .put("synopsis", book.synopsis)
                        .put("tagsCsv", book.tagsCsv)
                        .put("shelfIdsCsv", book.shelfIdsCsv)
                        .put("isFavorite", book.isFavorite)
                        .put("isVaultProtected", book.isVaultProtected)
                        .put("lastReadTimestamp", book.lastReadTimestamp)
                        .put("sizeBytes", book.sizeBytes)
                        .put("addedAt", book.addedAt)
                        .put("sourceModifiedAt", book.sourceModifiedAt)
                        .put("repositoryId", book.repositoryId)
                        .put("sourceStatus", book.sourceStatus)
                        .put("contentFingerprint", book.contentFingerprint)
                        .put("coverPage", book.coverPage)
                        .put("seriesOrder", book.seriesOrder)
                )
            }
        })
        root.put("shelves", JSONArray().also { array ->
            dao.getAllShelves().forEach { shelf ->
                array.put(
                    JSONObject().put("id", shelf.id).put("name", shelf.name)
                        .put("subtitle", shelf.subtitle).put("itemCountDisplay", shelf.itemCountDisplay)
                        .put("updatedLabel", shelf.updatedLabel).put("coverUrlsCsv", shelf.coverUrlsCsv)
                        .put("isProtected", shelf.isProtected).put("sortOrder", shelf.sortOrder)
                )
            }
        })
        root.put("annotations", JSONArray().also { array ->
            dao.getAllAnnotations().forEach { note ->
                array.put(
                    JSONObject().put("bookId", note.bookId).put("chapterTitle", note.chapterTitle)
                        .put("pageNumber", note.pageNumber).put("quoteText", note.quoteText)
                        .put("noteText", note.noteText).put("colorName", note.colorName)
                        .put("type", note.type).put("createdAtText", note.createdAtText)
                )
            }
        })
        root.put("readerStates", JSONArray().also { array ->
            dao.getAllReaderStates().forEach { state ->
                array.put(
                    JSONObject().put("bookId", state.bookId).put("page", state.page)
                        .put("offsetFraction", state.offsetFraction.toDouble()).put("scale", state.scale.toDouble())
                        .put("panX", state.panX.toDouble()).put("panY", state.panY.toDouble())
                        .put("layoutMode", state.layoutMode ?: JSONObject.NULL)
                        .put("fitMode", state.fitMode ?: JSONObject.NULL)
                )
            }
        })
        root.put("chapters", JSONArray().also { array ->
            dao.getAllChapters().forEach { chapter ->
                array.put(
                    JSONObject().put("id", chapter.id).put("bookId", chapter.bookId)
                        .put("chapterNumber", chapter.chapterNumber).put("title", chapter.title)
                        .put("startPage", chapter.startPage).put("endPage", chapter.endPage)
                        .put("isCompleted", chapter.isCompleted).put("isCurrent", chapter.isCurrent)
                )
            }
        })
        root.put("repositories", JSONArray().also { array ->
            dao.getAllRepositories().forEach { repo ->
                array.put(
                    JSONObject().put("id", repo.id).put("name", repo.name)
                        .put("pathUri", repo.pathUri).put("storageType", repo.storageType)
                        .put("bookCount", repo.bookCount).put("sizeText", repo.sizeText)
                        .put("scanRule", repo.scanRule)
                )
            }
        })
        output.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
    }

    suspend fun import(dao: AsterDao, input: InputStream): ImportResult = withContext(Dispatchers.IO) {
        val text = input.use { String(it.readBytes(), Charsets.UTF_8) }
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "不是 Aster Reader 书库备份文件" }
        require(root.optInt("version", 0) <= VERSION) { "备份文件版本过新，请升级应用后重试" }

        val existing = dao.getAllBooks().associateBy { it.id }
        var restored = 0
        var updated = 0
        val booksArray = root.optJSONArray("books") ?: JSONArray()
        for (i in 0 until booksArray.length()) {
            val item = booksArray.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isBlank()) continue
            val current = existing[id]
            if (current != null) {
                // 已有作品：只恢复用户产生的状态，保留本机的封面与来源 URI
                dao.updateBook(
                    current.copy(
                        currentPage = item.optInt("currentPage", current.currentPage),
                        progressPercent = item.optInt("progressPercent", current.progressPercent),
                        currentChapterTitle = item.optString("currentChapterTitle", current.currentChapterTitle),
                        statusBadge = item.optString("statusBadge", current.statusBadge),
                        lastReadTimestamp = item.optLong("lastReadTimestamp", current.lastReadTimestamp),
                        isFavorite = item.optBoolean("isFavorite", current.isFavorite),
                        tagsCsv = item.optString("tagsCsv", current.tagsCsv),
                        shelfIdsCsv = item.optString("shelfIdsCsv", current.shelfIdsCsv),
                        series = item.optString("series", current.series),
                        seriesOrder = item.optInt("seriesOrder", current.seriesOrder),
                        synopsis = item.optString("synopsis", current.synopsis)
                    )
                )
                updated++
            } else {
                dao.insertBook(
                    BookEntity(
                        id = id,
                        title = item.optString("title", "未命名"),
                        author = item.optString("author", ""),
                        series = item.optString("series", ""),
                        format = item.optString("format", "CBZ"),
                        coverUrl = "",
                        altCoverUrl = "",
                        currentPage = item.optInt("currentPage", 1),
                        totalPages = item.optInt("totalPages", 1).coerceAtLeast(1),
                        progressPercent = item.optInt("progressPercent", 0),
                        currentChapterTitle = item.optString("currentChapterTitle", ""),
                        statusBadge = item.optString("statusBadge", "未读"),
                        seriesBadge = item.optString("seriesBadge", ""),
                        fileSizeText = item.optString("fileSizeText", ""),
                        filePath = item.optString("filePath", ""),
                        repositoryName = item.optString("repositoryName", ""),
                        updatedTimeText = item.optString("updatedTimeText", ""),
                        synopsis = item.optString("synopsis", ""),
                        tagsCsv = item.optString("tagsCsv", ""),
                        shelfIdsCsv = item.optString("shelfIdsCsv", ""),
                        isFavorite = item.optBoolean("isFavorite", false),
                        isVaultProtected = item.optBoolean("isVaultProtected", false),
                        lastReadTimestamp = item.optLong("lastReadTimestamp", 0L),
                        sizeBytes = item.optLong("sizeBytes", 0L),
                        addedAt = item.optLong("addedAt", System.currentTimeMillis()),
                        sourceModifiedAt = item.optLong("sourceModifiedAt", 0L),
                        repositoryId = item.optString("repositoryId", ""),
                        sourceStatus = "MISSING",
                        contentFingerprint = item.optString("contentFingerprint", ""),
                        coverPage = item.optInt("coverPage", 1).coerceAtLeast(1),
                        seriesOrder = item.optInt("seriesOrder", 0)
                    )
                )
                restored++
            }
        }

        val shelves = parseArray(root, "shelves") { item ->
            ShelfEntity(
                id = item.optString("id"), name = item.optString("name"),
                subtitle = item.optString("subtitle", ""),
                itemCountDisplay = item.optInt("itemCountDisplay", 0),
                updatedLabel = item.optString("updatedLabel", ""),
                coverUrlsCsv = item.optString("coverUrlsCsv", ""),
                isProtected = item.optBoolean("isProtected", false),
                sortOrder = item.optInt("sortOrder", 0)
            )
        }
        if (shelves.isNotEmpty()) dao.insertShelves(shelves)

        val annotations = parseArray(root, "annotations") { item ->
            AnnotationEntity(
                id = 0, // 让本机重新分配主键，避免覆盖已有批注
                bookId = item.optString("bookId"),
                chapterTitle = item.optString("chapterTitle", ""),
                pageNumber = item.optInt("pageNumber", 1),
                quoteText = item.optString("quoteText", ""),
                noteText = item.optString("noteText", ""),
                colorName = item.optString("colorName", "YELLOW"),
                type = item.optString("type", "BOOKMARK"),
                createdAtText = item.optString("createdAtText", "")
            )
        }
        if (annotations.isNotEmpty()) dao.insertAnnotations(annotations)

        val states = parseArray(root, "readerStates") { item ->
            ReaderStateEntity(
                bookId = item.optString("bookId"),
                page = item.optInt("page", 1),
                offsetFraction = item.optDouble("offsetFraction", 0.0).toFloat(),
                scale = item.optDouble("scale", 1.0).toFloat(),
                panX = item.optDouble("panX", 0.0).toFloat(),
                panY = item.optDouble("panY", 0.0).toFloat(),
                layoutMode = item.optStringOrNull("layoutMode"),
                fitMode = item.optStringOrNull("fitMode")
            )
        }
        if (states.isNotEmpty()) dao.restoreReaderStates(states)

        val chapters = parseArray(root, "chapters") { item ->
            ChapterEntity(
                id = item.optString("id"), bookId = item.optString("bookId"),
                chapterNumber = item.optInt("chapterNumber", 1),
                title = item.optString("title", ""),
                startPage = item.optInt("startPage", 1), endPage = item.optInt("endPage", 1),
                isCompleted = item.optBoolean("isCompleted", false),
                isCurrent = item.optBoolean("isCurrent", false)
            )
        }
        if (chapters.isNotEmpty()) dao.insertChapters(chapters)

        val repositories = parseArray(root, "repositories") { item ->
            RepositoryEntity(
                id = item.optString("id"), name = item.optString("name"),
                pathUri = item.optString("pathUri", ""),
                storageType = item.optString("storageType", "SMARTPHONE"),
                bookCount = 0, sizeText = "", lastScanText = "需重新授权",
                scanRule = item.optString("scanRule", "每个文件作为作品"),
                isHealthy = false
            )
        }
        if (repositories.isNotEmpty()) dao.insertRepositories(repositories)

        ImportResult(restored, updated, annotations.size, shelves.size)
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else optString(key)

    private inline fun <T> parseArray(
        root: JSONObject,
        key: String,
        mapper: (JSONObject) -> T
    ): List<T> {
        val array = root.optJSONArray(key) ?: return emptyList()
        val result = ArrayList<T>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            runCatching { result.add(mapper(item)) }
        }
        return result
    }
}
