package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AsterViewModel
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ShelvesViewMode
import com.example.ui.SortOption
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.MainNavDestination
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import androidx.lifecycle.ViewModelStore
import org.junit.After
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
    private val store = ViewModelStore()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("reader_settings", 0).edit().clear().commit()
        app.getSharedPreferences("pin_attempts", 0).edit().clear().commit()
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AsterViewModel(app)
        store.put("被测模型", viewModel)
        runBlocking { withTimeout(5000) { viewModel.uiState.first { it.settingsLoaded } } }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test
    fun testInitialEmptyLibraryState() = runBlocking {
        val uiState = viewModel.uiState.value
        assertEquals(MainNavDestination.LIBRARY, uiState.currentTab)
        assertTrue(uiState.subScreenStack.isEmpty())
        assertNull(uiState.activeSubScreen)
    }

    @Test fun shelfSavedCallbackOnlyRunsAfterSuccessfulPersistence() = runBlocking {
        val name = "持久化回调-${java.util.UUID.randomUUID()}"
        val saved = java.util.concurrent.CountDownLatch(1)
        viewModel.createCustomShelf(name, "", false, onSaved = { saved.countDown() })
        assertTrue(saved.await(5, java.util.concurrent.TimeUnit.SECONDS))
        viewModel.consumeSnackbar()
        var duplicateSaved = false
        viewModel.createCustomShelf(name, "", false, onSaved = { duplicateSaved = true })
        withTimeout(5000) { viewModel.snackbar.first { it != null } }
        assertFalse(duplicateSaved)
    }

    @Test fun firstReadyStateAlreadyContainsRestoredNavigation() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        com.example.data.local.ReaderPreferences(app).saveSession(
            tab = MainNavDestination.SHELVES.name, stackEncoding = "", searchQuery = "")
        val restored = AsterViewModel(app)
        store.put("启动恢复", restored)
        val firstReady = withTimeout(5000) { restored.uiState.first { it.settingsLoaded } }
        assertEquals(MainNavDestination.SHELVES, firstReady.currentTab)
    }

    @Test fun initialSettingsLoadKeepsNavigationChosenDuringStartup() = runBlocking {
        val starting = AsterViewModel(ApplicationProvider.getApplicationContext<Application>())
        store.put("启动期间导航", starting)
        starting.pushSubScreen(SubScreen.SettingsSection("about"))
        val ready = withTimeout(5000) { starting.uiState.first { it.settingsLoaded } }
        assertEquals(SubScreen.SettingsSection("about"), ready.activeSubScreen)
    }

    @Test fun disablingAppLockRequiresFreshAuthenticationAndCancellationKeepsProtection() = runBlocking {
        viewModel.setCustomPin("4321")
        withTimeout(5000) { viewModel.uiState.first { it.hasCustomPin } }
        viewModel.setAppLockEnabled(true)
        withTimeout(5000) { viewModel.uiState.first { it.isAppLockEnabled } }
        viewModel.setAppLockEnabled(false)
        assertTrue(viewModel.uiState.value.isAppLockEnabled)
        assertTrue(viewModel.uiState.value.showQuickAuthSheet)
        viewModel.unlockAppAndVault()
        assertTrue(viewModel.uiState.value.isAppLockEnabled)
        viewModel.dismissQuickAuthSheet()
        assertTrue(viewModel.uiState.value.isAppLockEnabled)
        viewModel.setAppLockEnabled(false)
        viewModel.beginUnlockAttempt()
        assertTrue(viewModel.unlockWithPin("4321"))
        withTimeout(5000) { viewModel.uiState.first { !it.isAppLockEnabled } }
        Unit
    }

    @Test fun lockedSessionCannotRunPendingSensitiveSetting() = runBlocking {
        viewModel.setCustomPin("4321")
        withTimeout(5000) { viewModel.uiState.first { it.hasCustomPin } }
        viewModel.setFlagSecureEnabled(false)
        assertTrue(viewModel.uiState.value.showQuickAuthSheet)
        viewModel.beginUnlockAttempt()
        viewModel.lockAppImmediately()
        viewModel.unlockAppAndVault()
        assertTrue(viewModel.uiState.value.flagSecureEnabled)
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
    }

    @Test fun hidingStatsTabReturnsToLibraryWithoutDisablingRecording() {
        viewModel.selectMainTab(MainNavDestination.STATS)
        assertEquals(MainNavDestination.STATS, viewModel.uiState.value.currentTab)
        viewModel.setShowStatsTab(false)
        assertEquals(MainNavDestination.LIBRARY, viewModel.uiState.value.currentTab)
        assertTrue(viewModel.uiState.value.showReadingStats)
        viewModel.selectMainTab(MainNavDestination.STATS)
        assertEquals(MainNavDestination.LIBRARY, viewModel.uiState.value.currentTab)
    }

    @Test fun pinCooldownSurvivesViewModelRecreation() = runBlocking {
        viewModel.setCustomPin("4321")
        withTimeout(5000) { viewModel.uiState.first { it.hasCustomPin } }
        repeat(5) {
            viewModel.beginUnlockAttempt()
            assertFalse(viewModel.unlockWithPin("0000"))
        }
        assertTrue(viewModel.pinLockRemainingSeconds() > 0)
        val recreated = AsterViewModel(ApplicationProvider.getApplicationContext<Application>())
        store.put("重建模型", recreated)
        withTimeout(5000) { recreated.uiState.first { it.settingsLoaded } }
        assertTrue(recreated.pinLockRemainingSeconds() > 0)
        recreated.beginUnlockAttempt()
        assertFalse(recreated.unlockWithPin("4321"))
    }

    @Test fun pinCannotUnlockWithoutAnAuthenticationAttempt() = runBlocking {
        viewModel.setCustomPin("4321")
        withTimeout(5000) { viewModel.uiState.first { it.hasCustomPin } }
        assertFalse(viewModel.unlockWithPin("4321"))
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


        viewModel.setFitModeLabel("原始比例")
        assertEquals("原始比例", viewModel.uiState.value.fitModeLabel)
    }

    @Test
    fun testSecurityAndAppLockOperations() {
        // 初始应用锁状态
        assertFalse(viewModel.uiState.value.isAppLockEnabled)
        assertFalse(viewModel.uiState.value.isVaultUnlocked)

        // 解锁与锁定流程
        viewModel.requestVaultUnlock()
        viewModel.onSystemAuthenticationStarted()
        viewModel.unlockAppAndVault()
        assertTrue(viewModel.uiState.value.isVaultUnlocked)
        assertFalse(viewModel.uiState.value.isAppLockedNow)

        viewModel.lockAppImmediately()
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
    }

    @Test
    fun unsolicitedOrCancelledAuthenticationNeverUnlocksVault() {
        viewModel.unlockAppAndVault()
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
        viewModel.requestVaultUnlock()
        viewModel.onSystemAuthenticationStarted()
        viewModel.onSystemAuthenticationFailed()
        viewModel.unlockAppAndVault()
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
    }

    @Test
    fun screenOffInvalidatesAnOutstandingAuthenticationResult() {
        viewModel.requestVaultUnlock()
        viewModel.onSystemAuthenticationStarted()
        viewModel.onScreenOff()
        viewModel.unlockAppAndVault()
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
    }

    @Test
    fun importedUriIsRequiredAndPinIsHashed() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dao = com.example.data.local.AsterDatabase.getDatabase(app).asterDao()
        val count = dao.getBookCount()
        viewModel.importLocalFileUri(null)
        assertEquals(count, dao.getBookCount())

        assertEquals("", viewModel.uiState.value.pinHash)
        assertFalse(viewModel.verifyPin("1234"))

        viewModel.setCustomPin("4321")
        withTimeout(5000) { viewModel.uiState.first { it.hasCustomPin } }
        assertTrue(viewModel.uiState.value.hasCustomPin)
        assertTrue(viewModel.verifyPin("4321"))
        assertFalse(viewModel.verifyPin("1234"))
        // PIN 以摘要形式保存，绝不明文
        assertFalse(viewModel.uiState.value.pinHash.contains("4321"))

        viewModel.setCustomPin("12")
        assertTrue(viewModel.verifyPin("4321"))
    }

    @Test
    fun testThemingAndMotionSettings() {
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.setReduceMotion(true)
        assertTrue(viewModel.uiState.value.reduceMotion)

        viewModel.setShowRecentReading(false)
        assertFalse(viewModel.uiState.value.showRecentReading)
    }
}
