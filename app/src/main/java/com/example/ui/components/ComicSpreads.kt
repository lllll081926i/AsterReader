package com.example.ui.components

/** 页号从 1 开始，分页索引从 0 开始。 */
class ComicSpreads(val totalPages: Int, val doublePage: Boolean, val coverSingle: Boolean) {
    init { require(totalPages > 0) }
    val count: Int = when {
        !doublePage -> totalPages
        coverSingle -> 1 + totalPages / 2
        else -> (totalPages + 1) / 2
    }
    fun indexOf(page: Int): Int {
        val safe = page.coerceIn(1, totalPages)
        return when {
            !doublePage -> safe - 1
            coverSingle && safe == 1 -> 0
            coverSingle -> 1 + (safe - 2) / 2
            else -> (safe - 1) / 2
        }
    }
    fun firstPage(index: Int): Int = when {
        !doublePage -> index + 1
        coverSingle && index == 0 -> 1
        coverSingle -> 2 + (index - 1) * 2
        else -> index * 2 + 1
    }
    fun pages(index: Int): IntRange {
        val first = firstPage(index)
        val size = if (!doublePage || (coverSingle && index == 0)) 1 else 2
        return first..minOf(totalPages, first + size - 1)
    }
}
