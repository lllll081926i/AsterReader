package com.example

import com.example.data.local.AnnotationEntity
import com.example.ui.LibrarySearchRequest
import com.example.ui.SortOption
import com.example.ui.searchLibrary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LibrarySearchTest {
    private fun request(scope: String = "全部", sort: SortOption = SortOption.RECENT_READ) =
        LibrarySearchRequest("", scope, sort, "全部", emptySet(), emptySet(), emptySet(), false)

    @Test
    fun searchHidesProtectedAndOrphanNotesAndUsesActualNumericSort() = runBlocking {
        val small = comicFixture("小文件").copy(sizeBytes = 2, addedAt = 20)
        val large = comicFixture("大文件").copy(sizeBytes = 100, addedAt = 10)
        val hidden = comicFixture("私密").copy(isVaultEncrypted = true)
        val notes = listOf("小文件", "私密", "孤立").map {
            AnnotationEntity(bookId = it, chapterTitle = "第 1 页", pageNumber = 1,
                quoteText = "", noteText = "漫画笔记", colorName = "GREEN", type = "NOTE", createdAtText = "现在")
        }
        val result = searchLibrary(listOf(small, large, hidden), notes, request(sort = SortOption.FILE_SIZE))
        assertEquals(listOf("大文件", "小文件"), result.books.map { it.id })
        assertEquals(listOf("小文件"), result.annotations.map { it.bookId })
        val bookmarks = searchLibrary(listOf(small, large, hidden), notes, request("书签札记"))
        assertTrue(bookmarks.books.isEmpty())
        assertEquals(1, bookmarks.annotations.size)
    }

    @Test
    fun tenThousandComicsAndNotesUseExactParentLookup() = runBlocking {
        val books = (0 until 10_000).map { comicFixture("漫画$it").copy(title = "漫画 $it", addedAt = it.toLong()) }
        val notes = books.map { AnnotationEntity(bookId = it.id, chapterTitle = "第 1 页", pageNumber = 1,
            quoteText = "", noteText = "笔记", colorName = "GREEN", type = "NOTE", createdAtText = "现在") }
        val result = searchLibrary(books, notes, request(sort = SortOption.RECENT_ADDED))
        assertEquals("漫画9999", result.books.first().id)
        assertEquals(10_000, result.annotations.size)
        val tagged = books.mapIndexed { index, book -> book.copy(tagsCsv = if (index % 2 == 0) " 奇幻 , 漫画 " else "科幻") }
        val filtered = searchLibrary(tagged, notes, request().copy(tags = setOf("冒险", "奇幻")))
        assertEquals(5000, filtered.books.size)
        assertEquals(5000, filtered.annotations.size)
    }
}
