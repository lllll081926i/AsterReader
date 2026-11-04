package com.example

import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ShelvesViewMode
import com.example.ui.SortOption
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsterUiModelTest {

    @Test
    fun themeModesExposeExpectedLabels() {
        assertEquals("跟随系统", ThemeMode.SYSTEM.label)
        assertEquals("浅色模式", ThemeMode.LIGHT.label)
        assertEquals("深色模式", ThemeMode.DARK.label)
    }

    @Test
    fun readerLayoutModesExposeExpectedShortBadges() {
        assertEquals("滚动", ReaderLayoutMode.VERTICAL_SCROLL.shortBadge)
        assertEquals("右翻", ReaderLayoutMode.RTL_PAGER.shortBadge)
        assertEquals("左翻", ReaderLayoutMode.LTR_PAGER.shortBadge)
        assertEquals("双页", ReaderLayoutMode.DUAL_PAGE.shortBadge)
    }

    @Test
    fun sortOptionCoversAllDimensions() {
        val options = SortOption.entries
        assertEquals(5, options.size)
        assertTrue(options.contains(SortOption.RECENT_READ))
        assertTrue(options.contains(SortOption.RECENT_ADDED))
        assertTrue(options.contains(SortOption.TITLE_AZ))
        assertTrue(options.contains(SortOption.FILE_SIZE))
        assertTrue(options.contains(SortOption.PROGRESS))
    }

    @Test
    fun subScreenHierarchyIsWellFormed() {
        val searchScreen: SubScreen = SubScreen.Search(openFilterSheetInitially = true)
        val detailScreen: SubScreen = SubScreen.PublicationDetail("frieren_11")
        val readerScreen: SubScreen = SubScreen.Reader("frieren_11", initialPage = 5)
        val shelfScreen: SubScreen = SubScreen.ShelfDetail(
            shelfId = "shelf_fantasy",
            title = "奇幻物语",
            subtitle = "精选",
            quickType = "ALL"
        )

        assertNotNull(searchScreen)
        assertEquals("frieren_11", (detailScreen as SubScreen.PublicationDetail).bookId)
        assertEquals(5, (readerScreen as SubScreen.Reader).initialPage)
        assertEquals("shelf_fantasy", (shelfScreen as SubScreen.ShelfDetail).shelfId)
    }

    @Test
    fun libraryAndShelvesLayoutModesCoverAllDisplayOptions() {
        assertEquals(4, LibraryLayoutMode.entries.size)
        assertEquals(4, ShelvesViewMode.entries.size)
    }
}
