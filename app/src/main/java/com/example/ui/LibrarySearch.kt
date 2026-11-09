package com.example.ui

import com.example.data.local.AnnotationEntity
import com.example.data.local.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class LibrarySearchRequest(
    val query: String,
    val scope: String,
    val sort: SortOption,
    val status: String,
    val formats: Set<String>,
    val tags: Set<String>,
    val repositories: Set<String>,
    val unlocked: Boolean
)

data class LibrarySearchResult(val books: List<BookEntity>, val annotations: List<AnnotationEntity>)

/** 搜索在计算线程执行，批注通过主键映射查找所属漫画。 */
suspend fun searchLibrary(
    books: List<BookEntity>,
    annotations: List<AnnotationEntity>,
    request: LibrarySearchRequest
): LibrarySearchResult = withContext(Dispatchers.Default) {
    val query = request.query.trim()
    fun matchesSource(book: BookEntity): Boolean =
        (request.unlocked || (!book.isVaultProtected && !book.isVaultEncrypted)) &&
            (request.status == "全部" || book.statusBadge == request.status) &&
            (request.formats.isEmpty() || book.format in request.formats) &&
            (request.tags.isEmpty() || book.tagsCsv.split(',').any { it.trim() in request.tags }) &&
            (request.repositories.isEmpty() || book.repositoryName in request.repositories)

    val parents = HashMap<String, BookEntity>(books.size)
    val matching = ArrayList<BookEntity>()
    for ((index, book) in books.withIndex()) {
        if (index % 128 == 0) currentCoroutineContext().ensureActive()
        if (!matchesSource(book)) continue
        parents[book.id] = book
        if (request.scope == "书签与笔记") continue
        if (query.isBlank() || book.title.contains(query, true) || book.author.contains(query, true) ||
            book.series.contains(query, true) || book.tagsCsv.contains(query, true) || book.format.contains(query, true)) {
            matching.add(book)
        }
    }
    val sorted = when (request.sort) {
        SortOption.RECENT_READ -> matching.sortedByDescending { it.lastReadTimestamp }
        SortOption.RECENT_ADDED -> matching.sortedByDescending { it.addedAt }
        SortOption.TITLE_AZ -> matching.sortedBy { it.title }
        SortOption.FILE_SIZE -> matching.sortedByDescending { it.sizeBytes }
        SortOption.PROGRESS -> matching.sortedByDescending { it.progressPercent }
    }
    val notes = if (request.scope in setOf("全部", "书签与笔记")) annotations.filterIndexed { index, note ->
        if (index % 128 == 0) currentCoroutineContext().ensureActive()
        val parent = parents[note.bookId]
        parent != null && (query.isBlank() || note.quoteText.contains(query, true) ||
            note.noteText.contains(query, true) || note.chapterTitle.contains(query, true) || parent.title.contains(query, true))
    } else emptyList()
    LibrarySearchResult(sorted, notes)
}
