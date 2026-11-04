package com.example

import com.example.ui.components.ComicSpreads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicSpreadsTest {
    @Test
    fun coverCanRemainSingleWithoutSkippingOddOrEvenFinalPages() {
        for (total in 1..25) {
            for (doublePage in listOf(false, true)) for (coverSingle in listOf(false, true)) {
                val spreads = ComicSpreads(total, doublePage, coverSingle)
                val pages = (0 until spreads.count).flatMap { spreads.pages(it).toList() }
                assertEquals((1..total).toList(), pages)
                for (page in 1..total) assertTrue(page in spreads.pages(spreads.indexOf(page)))
            }
        }
    }

    @Test
    fun narrowScreenFallsBackToSinglePageAndSeekUsesExactPage() {
        val single = ComicSpreads(100, false, true)
        assertEquals(100, single.count)
        assertEquals(76, single.indexOf(77))
        assertEquals(77..77, single.pages(76))
    }
}
