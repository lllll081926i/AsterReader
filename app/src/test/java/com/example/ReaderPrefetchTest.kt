package com.example

import com.example.ui.components.readerPrefetchPages
import org.junit.Assert.*
import org.junit.Test

class ReaderPrefetchTest {
    @Test fun allowsSixteenPagesAboveAndBelowAndPrioritizesScrollDirection() {
        val down = readerPrefetchPages(20, 50, 1, 16, 16)
        assertEquals((21..36).toList() + (19 downTo 4).toList(), down)
        val up = readerPrefetchPages(20, 50, -1, 16, 16)
        assertEquals((19 downTo 4).toList() + (21..36).toList(), up)
    }

    @Test fun disablingAndBoundaryClampingNeverRequestInvalidPages() {
        assertTrue(readerPrefetchPages(1, 10, 1, 0, 0).isEmpty())
        assertEquals((2..10).toList(), readerPrefetchPages(1, 10, 1, 16, 16))
        assertEquals(listOf(9, 8, 7), readerPrefetchPages(10, 10, -1, 3, 6))
    }
}
