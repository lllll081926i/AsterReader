package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AsterViewModel
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ReaderPaperTheme
import com.example.ui.ShelvesViewMode
import com.example.ui.SortOption
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.MainNavDestination
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AsterViewModelFeatureTest {

    private lateinit var viewModel: AsterViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        viewModel = AsterViewModel(app)
    }

    @Test
    fun testInitialSeedDataAndLibraryState() = runBlocking {
        val uiState = viewModel.uiState.value
        assertEquals(MainNavDestination.LIBRARY, uiState.currentTab)
        assertTrue(uiState.useDynamicColor) // 确保默认开启原生 Monet 动态取色
        assertTrue(uiState.subScreenStack.isEmpty())
        assertNull(uiState.activeSubScreen)
    }

    @Test
    fun testNavigationDestinationSwitching() {
        viewModel.selectMainTab(MainNavDestination.SHELVES)
        assertEquals(MainNavDestination.SHELVES, viewModel.uiState.value.currentTab)

        viewModel.selectMainTab(MainNavDestination.SEARCH)
        assertEquals(MainNavDestination.SEARCH, viewModel.uiState.value.currentTab)

        viewModel.selectMainTab(MainNavDestination.LIBRARY)
        assertEquals(MainNavDestination.LIBRARY, viewModel.uiState.value.currentTab)
    }

    @Test
    fun testSubScreenBackStackNavigation() {
        viewModel.pushSubScreen(SubScreen.Settings)
        assertEquals(1, viewModel.uiState.value.subScreenStack.size)
        assertEquals(SubScreen.Settings, viewModel.uiState.value.activeSubScreen)

        viewModel.pushSubScreen(SubScreen.PublicationDetail("frieren_11"))
        assertEquals(2, viewModel.uiState.value.subScreenStack.size)
        assertTrue(viewModel.uiState.value.activeSubScreen is SubScreen.PublicationDetail)

        viewModel.popSubScreen()
        assertEquals(1, viewModel.uiState.value.subScreenStack.size)
        assertEquals(SubScreen.Settings, viewModel.uiState.value.activeSubScreen)

        viewModel.popSubScreen()
        assertTrue(viewModel.uiState.value.subScreenStack.isEmpty())
        assertNull(viewModel.uiState.value.activeSubScreen)
    }

    @Test
    fun testLibrarySortAndLayoutModes() {
        viewModel.setSortOption(SortOption.TITLE_AZ)
        assertEquals(SortOption.TITLE_AZ, viewModel.uiState.value.sortOption)

        viewModel.setSortOption(SortOption.PROGRESS)
        assertEquals(SortOption.PROGRESS, viewModel.uiState.value.sortOption)

        viewModel.setLibraryLayoutMode(LibraryLayoutMode.LIST)
        assertEquals(LibraryLayoutMode.LIST, viewModel.uiState.value.libraryLayoutMode)

        viewModel.setShelvesViewMode(ShelvesViewMode.COMPACT_GRID)
        assertEquals(ShelvesViewMode.COMPACT_GRID, viewModel.uiState.value.shelvesViewMode)
    }

    @Test
    fun testSearchAndFilterLogic() {
        viewModel.updateSearchQuery("芙莉莲")
        assertEquals("芙莉莲", viewModel.uiState.value.searchQuery)

        viewModel.setSearchStatusFilter("在读")
        assertEquals("在读", viewModel.uiState.value.searchStatusFilter)

        viewModel.toggleSearchFormatFilter("CBZ")
        assertTrue(viewModel.uiState.value.searchFormatFilters.contains("CBZ"))
        assertTrue(viewModel.uiState.value.activeFilterCount >= 2)

        viewModel.resetAllSearchFilters()
        assertEquals("全部", viewModel.uiState.value.searchStatusFilter)
        assertTrue(viewModel.uiState.value.searchFormatFilters.isEmpty())
        assertEquals(0, viewModel.uiState.value.activeFilterCount)
    }

    @Test
    fun testBatchSelectionMode() {
        viewModel.enterBatchSelection("frieren_11")
        assertTrue(viewModel.uiState.value.isBatchSelectionMode)
        assertTrue(viewModel.uiState.value.selectedBookIds.contains("frieren_11"))

        viewModel.toggleBookBatchSelection("dungeon_meshi_12")
        assertEquals(2, viewModel.uiState.value.selectedBookIds.size)

        viewModel.toggleBookBatchSelection("frieren_11")
        assertFalse(viewModel.uiState.value.selectedBookIds.contains("frieren_11"))

        viewModel.selectAllVisibleBooks(listOf("book_1", "book_2", "book_3"))
        assertEquals(3, viewModel.uiState.value.selectedBookIds.size)

        viewModel.exitBatchSelection()
        assertFalse(viewModel.uiState.value.isBatchSelectionMode)
        assertTrue(viewModel.uiState.value.selectedBookIds.isEmpty())
    }

    @Test
    fun testReaderControlsAndState() {
        viewModel.openBookReader("frieren_11", initialPage = 10, preferDualPageOnTablet = false)
        assertTrue(viewModel.uiState.value.activeSubScreen is SubScreen.Reader)
        val readerScreen = viewModel.uiState.value.activeSubScreen as SubScreen.Reader
        assertEquals("frieren_11", readerScreen.bookId)
        assertEquals(10, readerScreen.initialPage)

        viewModel.setReaderLayoutMode(ReaderLayoutMode.DUAL_PAGE)
        assertEquals(ReaderLayoutMode.DUAL_PAGE, viewModel.uiState.value.readerLayoutMode)

        viewModel.setReaderFontSize(20)
        assertEquals(20, viewModel.uiState.value.readerFontSizeSp)

        viewModel.setReaderPaperTheme(ReaderPaperTheme.OLED_BLACK)
        assertEquals(ReaderPaperTheme.OLED_BLACK, viewModel.uiState.value.readerPaperTheme)

        viewModel.setFitModeLabel("适合高度")
        assertEquals("适合高度", viewModel.uiState.value.fitModeLabel)
    }

    @Test
    fun testSecurityAndAppLockOperations() {
        // 初始应用锁状态
        assertTrue(viewModel.uiState.value.isAppLockEnabled)
        assertFalse(viewModel.uiState.value.isVaultUnlocked)

        // PIN 码校验
        assertTrue(viewModel.verifyPin("1234"))
        assertFalse(viewModel.verifyPin("9999"))

        // 解锁与锁定流程
        viewModel.unlockAppAndVault()
        assertTrue(viewModel.uiState.value.isVaultUnlocked)
        assertFalse(viewModel.uiState.value.isAppLockedNow)

        viewModel.lockAppImmediately()
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
    }

    @Test
    fun testThemingAndMotionSettings() {
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.setReduceMotion(true)
        assertTrue(viewModel.uiState.value.reduceMotion)

        viewModel.setUseDynamicColor(false)
        assertFalse(viewModel.uiState.value.useDynamicColor)

        viewModel.setShowRecentReading(false)
        assertFalse(viewModel.uiState.value.showRecentReading)
    }
}
