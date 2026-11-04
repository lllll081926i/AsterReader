package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnnotationEntity
import com.example.data.local.LibraryRepository
import com.example.data.local.ReaderPreferences
import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.RepositoryEntity
import com.example.data.local.ShelfEntity
import com.example.ui.components.MainNavDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式")
}

enum class ReaderLayoutMode(val label: String, val shortBadge: String) {
    VERTICAL_SCROLL("上下滚动", "滚动"),
    RTL_PAGER("右向左翻页", "右翻"),
    LTR_PAGER("左向右翻页", "左翻"),
    DUAL_PAGE("双页展开", "双页")
}

enum class LibraryLayoutMode(val label: String) {
    GRID_2("双列"),
    GRID_3("三列"),
    LIST("列表"),
    GALLERY("画廊")
}

enum class ShelvesViewMode(val label: String) {
    COLLAGE_GRID("双列"),
    COMPACT_GRID("三列"),
    HORIZONTAL_LIST("列表"),
    BOOKSHELF_ROW("展架")
}


enum class SortOption(val label: String) {
    RECENT_READ("最近阅读"),
    RECENT_ADDED("最近添加"),
    TITLE_AZ("标题 A-Z"),
    FILE_SIZE("文件大小"),
    PROGRESS("阅读进度")
}

sealed interface SubScreen {
    data class Search(val openFilterSheetInitially: Boolean = false, val initialScope: String = "全部") : SubScreen
    data class PublicationDetail(val bookId: String) : SubScreen
    data class Reader(val bookId: String, val initialPage: Int? = null) : SubScreen
    data class ShelfDetail(
        val shelfId: String,
        val title: String,
        val subtitle: String,
        val quickType: String? = null // "ALL", "FAV", "HISTORY", "BOOKMARK"
    ) : SubScreen
    data object PrivacySecuritySettings : SubScreen
    data object Settings : SubScreen
    data object Onboarding : SubScreen
}

data class AsterUiState(
    val currentTab: MainNavDestination = MainNavDestination.LIBRARY,
    val subScreenStack: List<SubScreen> = emptyList(),
    val tabletInspectedBookId: String = "",

    // Theme & Appearance (Monet Dynamic Color is global default as per design.md & ADR-001)
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reduceMotion: Boolean = false,
    val isGridView: Boolean = true,
    val libraryLayoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID_2,
    val shelvesViewMode: ShelvesViewMode = ShelvesViewMode.COLLAGE_GRID,
    val showRecentReading: Boolean = true,

    // Security & Biometric Auth
    val isAppLockEnabled: Boolean = false,
    val isAppLockedNow: Boolean = true,
    val settingsLoaded: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val showQuickAuthSheet: Boolean = false,
    val unlockMethodLabel: String = "系统锁屏凭据",
    val autoLockPolicyLabel: String = "立即锁定",
    val lockOnScreenOff: Boolean = true,
    val secondaryAuthForVault: Boolean = true,
    val flagSecureEnabled: Boolean = true,
    val hideRecentsPreview: Boolean = true,
    val hideMetadataWhenLocked: Boolean = true,
    val recoveryKeyConfigured: Boolean = false,

    val lastIntegrityCheckResult: String = "尚未校验",

    // Library Filter & Search
    val libraryQuickStatusFilter: String = "全部格式", // "全部格式", "未读", "在读", "已完结"
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val sortOption: SortOption = SortOption.RECENT_READ,
    val searchStatusFilter: String = "全部", // "全部", "未读", "在读", "已读完"
    val searchFormatFilters: Set<String> = emptySet(), // empty = all
    val searchTagFilters: Set<String> = emptySet(),
    val searchStorageFilters: Set<String> = emptySet(),

    // Batch Selection
    val isBatchSelectionMode: Boolean = false,
    val selectedBookIds: Set<String> = emptySet(),

    // 漫画阅读设置
    val readerLayoutMode: ReaderLayoutMode = ReaderLayoutMode.VERTICAL_SCROLL,
    val smartSpreadStitch: Boolean = true,
    val volumeKeyPageTurn: Boolean = true,
    val fitModeLabel: String = "适合屏幕",

    // Cache & Diagnostics
    val lastBackupTimeText: String = "尚无备份",
    val isScanning: Boolean = false,
    val requestDirectoryPicker: Boolean = false,
    val pendingRepositoryName: String = "本地目录",
    val pendingScanRule: String = "每个文件作为作品",
    val snackbarMessage: String? = null
) {
    val activeSubScreen: SubScreen?
        get() = subScreenStack.lastOrNull()

    val activeFilterCount: Int
        get() {
            var count = 0
            if (searchStatusFilter != "全部") count++
            if (searchFormatFilters.isNotEmpty()) count++
            if (searchTagFilters.isNotEmpty()) count++
            if (searchStorageFilters.isNotEmpty()) count++
            return count
        }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AsterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private val preferences = ReaderPreferences(application)
    private val annotationMutex = Mutex()
    private val progressMutex = Mutex()
    private val pendingProgress = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, String?>>()
    private var progressSaveJob: kotlinx.coroutines.Job? = null
    private val cleanupScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private var scanJob: kotlinx.coroutines.Job? = null
    private var backgroundAt: Long? = null
    private var backgroundLock: kotlinx.coroutines.Job? = null
    private val operationErrors = CoroutineExceptionHandler { _, _ ->
        showSnackbar("操作失败，请检查来源授权、文件和可用空间后重试")
    }

    private val _uiState = MutableStateFlow(AsterUiState())
    val uiState: StateFlow<AsterUiState> = _uiState.asStateFlow()

    private var pendingPostAuthAction: (() -> Unit)? = null
    private var systemAuthenticationInProgress = false

    val allBooks: StateFlow<List<BookEntity>> = uiState
        .distinctUntilChangedBy { Triple(it.settingsLoaded, it.isAppLockedNow, it.isVaultUnlocked) }
        .flatMapLatest { state ->
            when {
                !state.settingsLoaded || state.isAppLockedNow -> flowOf(emptyList())
                state.isVaultUnlocked -> repository.observeAllBooks()
                else -> repository.observePublicBooks()
            }
        }.sample(150).stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    val allShelves: StateFlow<List<ShelfEntity>> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow to it.isVaultUnlocked }
        .flatMapLatest { state ->
            if (state.isAppLockedNow) flowOf(emptyList())
            else repository.observeAllShelves().map { shelves ->
                shelves.map { shelf -> if (shelf.isProtected && !state.isVaultUnlocked)
                    shelf.copy(name = "受保护书架", subtitle = "访问锁，不加密文件", coverUrlsCsv = "", itemCountDisplay = 0)
                else shelf }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    val allAnnotations: StateFlow<List<AnnotationEntity>> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow to it.isVaultUnlocked }
        .flatMapLatest { state ->
            if (state.isAppLockedNow) flowOf(emptyList())
            else repository.observeVisibleAnnotations(state.isVaultUnlocked)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    val allRepositories: StateFlow<List<RepositoryEntity>> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow }
        .flatMapLatest { state -> if (state.isAppLockedNow) flowOf(emptyList()) else repository.observeAllRepositories() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    init {
        viewModelScope.launch(operationErrors) {
            try { _uiState.value = preferences.load() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.update { it.copy(settingsLoaded = true, isAppLockEnabled = true,
                    isAppLockedNow = true, snackbarMessage = "无法读取设置，请先验证身份") }
                return@launch
            }
            uiState.filter { it.settingsLoaded }.distinctUntilChangedBy { state ->
                listOf(state.isAppLockEnabled, state.themeMode, state.reduceMotion, state.libraryLayoutMode,
                    state.shelvesViewMode, state.showRecentReading, state.readerLayoutMode,
                    state.autoLockPolicyLabel, state.lockOnScreenOff, state.flagSecureEnabled,
                    state.hideRecentsPreview, state.volumeKeyPageTurn, state.smartSpreadStitch, state.fitModeLabel)
            }.debounce(200).collect {
                try { preferences.save(it) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { showSnackbar("设置保存失败，请检查可用空间后重试") }
            }
        }
    }

    suspend fun renderPage(id: String, page: Int) = repository.renderPage(id, page)

    fun observeChapters(bookId: String) = uiState
        .distinctUntilChangedBy { Triple(it.settingsLoaded, it.isAppLockedNow, it.isVaultUnlocked) }
        .flatMapLatest { state ->
            if (!state.settingsLoaded || state.isAppLockedNow) flowOf(emptyList())
            else repository.observeAccessibleBookById(bookId, state.isVaultUnlocked).flatMapLatest { book ->
                if (book == null) flowOf(emptyList()) else repository.observeChaptersForBook(bookId)
            }
        }

    fun observeBookAnnotations(bookId: String) = uiState
        .distinctUntilChangedBy { Triple(it.settingsLoaded, it.isAppLockedNow, it.isVaultUnlocked) }
        .flatMapLatest { state ->
            if (!state.settingsLoaded || state.isAppLockedNow) flowOf(emptyList())
            else repository.observeAccessibleBookById(bookId, state.isVaultUnlocked).flatMapLatest { book ->
                if (book == null) flowOf(emptyList()) else repository.observeAnnotationsForBook(bookId)
            }
        }

    // ========================================================================
    // Navigation & Backstack
    // ========================================================================
    fun selectMainTab(tab: MainNavDestination) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                subScreenStack = emptyList(),
                isBatchSelectionMode = false,
                selectedBookIds = emptySet()
            )
        }
    }

    fun pushSubScreen(screen: SubScreen) {
        _uiState.update { state ->
            state.copy(subScreenStack = state.subScreenStack + screen)
        }
    }

    fun popSubScreen(): Boolean {
        val current = _uiState.value
        if (current.isBatchSelectionMode) {
            exitBatchSelection()
            return true
        }
        if (current.subScreenStack.isNotEmpty()) {
            _uiState.update { it.copy(subScreenStack = it.subScreenStack.dropLast(1)) }
            return true
        }
        if (current.currentTab != MainNavDestination.LIBRARY) {
            _uiState.update { it.copy(currentTab = MainNavDestination.LIBRARY) }
            return true
        }
        return false
    }

    fun inspectBookOnTablet(bookId: String) {
        val book = allBooks.value.find { it.id == bookId }
        if (book != null && book.isVaultProtected && !_uiState.value.isVaultUnlocked) {
            requestVaultUnlock {
                _uiState.update { it.copy(tabletInspectedBookId = bookId) }
            }
        } else {
            _uiState.update { it.copy(tabletInspectedBookId = bookId) }
        }
    }

    fun openBookDetail(bookId: String) {
        val book = allBooks.value.find { it.id == bookId }
        if (book != null && book.isVaultProtected && !_uiState.value.isVaultUnlocked) {
            requestVaultUnlock {
                _uiState.update { it.copy(tabletInspectedBookId = bookId) }
                pushSubScreen(SubScreen.PublicationDetail(bookId))
            }
        } else {
            _uiState.update { it.copy(tabletInspectedBookId = bookId) }
            pushSubScreen(SubScreen.PublicationDetail(bookId))
        }
    }

    fun openBookReader(bookId: String, initialPage: Int? = null, preferDualPageOnTablet: Boolean = false) {
        val book = allBooks.value.find { it.id == bookId }
        val launchReader = {
            if (preferDualPageOnTablet && book?.format == "CBZ") {
                _uiState.update { it.copy(readerLayoutMode = ReaderLayoutMode.DUAL_PAGE) }
            }
            pushSubScreen(SubScreen.Reader(bookId, initialPage))
        }
        if (book != null && book.isVaultProtected && !_uiState.value.isVaultUnlocked) {
            requestVaultUnlock(launchReader)
        } else {
            launchReader()
        }
    }

    fun openSearch(openFilterInitially: Boolean = false, initialQuery: String? = null, initialScope: String = "全部") {
        _uiState.update { state ->
            state.copy(
                searchQuery = initialQuery ?: state.searchQuery,
                subScreenStack = state.subScreenStack + SubScreen.Search(openFilterInitially, initialScope)
            )
        }
    }

    // ========================================================================
    // Security & System Biometric Authentication
    // ========================================================================
    fun lockAppImmediately() {
        releaseReaderResources()
        systemAuthenticationInProgress = false
        pendingPostAuthAction = null
        _uiState.update { it.copy(isAppLockedNow = it.isAppLockEnabled, isVaultUnlocked = false,
            showQuickAuthSheet = false, subScreenStack = emptyList(), tabletInspectedBookId = "",
            selectedBookIds = emptySet(), isBatchSelectionMode = false, searchQuery = "",
            recentSearches = emptyList(), snackbarMessage = null) }
    }

    fun unlockAppAndVault() {
        if (!systemAuthenticationInProgress || !_uiState.value.showQuickAuthSheet) return
        systemAuthenticationInProgress = false
        backgroundAt = null
        val action = pendingPostAuthAction
        pendingPostAuthAction = null
        _uiState.update { it.copy(isAppLockedNow = false, isVaultUnlocked = true, showQuickAuthSheet = false) }
        action?.invoke()
    }

    fun requestVaultUnlock(onUnlocked: (() -> Unit)? = null) {
        if (_uiState.value.isVaultUnlocked && !_uiState.value.isAppLockedNow) { onUnlocked?.invoke(); return }
        pendingPostAuthAction = onUnlocked
        _uiState.update { it.copy(showQuickAuthSheet = true) }
    }

    fun dismissQuickAuthSheet() {
        systemAuthenticationInProgress = false
        pendingPostAuthAction = null
        _uiState.update { it.copy(showQuickAuthSheet = false) }
        onForeground(false)
    }

    fun onSystemAuthenticationStarted() { systemAuthenticationInProgress = true }

    fun onSystemAuthenticationFailed() {
        systemAuthenticationInProgress = false
        onForeground(false)
    }

    fun lockVaultSession() { lockAppImmediately() }

    fun setAppLockEnabled(enabled: Boolean) {
        val manager = getApplication<Application>().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (enabled && !manager.isDeviceSecure) { showSnackbar("请先配置系统安全锁屏"); return }
        pendingPostAuthAction = {
            viewModelScope.launch(operationErrors) {
                val updated = _uiState.value.copy(isAppLockEnabled = enabled)
                preferences.save(updated)
                _uiState.update { it.copy(isAppLockEnabled = enabled) }
            }
        }
        _uiState.update { it.copy(showQuickAuthSheet = true) }
    }

    fun onBackground() {
        if (scanJob?.isActive == true) cancelScanning(false)
        releaseReaderResources()
        backgroundAt = SystemClock.elapsedRealtime()
        backgroundLock?.cancel()
        if (systemAuthenticationInProgress) return
        val timeout = lockTimeout()
        if (timeout == 0L) lockAppImmediately()
        else if (timeout != Long.MAX_VALUE) backgroundLock = viewModelScope.launch {
            kotlinx.coroutines.delay(timeout)
            lockAppImmediately()
        }
    }

    private fun lockTimeout(): Long = when (_uiState.value.autoLockPolicyLabel) {
        "30 秒后" -> 30_000L
        "1 分钟后" -> 60_000L
        "5 分钟后" -> 300_000L
        "仅息屏锁定" -> Long.MAX_VALUE
        else -> 0L
    }

    fun onForeground(deviceLocked: Boolean) {
        if (systemAuthenticationInProgress) return
        backgroundLock?.cancel()
        val state = _uiState.value
        val timeout = lockTimeout()
        val expired = backgroundAt?.let { SystemClock.elapsedRealtime() - it >= timeout } ?: false
        backgroundAt = null
        if ((state.lockOnScreenOff && deviceLocked) || expired) lockAppImmediately()
    }

    fun onScreenOff() { if (_uiState.value.lockOnScreenOff) lockAppImmediately() }

    fun setUnlockMethodLabel(label: String) {
        _uiState.update { it.copy(unlockMethodLabel = label) }
    }

    fun setAutoLockPolicyLabel(label: String) {
        _uiState.update { it.copy(autoLockPolicyLabel = label) }
    }

    fun setLockOnScreenOff(enabled: Boolean) {
        _uiState.update { it.copy(lockOnScreenOff = enabled) }
    }

    fun setSecondaryAuthForVault(enabled: Boolean) {
        _uiState.update { it.copy(secondaryAuthForVault = enabled) }
    }

    fun setFlagSecureEnabled(enabled: Boolean) {
        _uiState.update { it.copy(flagSecureEnabled = enabled) }
    }

    fun setHideRecentsPreview(enabled: Boolean) {
        _uiState.update { it.copy(hideRecentsPreview = enabled) }
    }

    fun setHideMetadataWhenLocked(enabled: Boolean) {
        _uiState.update { it.copy(hideMetadataWhenLocked = enabled) }
    }

    fun runVaultIntegrityCheck() {
        _uiState.update { it.copy(lastIntegrityCheckResult = "未执行：当前版本没有可验证的保险库实现") }
        showSnackbar("当前版本尚不支持保险库校验，未验证任何密文")
    }

    // ========================================================================
    // Book & Library Operations
    // ========================================================================
    fun setShowRecentReading(enabled: Boolean) {
        _uiState.update {
            it.copy(
                showRecentReading = enabled,
                snackbarMessage = if (enabled) "已开启最近阅读展示" else "已隐藏最近阅读模块"
            )
        }
    }

    fun removeSearchHistoryItem(item: String) {
        _uiState.update { state ->
            state.copy(recentSearches = state.recentSearches.filter { it != item })
        }
    }

    fun setLibraryQuickStatusFilter(filter: String) {
        _uiState.update { it.copy(libraryQuickStatusFilter = filter) }
    }

    fun toggleGridView() {
        cycleLibraryLayoutMode()
    }

    fun setLibraryLayoutMode(mode: LibraryLayoutMode) {
        _uiState.update {
            it.copy(
                libraryLayoutMode = mode,
                isGridView = mode != LibraryLayoutMode.LIST
            )
        }
    }

    fun cycleLibraryLayoutMode() {
        _uiState.update { state ->
            val modes = LibraryLayoutMode.entries
            val next = modes[(modes.indexOf(state.libraryLayoutMode) + 1) % modes.size]
            state.copy(
                libraryLayoutMode = next,
                isGridView = next != LibraryLayoutMode.LIST
            )
        }
    }

    fun setShelvesViewMode(mode: ShelvesViewMode) {
        _uiState.update { it.copy(shelvesViewMode = mode) }
    }

    fun cycleShelvesViewMode() {
        _uiState.update { state ->
            val modes = ShelvesViewMode.entries
            val next = modes[(modes.indexOf(state.shelvesViewMode) + 1) % modes.size]
            state.copy(shelvesViewMode = next)
        }
    }

    fun toggleFavorite(bookId: String) {
        viewModelScope.launch(operationErrors) {
            val book = repository.getBookById(bookId) ?: return@launch
            repository.toggleFavorite(bookId)
            val updated = book.copy(isFavorite = !book.isFavorite)
            showSnackbar(if (updated.isFavorite) "已加入收藏《${book.title}》" else "已取消收藏《${book.title}》")
        }
    }

    fun updateReadingProgress(bookId: String, newPage: Int, chapterTitle: String? = null) {
        pendingProgress[bookId] = newPage to chapterTitle
        if (progressSaveJob?.isActive == true) return
        progressSaveJob = viewModelScope.launch(operationErrors) {
            while (pendingProgress.isNotEmpty()) {
                kotlinx.coroutines.delay(1000)
                flushReadingProgress()
            }
        }
    }

    private suspend fun flushReadingProgress() = progressMutex.withLock {
        val snapshot = pendingProgress.entries.map { it.key to it.value }
        for ((id, requested) in snapshot) {
            val book = repository.getBookById(id)
            if (book != null) {
                val page = requested.first.coerceIn(1, book.totalPages.coerceAtLeast(1))
                val percent = (page.toLong() * 100 / book.totalPages.coerceAtLeast(1)).toInt().coerceIn(0, 100)
                if (book.currentPage != page || book.lastReadTimestamp == 0L ||
                    (requested.second != null && book.currentChapterTitle != requested.second)) {
                    repository.updateProgress(id, page, percent, if (percent >= 100) "已读完" else "在读",
                        requested.second, System.currentTimeMillis())
                }
            }
            pendingProgress.remove(id, requested)
        }
    }

    fun releaseReaderResources() {
        repository.trimPageMemory()
        cleanupScope.launch {
            try { flushReadingProgress() }
            catch (_: Exception) { showSnackbar("阅读进度保存失败，请检查可用空间") }
            finally { runCatching { repository.closeReadSession() } }
        }
    }

    override fun onCleared() {
        cleanupScope.launch {
            try { flushReadingProgress() }
            catch (_: Exception) { /* 关闭时保留已有进度，不把失败写成成功。 */ }
            finally {
                try { runCatching { repository.closeReadSession() } }
                finally { cleanupScope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
            }
        }
        super.onCleared()
    }

    fun updateBookMetadata(
        bookId: String,
        newTitle: String,
        newAuthor: String,
        newSeries: String,
        newSynopsis: String,
        newTagsCsv: String
    ) {
        viewModelScope.launch(operationErrors) {
            val book = repository.getBookById(bookId) ?: return@launch
            repository.updateBook(
                book.copy(
                    title = newTitle.trim().ifEmpty { book.title },
                    author = newAuthor.trim().ifEmpty { book.author },
                    series = newSeries.trim(),
                    synopsis = newSynopsis.trim(),
                    tagsCsv = newTagsCsv.trim()
                )
            )
            showSnackbar("已保存《$newTitle》元数据覆写")
        }
    }

    fun toggleBookVaultProtection(bookId: String, encryptToVault: Boolean) {
        showSnackbar("当前版本尚不支持安全的加密迁移或解密导出，原文件与索引未更改")
    }

    // ========================================================================
    // Batch Selection (L-05)
    // ========================================================================
    fun toggleBookBatchSelection(bookId: String) {
        _uiState.update { state ->
            val nextSet = if (bookId in state.selectedBookIds) {
                state.selectedBookIds - bookId
            } else {
                state.selectedBookIds + bookId
            }
            state.copy(
                isBatchSelectionMode = nextSet.isNotEmpty(),
                selectedBookIds = nextSet
            )
        }
    }

    fun enterBatchSelection(initialBookId: String? = null) {
        _uiState.update {
            it.copy(
                isBatchSelectionMode = true,
                selectedBookIds = if (initialBookId != null) setOf(initialBookId) else emptySet()
            )
        }
    }

    fun selectAllVisibleBooks(bookIds: List<String>) {
        _uiState.update {
            it.copy(
                isBatchSelectionMode = true,
                selectedBookIds = bookIds.toSet()
            )
        }
    }

    fun exitBatchSelection() {
        _uiState.update {
            it.copy(isBatchSelectionMode = false, selectedBookIds = emptySet())
        }
    }

    fun batchAddSelectedToShelf(shelfId: String, shelfName: String) {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(operationErrors) {
            ids.forEach { id ->
                val book = repository.getBookById(id) ?: return@forEach
                val currentShelves = book.shelfIdsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
                currentShelves.add(shelfId)
                repository.updateBook(book.copy(shelfIdsCsv = currentShelves.joinToString(",")))
            }
            exitBatchSelection()
            showSnackbar("已将 ${ids.size} 部作品加入书架「$shelfName」")
        }
    }

    fun batchEncryptSelectedToVault() {
        showSnackbar("当前版本尚不支持安全的批量加密，未修改任何文件")
    }

    fun batchRemoveSelectedFromLibrary() {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(operationErrors) {
            repository.removeBooks(ids)
            exitBatchSelection()
            showSnackbar("已从资料库移除 ${ids.size} 部作品索引（保留外部源文件）")
        }
    }

    // ========================================================================
    // Search & Multi-Dimensional Filters (L-03, L-04)
    // ========================================================================
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun commitSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            val updated = (listOf(trimmed) + state.recentSearches.filter { it != trimmed }).take(8)
            state.copy(recentSearches = updated)
        }
    }

    fun clearSearchHistory() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setSearchStatusFilter(status: String) {
        _uiState.update { it.copy(searchStatusFilter = status) }
    }

    fun toggleSearchFormatFilter(format: String) {
        _uiState.update { state ->
            val next = if (format in state.searchFormatFilters) {
                state.searchFormatFilters - format
            } else {
                state.searchFormatFilters + format
            }
            state.copy(searchFormatFilters = next)
        }
    }

    fun toggleSearchTagFilter(tag: String) {
        _uiState.update { state ->
            val next = if (tag in state.searchTagFilters) {
                state.searchTagFilters - tag
            } else {
                state.searchTagFilters + tag
            }
            state.copy(searchTagFilters = next)
        }
    }

    fun toggleSearchStorageFilter(storage: String) {
        _uiState.update { state ->
            val next = if (storage in state.searchStorageFilters) {
                state.searchStorageFilters - storage
            } else {
                state.searchStorageFilters + storage
            }
            state.copy(searchStorageFilters = next)
        }
    }

    fun resetAllSearchFilters() {
        _uiState.update {
            it.copy(
                sortOption = SortOption.RECENT_READ,
                searchStatusFilter = "全部",
                searchFormatFilters = emptySet(),
                searchTagFilters = emptySet(),
                searchStorageFilters = emptySet()
            )
        }
    }

    // ========================================================================
    // Shelves (S-01 ~ S-04)
    // ========================================================================
    fun createCustomShelf(name: String, subtitle: String, isProtected: Boolean) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(operationErrors) {
            val id = "shelf_${System.currentTimeMillis()}"
            val newShelf = ShelfEntity(
                id = id,
                name = cleanName,
                subtitle = subtitle.trim().ifBlank { "0 本 · 刚刚创建" },
                itemCountDisplay = 0,
                updatedLabel = "刚刚创建",
                coverUrlsCsv = "",
                isProtected = isProtected,
                sortOrder = allShelves.value.size
            )
            repository.insertShelf(newShelf)
            showSnackbar("已新建书架「$cleanName」")
        }
    }

    fun deleteCustomShelf(shelfId: String) {
        viewModelScope.launch(operationErrors) {
            repository.deleteShelfById(shelfId)
            showSnackbar("已删除书架分类（书籍本体保留在资料库）")
        }
    }

    fun moveShelfUp(shelf: ShelfEntity) {
        viewModelScope.launch(operationErrors) {
            val currentList = allShelves.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == shelf.id }
            if (index > 0) {
                val prev = currentList[index - 1]
                repository.updateShelf(shelf.copy(sortOrder = prev.sortOrder))
                repository.updateShelf(prev.copy(sortOrder = shelf.sortOrder))
            }
        }
    }

    // ========================================================================
    // Annotations: Bookmarks, Highlights, Notes (P-05, R-07)
    // ========================================================================
    fun addAnnotation(
        bookId: String,
        chapterTitle: String,
        pageNumber: Int,
        quoteText: String,
        noteText: String,
        colorName: String,
        type: String
    ) {
        viewModelScope.launch(operationErrors) {
            repository.insertAnnotation(
                AnnotationEntity(
                    bookId = bookId,
                    chapterTitle = chapterTitle,
                    pageNumber = pageNumber,
                    quoteText = quoteText,
                    noteText = noteText,
                    colorName = colorName,
                    type = type,
                    createdAtText = "刚刚"
                )
            )
            val label = when (type) {
                "BOOKMARK" -> "书签"
                "HIGHLIGHT" -> "高亮标注"
                else -> "阅读笔记"
            }
            showSnackbar("已保存$label (第 $pageNumber 页)")
        }
    }

    fun deleteAnnotation(id: Int) {
        viewModelScope.launch(operationErrors) {
            repository.deleteAnnotationById(id)
            showSnackbar("已移除该条标记")
        }
    }

    // ========================================================================
    // Content Import & Repositories (I-01 ~ I-06)
    // ========================================================================
    fun importLocalFileUri(uri: Uri?, fallbackName: String = "新导入作品") {
        if (uri == null) return
        viewModelScope.launch(operationErrors) {
            repository.importDocument(uri)
            showSnackbar("文件已解析并加入资料库")
        }
    }

    fun addRepositoryAndScan(repoName: String, scanRule: String) {
        _uiState.update { it.copy(pendingRepositoryName = repoName, pendingScanRule = scanRule, requestDirectoryPicker = true) }
    }

    private fun startScan(operation: suspend () -> Unit) {
        if (_uiState.value.isScanning) { showSnackbar("正在扫描，可取消当前扫描后重试"); return }
        _uiState.update { it.copy(isScanning = true) }
        scanJob = viewModelScope.launch(operationErrors) {
            try { operation() }
            finally { _uiState.update { it.copy(isScanning = false) } }
        }
    }

    fun cancelScanning(notify: Boolean = true) {
        scanJob?.cancel()
        if (notify) showSnackbar("已取消扫描，已导入的漫画保留")
    }

    fun onDirectorySelected(uri: Uri?) {
        val state = _uiState.value
        _uiState.update { it.copy(requestDirectoryPicker = false) }
        if (uri == null) return
        startScan {
            val (success, failed) = repository.scanRepository(uri, state.pendingRepositoryName, state.pendingScanRule)
            showSnackbar("扫描完成：已识别 $success 项，失败 $failed 项")
        }
    }

    fun rescanRepositories() {
        if (_uiState.value.isScanning) { cancelScanning(); return }
        startScan {
            val repos = repository.observeAllRepositories().first()
            if (repos.isEmpty()) { showSnackbar("尚无仓库，请先选择目录"); return@startScan }
            var success = 0
            var failed = 0
            repos.forEach { repo ->
                try {
                    val result = repository.scanRepository(Uri.parse(repo.pathUri), repo.name, repo.scanRule)
                    success += result.first; failed += result.second
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failed++ }
            }
            showSnackbar("扫描完成：已识别 $success 项，失败 $failed 项")
        }
    }

    // ========================================================================
    // 漫画阅读与主题设置（R-04、T-02、T-04）
    // ========================================================================
    fun cycleReaderLayoutMode() {
        _uiState.update { state ->
            val modes = ReaderLayoutMode.entries
            val nextIdx = (modes.indexOf(state.readerLayoutMode) + 1) % modes.size
            state.copy(readerLayoutMode = modes[nextIdx])
        }
    }

    fun setReaderLayoutMode(mode: ReaderLayoutMode) {
        _uiState.update { it.copy(readerLayoutMode = mode) }
    }


    fun setSmartSpreadStitch(enabled: Boolean) {
        _uiState.update { it.copy(smartSpreadStitch = enabled) }
    }

    fun setVolumeKeyPageTurn(enabled: Boolean) {
        _uiState.update { it.copy(volumeKeyPageTurn = enabled) }
    }


    fun setFitModeLabel(label: String) {
        _uiState.update { it.copy(fitModeLabel = label) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        _uiState.update { it.copy(useDynamicColor = true) }
        if (!enabled) showSnackbar("全局主题使用系统动态取色，备用色板仅用于异常兜底")
    }

    fun setReduceMotion(enabled: Boolean) {
        _uiState.update { it.copy(reduceMotion = enabled) }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearRegenerableCache() {
        viewModelScope.launch(operationErrors) {
            repository.trimPageMemory()
            coil.Coil.imageLoader(getApplication()).memoryCache?.clear()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                coil.Coil.imageLoader(getApplication()).diskCache?.clear()
            }
            showSnackbar("已清理图片缓存，未修改原文件")
        }
    }

    fun toggleBookmark(bookId: String, page: Int, chapter: String) {
        viewModelScope.launch(operationErrors) {
            annotationMutex.withLock {
                val marks = repository.observeAnnotationsForBook(bookId).first()
                if (marks.any { it.type == "BOOKMARK" && it.pageNumber == page }) repository.removeBookmark(bookId, page)
                else repository.insertAnnotation(AnnotationEntity(bookId = bookId, chapterTitle = chapter,
                    pageNumber = page, quoteText = "", noteText = "", colorName = "GREEN", type = "BOOKMARK", createdAtText = "刚刚"))
            }
        }
    }

    fun createLocalBackup(includeVault: Boolean) {
        showSnackbar("当前版本尚不支持可恢复的加密备份，未生成备份文件")
    }

    fun showSnackbar(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
