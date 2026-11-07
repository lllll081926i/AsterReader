package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.asterreader.core.database.ReaderStateEntity
import com.asterreader.core.model.ReaderViewport
import com.example.data.local.AnnotationEntity
import com.example.data.local.LibraryBackup
import com.example.data.local.LibraryRepository
import com.example.data.local.PinCredentials
import com.example.data.local.ReaderPreferences
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
import com.example.data.local.ShelfEntity
import com.example.ui.components.MainNavDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SnackbarTone { INFO, SUCCESS, ERROR }

/** 扫描进度：每个文件都会变，不放进 AsterUiState 以免重组整棵 UI。 */
data class ScanProgress(val processed: Int = 0, val discovered: Int = 0)

/** Snackbar 事件：带唯一 id，保证相同文案连续出现时也能重新计时。 */
data class SnackbarEvent(val message: String, val tone: SnackbarTone, val id: Long = System.nanoTime())

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
    data object SeriesList : SubScreen
    data object HiddenBooks : SubScreen
    data object Settings : SubScreen
    data object ReadingStats : SubScreen
    data object Onboarding : SubScreen
}

data class AsterUiState(
    val currentTab: MainNavDestination = MainNavDestination.LIBRARY,
    val subScreenStack: List<SubScreen> = emptyList(),
    val tabletInspectedBookId: String = "",

    // Theme & Appearance (Monet Dynamic Color 为全局默认)
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val reduceMotion: Boolean = false,
    val libraryLayoutMode: LibraryLayoutMode = LibraryLayoutMode.GRID_2,
    val shelvesViewMode: ShelvesViewMode = ShelvesViewMode.COLLAGE_GRID,
    val showRecentReading: Boolean = true,
    val showReadingStats: Boolean = true,

    // Security & Auth
    val isAppLockEnabled: Boolean = false,
    val isAppLockedNow: Boolean = true,
    val pinHash: String = "",
    val biometricUnlockEnabled: Boolean = true,
    val pinLockedUntilMs: Long = 0L,
    val requestPinSetup: Boolean = false,
    val pendingAppLockActivation: Boolean = false,
    val settingsLoaded: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val showQuickAuthSheet: Boolean = false,
    val autoLockPolicyLabel: String = "立即锁定",
    val lockOnScreenOff: Boolean = true,
    val flagSecureEnabled: Boolean = true,
    val hideRecentsPreview: Boolean = true,

    // Library Filter & Search
    val libraryQuickStatusFilter: String = "全部格式",
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val sortOption: SortOption = SortOption.RECENT_READ,
    val searchStatusFilter: String = "全部",
    val searchFormatFilters: Set<String> = emptySet(),
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
    val readerBrightness: Float = 0.95f,

    // 扫描与来源
    val isScanning: Boolean = false,
    val requestDirectoryPicker: Boolean = false,
    val pendingRepositoryName: String = "本地目录",
    val pendingRelocateBookId: String? = null,
    val pendingRelocateIsFolder: Boolean = false
) {
    val activeSubScreen: SubScreen?
        get() = subScreenStack.lastOrNull()

    val hasCustomPin: Boolean
        get() = pinHash.isNotBlank()

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
    // AndroidViewModel.application 是 private，这里保留主构造参数供备份导出/导入使用
    private val appContext: Application = application
    private val repository = LibraryRepository(application)
    private val preferences = ReaderPreferences(application)
    private val annotationMutex = Mutex()
    private val progressMutex = Mutex()

    /** 待落盘的阅读状态：页码与视口分开收集，统一节流写入。 */
    private val pendingProgress = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, String?>>()
    private val pendingViewports = java.util.concurrent.ConcurrentHashMap<String, ReaderViewport>()
    private var persistJob: kotlinx.coroutines.Job? = null
    private val cleanupScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private var scanJob: kotlinx.coroutines.Job? = null
    private var backgroundAt: Long? = null
    private var backgroundLock: kotlinx.coroutines.Job? = null
    private val operationErrors = CoroutineExceptionHandler { _, _ ->
        showSnackbar("操作失败，请检查来源授权、文件和可用空间后重试")
    }

    private val _uiState = MutableStateFlow(AsterUiState())
    val uiState: StateFlow<AsterUiState> = _uiState.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _snackbar = MutableStateFlow<SnackbarEvent?>(null)
    val snackbar: StateFlow<SnackbarEvent?> = _snackbar.asStateFlow()

    private var pendingPostAuthAction: (() -> Unit)? = null
    private var systemAuthenticationInProgress = false
    private var pinFailures = 0
    private var pendingExternalUri: Uri? = null

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
                    shelf.copy(name = "已隐藏书架", subtitle = "验证身份后可见", coverUrlsCsv = "", itemCountDisplay = 0)
                else shelf }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    val allAnnotations: StateFlow<List<AnnotationEntity>> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow to it.isVaultUnlocked }
        .flatMapLatest { state ->
            if (state.isAppLockedNow) flowOf(emptyList())
            else repository.observeVisibleAnnotations(state.isVaultUnlocked)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    val allRepositories: StateFlow<List<com.example.data.local.RepositoryEntity>> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow }
        .flatMapLatest { state -> if (state.isAppLockedNow) flowOf(emptyList()) else repository.observeAllRepositories() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    /** 已隐藏数量：只暴露计数，不暴露内容，未验证也可显示入口。 */
    val hiddenCount: StateFlow<Int> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow }
        .flatMapLatest { state -> if (state.isAppLockedNow) flowOf(0) else repository.observeHiddenCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), 0)

    /** 已隐藏作品：只在通过验证后提供，未解锁时为空，避免绕过入口。 */
    val hiddenBooks: StateFlow<List<BookEntity>> = uiState
        .distinctUntilChangedBy { it.isAppLockedNow to it.isVaultUnlocked }
        .flatMapLatest { state ->
            if (state.isAppLockedNow || !state.isVaultUnlocked) flowOf(emptyList())
            else repository.observeAllBooks().map { books -> books.filter { it.isVaultProtected } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000, 0), emptyList())

    init {
        viewModelScope.launch(operationErrors) {
            try { _uiState.value = preferences.load() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                _uiState.update { it.copy(settingsLoaded = true, isAppLockEnabled = true,
                    isAppLockedNow = true) }
                showSnackbar("无法读取设置，请先验证身份", SnackbarTone.ERROR)
                return@launch
            }
            uiState.filter { it.settingsLoaded }.distinctUntilChangedBy { state ->
                listOf(state.isAppLockEnabled, state.themeMode, state.reduceMotion, state.libraryLayoutMode,
                    state.shelvesViewMode, state.showRecentReading, state.showReadingStats, state.readerLayoutMode,
                    state.autoLockPolicyLabel, state.lockOnScreenOff, state.flagSecureEnabled,
                    state.hideRecentsPreview, state.volumeKeyPageTurn, state.smartSpreadStitch,
                    state.fitModeLabel, state.readerBrightness, state.pinHash,
                    state.biometricUnlockEnabled, state.recentSearches)
            }.debounce(200).collect {
                try { preferences.save(it) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { showSnackbar("设置保存失败，请检查可用空间后重试") }
            }
        }
        viewModelScope.launch(operationErrors) { restoreNavSession() }
    }

    /**
     * 进程重建后回到离开时的位置：恢复主 tab、整个子页面栈与搜索词（30 分钟内有效）。
     * 应用锁开启时不恢复（先验证身份，避免绕过锁直接看到阅读内容）。
     */
    private suspend fun restoreNavSession() {
        uiState.filter { it.settingsLoaded }.first()
        if (_uiState.value.isAppLockEnabled) return
        val session = runCatching { preferences.loadSession() }.getOrNull() ?: return
        if (System.currentTimeMillis() - session.savedAt > NAV_SESSION_WINDOW_MS) return
        val tab = MainNavDestination.entries.firstOrNull { it.name == session.tab } ?: return

        // 兼容旧版只保存「阅读中的书」的会话
        val restored = decodeNavStack(session.stackEncoding).ifEmpty {
            session.legacyBookId?.let { listOf(SubScreen.Reader(it)) }.orEmpty()
        }
        // 丢掉指向已删除作品的子页面，避免恢复出一排「作品不可用」
        val valid = restored.filterNot { screen ->
            val bookId = when (screen) {
                is SubScreen.PublicationDetail -> screen.bookId
                is SubScreen.Reader -> screen.bookId
                else -> null
            }
            bookId != null && runCatching { repository.getBookById(bookId) }.getOrNull() == null
        }
        _uiState.update { state ->
            state.copy(
                currentTab = if (valid.any { screen -> screen is SubScreen.Reader }) MainNavDestination.LIBRARY else tab,
                subScreenStack = valid,
                searchQuery = session.searchQuery
            )
        }
    }

    /** 记录导航快照，供进程重建后恢复。 */
    private fun recordNavSession() {
        val state = _uiState.value
        viewModelScope.launch {
            runCatching {
                preferences.saveSession(
                    tab = state.currentTab.name,
                    stackEncoding = encodeNavStack(state.subScreenStack),
                    searchQuery = state.searchQuery
                )
            }
        }
    }

    /** 子页面栈用 URL-safe Base64 编码参数，避免分隔符与书名/标签冲突。 */
    private fun encodeNavStack(stack: List<SubScreen>): String = stack.joinToString("\n") { screen ->
        when (screen) {
            is SubScreen.Search -> "SEARCH|${if (screen.openFilterSheetInitially) 1 else 0}|${navEnc(screen.initialScope)}"
            is SubScreen.PublicationDetail -> "DETAIL|${navEnc(screen.bookId)}"
            is SubScreen.Reader -> "READER|${navEnc(screen.bookId)}|${screen.initialPage ?: -1}"
            is SubScreen.ShelfDetail -> listOf(
                "SHELF", navEnc(screen.shelfId), navEnc(screen.title),
                navEnc(screen.subtitle), navEnc(screen.quickType.orEmpty())
            ).joinToString("|")
            SubScreen.PrivacySecuritySettings -> "PRIVACY"
            SubScreen.SeriesList -> "SERIES"
            SubScreen.HiddenBooks -> "HIDDEN"
            SubScreen.Settings -> "SETTINGS"
            SubScreen.ReadingStats -> "STATS"
            SubScreen.Onboarding -> "ONBOARDING"
        }
    }

    private fun decodeNavStack(raw: String): List<SubScreen> =
        raw.split('\n').filter { it.isNotBlank() }.mapNotNull { line ->
            runCatching {
                val parts = line.split('|')
                when (parts[0]) {
                    "SEARCH" -> SubScreen.Search(parts.getOrNull(1) == "1", navDec(parts.getOrElse(2) { "" }))
                    "DETAIL" -> SubScreen.PublicationDetail(navDec(parts[1]))
                    "READER" -> SubScreen.Reader(
                        navDec(parts[1]), parts.getOrNull(2)?.toIntOrNull()?.takeIf { it > 0 }
                    )
                    "SHELF" -> SubScreen.ShelfDetail(
                        navDec(parts[1]), navDec(parts[2]), navDec(parts[3]),
                        parts.getOrNull(4)?.takeIf { it.isNotBlank() }?.let(::navDec)
                    )
                    "PRIVACY" -> SubScreen.PrivacySecuritySettings
                    "SERIES" -> SubScreen.SeriesList
                    "HIDDEN" -> SubScreen.HiddenBooks
                    "SETTINGS" -> SubScreen.Settings
                    "STATS" -> SubScreen.ReadingStats
                    "ONBOARDING" -> SubScreen.Onboarding
                    else -> null
                }
            }.getOrNull()
        }

    private fun navEnc(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun navDec(value: String): String =
        String(java.util.Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

    suspend fun renderPage(id: String, page: Int, maxPixels: Int = LibraryRepository.DEFAULT_PAGE_PIXELS) =
        repository.renderPage(id, page, maxPixels)

    suspend fun renderPageRegion(
        id: String,
        page: Int,
        topFraction: Float,
        bottomFraction: Float,
        maxPixels: Int = LibraryRepository.ZOOMED_PAGE_PIXELS
    ) = repository.renderPageRegion(id, page, topFraction, bottomFraction, maxPixels)

    /** 预取相邻页；失败静默，不影响阅读。 */
    fun prefetchPages(id: String, pages: List<Int>) {
        viewModelScope.launch {
            runCatching { repository.prefetchPages(id, pages) }
        }
    }

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

    fun observeReaderState(bookId: String) = repository.observeReaderState(bookId)

    suspend fun loadReaderState(bookId: String): ReaderStateEntity? = repository.readerState(bookId)

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
        recordNavSession()
    }

    fun pushSubScreen(screen: SubScreen) {
        _uiState.update { state -> state.copy(subScreenStack = state.subScreenStack + screen) }
        recordNavSession()
    }

    fun popSubScreen(): Boolean {
        val current = _uiState.value
        if (current.isBatchSelectionMode) {
            exitBatchSelection()
            return true
        }
        if (current.subScreenStack.isNotEmpty()) {
            _uiState.update { it.copy(subScreenStack = it.subScreenStack.dropLast(1)) }
            recordNavSession()
            return true
        }
        if (current.currentTab != MainNavDestination.LIBRARY) {
            _uiState.update { it.copy(currentTab = MainNavDestination.LIBRARY) }
            recordNavSession()
            return true
        }
        return false
    }

    fun inspectBookOnTablet(bookId: String) {
        val book = allBooks.value.find { it.id == bookId }
        if (book != null && book.isVaultProtected && !_uiState.value.isVaultUnlocked) {
            requestVaultUnlock { _uiState.update { it.copy(tabletInspectedBookId = bookId) } }
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
            if (preferDualPageOnTablet && book != null && book.totalPages > 1) {
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

    /**
     * L1 隐藏：只改变可见性，文件保持原样。隐藏后立即关闭该作品相关的详情/阅读页，
     * 避免「隐藏了但还停在书里」。
     */
    fun setBookHidden(bookId: String, hidden: Boolean) {
        viewModelScope.launch(operationErrors) {
            runCatching { repository.setBookHidden(bookId, hidden) }
                .onFailure { showSnackbar(it.message ?: "操作失败"); return@launch }
            if (hidden) {
                _uiState.update { state ->
                    state.copy(
                        subScreenStack = state.subScreenStack.filterNot { screen ->
                            (screen is SubScreen.PublicationDetail && screen.bookId == bookId) ||
                                (screen is SubScreen.Reader && screen.bookId == bookId)
                        }
                    )
                }
            }
            recordNavSession()
            showSnackbar(if (hidden) "已隐藏" else "已取消隐藏")
        }
    }

    fun openHiddenBooks() {
        requestVaultUnlock { pushSubScreen(SubScreen.HiddenBooks) }
    }

    fun openSearch(openFilterInitially: Boolean = false, initialQuery: String? = null, initialScope: String = "全部") {        _uiState.update { state ->
            state.copy(
                searchQuery = initialQuery ?: state.searchQuery,
                subScreenStack = state.subScreenStack + SubScreen.Search(openFilterInitially, initialScope)
            )
        }
    }

    // ========================================================================
    // Security & System Authentication
    // ========================================================================
    fun lockAppImmediately() {
        releaseReaderResources()
        systemAuthenticationInProgress = false
        pendingPostAuthAction = null
        _uiState.update { it.copy(isAppLockedNow = it.isAppLockEnabled, isVaultUnlocked = false,
            showQuickAuthSheet = false, subScreenStack = emptyList(), tabletInspectedBookId = "",
            selectedBookIds = emptySet(), isBatchSelectionMode = false, searchQuery = "") }
        _snackbar.value = null
    }

    fun verifyPin(input: String): Boolean {
        val hash = _uiState.value.pinHash
        return if (hash.isBlank()) input.trim() == DEFAULT_PIN else PinCredentials.verify(input, hash)
    }

    fun setCustomPin(pin: String) {
        val sanitized = pin.trim()
        if (sanitized.isEmpty() && _uiState.value.isAppLockEnabled) {
            showSnackbar("请先关闭应用锁，再清除 PIN 码", SnackbarTone.ERROR)
            return
        }
        if (sanitized.isNotEmpty() && (sanitized.length !in 4..8 || !sanitized.all(Char::isDigit))) {
            showSnackbar("PIN 码需为 4–8 位数字", SnackbarTone.ERROR)
            return
        }
        val activate = _uiState.value.pendingAppLockActivation && sanitized.isNotEmpty()
        _uiState.update {
            it.copy(
                pinHash = if (sanitized.isEmpty()) "" else PinCredentials.hash(sanitized),
                requestPinSetup = false,
                pendingAppLockActivation = false,
                isAppLockEnabled = if (activate) true else it.isAppLockEnabled
            )
        }
        pinFailures = 0
        _uiState.update { it.copy(pinLockedUntilMs = 0L) }
        if (activate) {
            viewModelScope.launch { preferences.save(_uiState.value) }
            showSnackbar("应用 PIN 已设置，应用锁已开启", SnackbarTone.SUCCESS)
        } else {
            showSnackbar(
                if (sanitized.isEmpty()) "已清除自定义 PIN，将使用默认 $DEFAULT_PIN" else "应用 PIN 已更新",
                SnackbarTone.SUCCESS
            )
        }
    }

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(biometricUnlockEnabled = enabled) }
    }

    fun consumePinSetupRequest() {
        _uiState.update { it.copy(requestPinSetup = false) }
    }

    /** 用户放弃设置 PIN：撤销「待开启应用锁」，避免出现默认 1234 的弱锁。 */
    fun cancelPendingAppLockActivation() {
        _uiState.update { it.copy(pendingAppLockActivation = false) }
    }

    /** 开始一次解锁尝试：PIN 与系统生物识别都走这个入口，杜绝「凭空解锁」。 */
    fun beginUnlockAttempt() { systemAuthenticationInProgress = true }

    fun cancelUnlockAttempt() { systemAuthenticationInProgress = false }

    fun pinLockRemainingSeconds(): Long =
        ((_uiState.value.pinLockedUntilMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)

    /**
     * PIN 解锁：连续 5 次失败后冷却 30 秒。冷却与失败计数只在本次进程内保留，
     * 应用锁本身不依赖它们做安全边界（PIN 摘要也不在明文里）。
     */
    fun unlockWithPin(pin: String): Boolean {
        if (!systemAuthenticationInProgress) return false
        if (pinLockRemainingSeconds() > 0) return false
        if (!verifyPin(pin)) {
            pinFailures++
            if (pinFailures >= MAX_PIN_ATTEMPTS) {
                pinFailures = 0
                _uiState.update { it.copy(pinLockedUntilMs = System.currentTimeMillis() + PIN_LOCKOUT_MS) }
                showSnackbar("尝试次数过多，请 30 秒后重试", SnackbarTone.ERROR)
            }
            return false
        }
        pinFailures = 0
        _uiState.update { it.copy(pinLockedUntilMs = 0L) }
        return performUnlock()
    }

    fun unlockAppAndVault() {
        if (!systemAuthenticationInProgress && !_uiState.value.showQuickAuthSheet) return
        performUnlock()
    }

    /** 验证通过后的统一状态跃迁，避免 PIN / 生物识别 / 系统凭据三条路径各自改状态。 */
    private fun performUnlock(): Boolean {
        systemAuthenticationInProgress = false
        backgroundAt = null
        val action = pendingPostAuthAction
        pendingPostAuthAction = null
        _uiState.update {
            it.copy(
                isAppLockedNow = false, isVaultUnlocked = true, showQuickAuthSheet = false
            )
        }
        _snackbar.value = null
        action?.invoke()
        pendingExternalUri?.let { uri ->
            pendingExternalUri = null
            importExternalDocument(uri)
        }
        return true
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
        _uiState.update { it.copy(showQuickAuthSheet = false) }
        onForeground(false)
    }

    fun lockVaultSession() { lockAppImmediately() }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch(operationErrors) {
            if (enabled && !_uiState.value.hasCustomPin) {
                // 绝不允许「应用锁开着却还在用默认 1234」：先设 PIN，设置成功后再真正开启
                _uiState.update {
                    it.copy(requestPinSetup = true, pendingAppLockActivation = true)
                }
                showSnackbar("请先设置应用 PIN，设置完成后应用锁才会开启", SnackbarTone.INFO)
                return@launch
            }
            _uiState.update {
                it.copy(
                    isAppLockEnabled = enabled,
                    isAppLockedNow = if (enabled) it.isAppLockedNow else false,
                    pendingAppLockActivation = false
                )
            }
            preferences.save(_uiState.value)
            showSnackbar(if (enabled) "应用锁已开启" else "应用锁已关闭", SnackbarTone.SUCCESS)
        }
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

    fun setAutoLockPolicyLabel(label: String) {
        _uiState.update { it.copy(autoLockPolicyLabel = label) }
    }

    fun setLockOnScreenOff(enabled: Boolean) {
        _uiState.update { it.copy(lockOnScreenOff = enabled) }
    }

    fun setFlagSecureEnabled(enabled: Boolean) {
        _uiState.update { it.copy(flagSecureEnabled = enabled) }
    }

    fun setHideRecentsPreview(enabled: Boolean) {
        _uiState.update { it.copy(hideRecentsPreview = enabled) }
    }

    // ========================================================================
    // Book & Library Operations
    // ========================================================================
    fun setShowRecentReading(enabled: Boolean) {
        _uiState.update { it.copy(showRecentReading = enabled) }
    }

    /** 关闭统计时同时清空计时，避免关闭前的会话继续累加。 */
    fun setShowReadingStats(enabled: Boolean) {
        if (!enabled) readingStartedAt = 0L
        _uiState.update { it.copy(showReadingStats = enabled) }
    }

    fun removeSearchHistoryItem(item: String) {
        _uiState.update { state -> state.copy(recentSearches = state.recentSearches.filter { it != item }) }
    }

    fun setLibraryQuickStatusFilter(filter: String) {
        _uiState.update { it.copy(libraryQuickStatusFilter = filter) }
    }

    fun toggleGridView() = cycleLibraryLayoutMode()

    fun setLibraryLayoutMode(mode: LibraryLayoutMode) {
        _uiState.update { it.copy(libraryLayoutMode = mode) }
    }

    fun cycleLibraryLayoutMode() {
        _uiState.update { state ->
            val modes = LibraryLayoutMode.entries
            state.copy(libraryLayoutMode = modes[(modes.indexOf(state.libraryLayoutMode) + 1) % modes.size])
        }
    }

    fun setShelvesViewMode(mode: ShelvesViewMode) {
        _uiState.update { it.copy(shelvesViewMode = mode) }
    }

    fun cycleShelvesViewMode() {
        _uiState.update { state ->
            val modes = ShelvesViewMode.entries
            state.copy(shelvesViewMode = modes[(modes.indexOf(state.shelvesViewMode) + 1) % modes.size])
        }
    }

    fun toggleFavorite(bookId: String) {
        viewModelScope.launch(operationErrors) {
            val book = repository.getBookById(bookId) ?: return@launch
            repository.toggleFavorite(bookId)
            showSnackbar(if (!book.isFavorite) "已加入收藏《${book.title}》" else "已取消收藏《${book.title}》")
        }
    }

    // ------------------------------------------------------------------
    // 阅读状态：节流写库，旋转/重建后可恢复
    // ------------------------------------------------------------------

    fun updateReadingProgress(bookId: String, newPage: Int, chapterTitle: String? = null) {
        pendingProgress[bookId] = newPage to chapterTitle
        schedulePersist()
    }

    // ========================================================================
    // 阅读时长统计
    // ========================================================================

    private val statsStore = com.example.data.local.ReadingStatsStore(application)
    private val _readingStats = kotlinx.coroutines.flow.MutableStateFlow(
        com.example.data.local.ReadingStatsStore.Summary()
    )
    val readingStats: kotlinx.coroutines.flow.StateFlow<com.example.data.local.ReadingStatsStore.Summary> =
        _readingStats
    private var readingStartedAt = 0L
    private var readingHeartbeatJob: kotlinx.coroutines.Job? = null

    fun refreshReadingStats() {
        viewModelScope.launch {
            _readingStats.value = runCatching { statsStore.read() }
                .getOrElse { com.example.data.local.ReadingStatsStore.Summary() }
        }
    }

    fun beginReadingSession() {
        if (!_uiState.value.showReadingStats) return
        if (readingStartedAt == 0L) readingStartedAt = android.os.SystemClock.elapsedRealtime()
        // 每整分钟落一次盘：进程被系统杀死时最多丢最后一分钟，而不是整段阅读时长
        readingHeartbeatJob?.cancel()
        readingHeartbeatJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                runCatching { statsStore.record(1.0) }
            }
        }
    }

    fun endReadingSession() {
        readingHeartbeatJob?.cancel()
        readingHeartbeatJob = null
        val started = readingStartedAt
        readingStartedAt = 0L
        if (started == 0L) return
        // 心跳已按整分钟入账，这里只补最后不足一分钟的零头
        val remainder = ((android.os.SystemClock.elapsedRealtime() - started) % 60_000) / 60_000.0
        if (remainder < 0.01) return
        viewModelScope.launch {
            runCatching { statsStore.record(remainder) }
            refreshReadingStats()
        }
    }

    fun updateViewport(bookId: String, page: Int, offsetFraction: Float, scale: Float, panX: Float, panY: Float) {
        pendingViewports[bookId] = ReaderViewport(page, offsetFraction, scale, panX, panY)
        pendingProgress[bookId] = page to null
        schedulePersist()
    }

    private fun schedulePersist() {
        if (persistJob?.isActive == true) return
        persistJob = viewModelScope.launch(operationErrors) {
            while (pendingProgress.isNotEmpty() || pendingViewports.isNotEmpty()) {
                kotlinx.coroutines.delay(900)
                flushReadingState()
            }
        }
    }

    private suspend fun flushReadingState() = progressMutex.withLock {
        val progressSnapshot = pendingProgress.entries.map { it.key to it.value }
        for ((id, requested) in progressSnapshot) {
            pendingProgress.remove(id, requested)
            val book = repository.getBookById(id) ?: continue
            val page = requested.first.coerceIn(1, book.totalPages.coerceAtLeast(1))
            val percent = (page.toLong() * 100 / book.totalPages.coerceAtLeast(1)).toInt().coerceIn(0, 100)
            if (book.currentPage != page || book.lastReadTimestamp == 0L ||
                (requested.second != null && book.currentChapterTitle != requested.second)) {
                repository.updateProgress(id, page, percent, if (percent >= 100) "已读完" else "在读",
                    requested.second, System.currentTimeMillis())
            }
        }
        val viewportSnapshot = pendingViewports.entries.map { it.key to it.value }
        for ((id, viewport) in viewportSnapshot) {
            pendingViewports.remove(id, viewport)
            runCatching { repository.saveViewport(id, viewport) }
        }
    }

    fun saveReaderOptionsFor(bookId: String) {
        viewModelScope.launch(operationErrors) {
            val state = _uiState.value
            repository.saveReaderOptions(bookId, state.readerLayoutMode.name, state.fitModeLabel, state.smartSpreadStitch)
        }
    }

    fun releaseReaderResources() {
        repository.trimPageMemory()
        cleanupScope.launch {
            try { flushReadingState() }
            catch (_: Exception) { showSnackbar("阅读进度保存失败，请检查可用空间") }
            finally { runCatching { repository.closeReadSession() } }
        }
    }

    override fun onCleared() {
        cleanupScope.launch {
            try { flushReadingState() }
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
            showSnackbar("已保存元数据")
        }
    }

    fun setCoverPage(bookId: String, page: Int) {
        viewModelScope.launch(operationErrors) {
            if (repository.setCoverPage(bookId, page)) showSnackbar("封面已更新为第 $page 页")
            else showSnackbar("无法从第 $page 页生成封面")
        }
    }

    fun moveSeriesOrder(bookId: String, series: String, order: Int) {
        viewModelScope.launch(operationErrors) {
            repository.updateSeries(bookId, series.trim(), order)
            showSnackbar("已更新系列与卷序")
        }
    }

    fun openNextVolume(current: BookEntity) {
        viewModelScope.launch(operationErrors) {
            val next = repository.seriesBooks(current.series)
                .filter { it.id != current.id && it.seriesOrder >= current.seriesOrder }
                .minByOrNull { it.seriesOrder }
            if (next == null) {
                showSnackbar(if (current.series.isBlank()) "这是单卷作品" else "已是《${current.series}》最后一卷")
                return@launch
            }
            _uiState.update { state ->
                state.copy(subScreenStack = state.subScreenStack.dropLast(1) + SubScreen.Reader(next.id, null))
            }
        }
    }

    // ========================================================================
    // Batch Selection
    // ========================================================================
    fun toggleBookBatchSelection(bookId: String) {
        _uiState.update { state ->
            val nextSet = if (bookId in state.selectedBookIds) state.selectedBookIds - bookId
            else state.selectedBookIds + bookId
            state.copy(isBatchSelectionMode = nextSet.isNotEmpty(), selectedBookIds = nextSet)
        }
    }

    fun enterBatchSelection(initialBookId: String? = null) {
        _uiState.update {
            it.copy(isBatchSelectionMode = true,
                selectedBookIds = if (initialBookId != null) setOf(initialBookId) else emptySet())
        }
    }

    fun selectAllVisibleBooks(bookIds: List<String>) {
        _uiState.update { it.copy(isBatchSelectionMode = true, selectedBookIds = bookIds.toSet()) }
    }

    fun exitBatchSelection() {
        _uiState.update { it.copy(isBatchSelectionMode = false, selectedBookIds = emptySet()) }
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

    fun batchRemoveSelectedFromLibrary() {
        val ids = _uiState.value.selectedBookIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(operationErrors) {
            repository.removeBooks(ids)
            exitBatchSelection()
            showSnackbar("已移除 ${ids.size} 部作品索引（保留外部源文件）")
        }
    }

    // ========================================================================
    // Search & Filters
    // ========================================================================
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        recordNavSession()
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
            val next = if (format in state.searchFormatFilters) state.searchFormatFilters - format
            else state.searchFormatFilters + format
            state.copy(searchFormatFilters = next)
        }
    }

    fun toggleSearchTagFilter(tag: String) {
        _uiState.update { state ->
            val next = if (tag in state.searchTagFilters) state.searchTagFilters - tag
            else state.searchTagFilters + tag
            state.copy(searchTagFilters = next)
        }
    }

    fun toggleSearchStorageFilter(storage: String) {
        _uiState.update { state ->
            val next = if (storage in state.searchStorageFilters) state.searchStorageFilters - storage
            else state.searchStorageFilters + storage
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
    // Shelves
    // ========================================================================
    fun createCustomShelf(name: String, subtitle: String, isProtected: Boolean) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch(operationErrors) {
            repository.insertShelf(
                ShelfEntity(
                    id = "shelf_${System.currentTimeMillis()}",
                    name = cleanName,
                    subtitle = subtitle.trim().ifBlank { "0 本 · 刚刚创建" },
                    itemCountDisplay = 0,
                    updatedLabel = "刚刚创建",
                    coverUrlsCsv = "",
                    isProtected = isProtected,
                    sortOrder = allShelves.value.size
                )
            )
            showSnackbar("已新建书架「$cleanName」")
        }
    }

    fun deleteCustomShelf(shelfId: String) {
        viewModelScope.launch(operationErrors) {
            allBooks.value.forEach { book ->
                val shelfIds = book.shelfIdsCsv.split(",").filter { it.isNotBlank() }
                if (shelfId in shelfIds) {
                    repository.updateBook(book.copy(shelfIdsCsv = shelfIds.filter { it != shelfId }.joinToString(",")))
                }
            }
            repository.deleteShelfById(shelfId)
            showSnackbar("已删除书架分类（书籍本体保留）")
        }
    }

    fun deleteRepository(repoId: String) {
        viewModelScope.launch(operationErrors) {
            repository.deleteRepository(repoId)
            showSnackbar("已移除书库目录")
        }
    }

    /** 按 sortOrder 交换相邻书架，真正改变展示顺序。 */
    fun moveShelf(shelf: ShelfEntity, offset: Int) {
        viewModelScope.launch(operationErrors) {
            val ordered = allShelves.value.sortedBy { it.sortOrder }
            val index = ordered.indexOfFirst { it.id == shelf.id }
            val target = index + offset
            if (index < 0 || target !in ordered.indices) return@launch
            val other = ordered[target]
            repository.updateShelf(shelf.copy(sortOrder = other.sortOrder))
            repository.updateShelf(other.copy(sortOrder = shelf.sortOrder))
        }
    }

    fun moveShelfUp(shelf: ShelfEntity) = moveShelf(shelf, -1)
    fun moveShelfDown(shelf: ShelfEntity) = moveShelf(shelf, 1)

    // ========================================================================
    // Annotations
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
    // Import & Repositories
    // ========================================================================
    /**
     * 由系统「打开方式 / 分享」进入：导入（已存在则复用）并直接进入阅读。
     * 应用锁未通过时先不跳转，避免绕过验证。
     */
    /** 外部「打开方式 / 分享」进来的文件：锁屏期间只排队，不写数据库也不生成封面。 */
    fun openExternalDocument(uri: Uri) {
        viewModelScope.launch(operationErrors) {
            // 等设置加载完成再判断，避免 onCreate 时 settingsLoaded=false 误判
            uiState.filter { it.settingsLoaded }.first()
            if (_uiState.value.isAppLockedNow) {
                pendingExternalUri = uri
                return@launch
            }
            importExternalDocument(uri)
        }
    }

    private fun importExternalDocument(uri: Uri) {
        viewModelScope.launch(operationErrors) {
            val book = runCatching { repository.importDocument(uri) }.getOrElse { failure ->
                showSnackbar(failure.message ?: "无法打开该文件", SnackbarTone.ERROR)
                return@launch
            }
            _uiState.update {
                it.copy(
                    currentTab = MainNavDestination.LIBRARY,
                    subScreenStack = listOf(SubScreen.Reader(book.id))
                )
            }
            recordNavSession()
        }
    }

    /** 导出书库清单到用户选择的 JSON 文件；不含漫画文件、PIN 或隐藏内容凭据。 */
    fun exportLibrary(uri: Uri) {
        viewModelScope.launch(operationErrors) {
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { stream ->
                        repository.exportLibraryTo(stream)
                    } ?: error("无法写入备份文件")
                }
            }
            outcome.onSuccess { showSnackbar("书库清单已导出", SnackbarTone.SUCCESS) }
                .onFailure { showSnackbar(it.message ?: "导出失败，请检查可用空间", SnackbarTone.ERROR) }
        }
    }

    /** 导入书库清单：恢复进度、标签、书架、批注；来源 URI 失效的作品标记 MISSING。 */
    fun importLibrary(uri: Uri) {
        viewModelScope.launch(operationErrors) {
            val outcome = runCatching {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { stream ->
                        repository.importLibraryFrom(stream)
                    } ?: error("无法读取备份文件")
                }
            }
            outcome.onSuccess { result ->
                showSnackbar(
                    "已恢复 ${result.restored + result.updated} 部作品、${result.annotations} 条批注；来源需重新授权",
                    SnackbarTone.SUCCESS
                )
            }.onFailure { showSnackbar(it.message ?: "导入失败，请确认是有效的备份文件", SnackbarTone.ERROR) }
        }
    }

    fun importLocalFileUri(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(operationErrors) {
            try {
                val book = repository.importDocument(uri)
                showSnackbar("已导入《${book.title}》· ${book.totalPages} 页")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                showSnackbar(failure.message ?: "导入失败，请检查文件格式与授权")
            }
        }
    }

    fun addRepositoryAndScan(repoName: String, scanRule: String) {
        _uiState.update { it.copy(pendingRepositoryName = repoName, requestDirectoryPicker = true) }
    }

    private fun startScan(operation: suspend () -> String) {
        if (_uiState.value.isScanning) { showSnackbar("正在扫描，可先取消"); return }
        _uiState.update { it.copy(isScanning = true) }
        _scanProgress.value = ScanProgress()
        scanJob = viewModelScope.launch(operationErrors) {
            try { showSnackbar(operation()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { showSnackbar(failure.message ?: "扫描失败，请重新授权目录") }
            finally { _uiState.update { it.copy(isScanning = false) } }
        }
    }

    private fun reportScanProgress(processed: Int, discovered: Int) {
        _scanProgress.value = ScanProgress(processed, discovered)
    }

    fun cancelScanning(notify: Boolean = true) {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
        if (notify) showSnackbar("已取消扫描，已导入的漫画保留")
    }

    fun onDirectorySelected(uri: Uri?) {
        val state = _uiState.value
        _uiState.update { it.copy(requestDirectoryPicker = false) }
        if (uri == null) return
        startScan {
            val report = repository.scanRepository(
                uri, state.pendingRepositoryName, "每个文件作为作品"
            ) { processed, discovered -> reportScanProgress(processed, discovered) }
            scanSummary(report)
        }
    }

    fun rescanRepositories() {
        if (_uiState.value.isScanning) { cancelScanning(); return }
        startScan {
            val covers = repository.refreshMissingCovers()
            val repos = repository.observeAllRepositories().first()
            if (repos.isEmpty()) {
                return@startScan if (covers > 0) "已补全 $covers 本封面" else "尚无书库，请先选择目录"
            }
            var imported = 0
            var updated = 0
            var failed = 0
            var missing = 0
            repos.forEach { repo ->
                try {
                    val report = repository.scanRepository(
                        Uri.parse(repo.pathUri), repo.name, repo.scanRule
                    ) { processed, discovered -> reportScanProgress(processed, discovered) }
                    imported += report.imported
                    updated += report.updated
                    failed += report.failed
                    missing += report.missing
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    failed++
                }
            }
            buildString {
                if (covers > 0) append("补全 $covers 本封面 · ")
                append("新增 $imported，更新 $updated")
                if (failed > 0) append("，失败 $failed")
                if (missing > 0) append("，失联 $missing")
            }
        }
    }

    private fun scanSummary(report: com.example.data.local.ScanReport): String = buildString {
        append("扫描完成：新增 ${report.imported}")
        if (report.updated > 0) append("，更新 ${report.updated}")
        if (report.failed > 0) append("，失败 ${report.failed}")
        if (report.missing > 0) append("，失联 ${report.missing}")
    }

    /** 失联作品重新定位：目录作品走目录选择器，其余走文件选择器。 */
    fun beginRelocate(bookId: String) {
        val book = allBooks.value.firstOrNull { it.id == bookId }
        _uiState.update {
            it.copy(
                pendingRelocateBookId = bookId,
                pendingRelocateIsFolder = book?.format == "IMAGE_DIR"
            )
        }
    }

    fun relocateBookSource(uri: Uri?) {
        val bookId = _uiState.value.pendingRelocateBookId
        _uiState.update { it.copy(pendingRelocateBookId = null, pendingRelocateIsFolder = false) }
        if (uri == null || bookId == null) return
        viewModelScope.launch(operationErrors) {
            if (repository.relocateBookSource(bookId, uri)) showSnackbar("已重新关联来源，阅读进度保留")
            else showSnackbar("重新关联失败，请确认选择的文件与原作品一致")
        }
    }

    // ========================================================================
    // 阅读设置与主题
    // ========================================================================
    fun cycleReaderLayoutMode() {
        _uiState.update { state ->
            val modes = ReaderLayoutMode.entries
            state.copy(readerLayoutMode = modes[(modes.indexOf(state.readerLayoutMode) + 1) % modes.size])
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

    fun setReaderBrightness(value: Float) {
        _uiState.update { it.copy(readerBrightness = value.coerceIn(0.2f, 1f)) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setReduceMotion(enabled: Boolean) {
        _uiState.update { it.copy(reduceMotion = enabled) }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearRegenerableCache() {        viewModelScope.launch(operationErrors) {
            repository.trimPageMemory()
            coil.Coil.imageLoader(getApplication()).memoryCache?.clear()
            val covers = repository.clearCoverCache()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                coil.Coil.imageLoader(getApplication()).diskCache?.clear()
            }
            repository.refreshMissingCovers()
            val mb = String.format(java.util.Locale.ROOT, "%.1f", covers / 1048576.0)
            showSnackbar("已清理缓存（封面 $mb MB），原文件未修改")
        }
    }

    /** 强制按当前封面页重建全部封面：用于修复历史版本没生成成功的封面。 */
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun rebuildLibraryCovers() {
        viewModelScope.launch(operationErrors) {
            coil.Coil.imageLoader(getApplication()).memoryCache?.clear()
            val count = repository.refreshAllBookCovers()
            showSnackbar(
                if (count > 0) "已重建 $count 本封面" else "没有可重建的封面，请检查来源是否仍可访问",
                if (count > 0) SnackbarTone.SUCCESS else SnackbarTone.INFO
            )
        }
    }

    fun toggleBookmark(bookId: String, page: Int, chapter: String) {
        viewModelScope.launch(operationErrors) {
            annotationMutex.withLock {
                val marks = repository.observeAnnotationsForBook(bookId).first()
                if (marks.any { it.type == "BOOKMARK" && it.pageNumber == page }) repository.removeBookmark(bookId, page)
                else repository.insertAnnotation(AnnotationEntity(bookId = bookId, chapterTitle = chapter,
                    pageNumber = page, quoteText = "", noteText = "", colorName = "GREEN", type = "BOOKMARK",
                    createdAtText = "刚刚"))
            }
        }
    }

    fun showSnackbar(msg: String, tone: SnackbarTone = toneFor(msg)) {
        _snackbar.value = SnackbarEvent(msg, tone)
    }

    private fun toneFor(message: String): SnackbarTone = when {
        message.contains("失败") || message.contains("无法") || message.contains("不能") ||
            message.contains("错误") || message.contains("过多") || message.contains("超出") -> SnackbarTone.ERROR
        message.contains("已") || message.contains("完成") -> SnackbarTone.SUCCESS
        else -> SnackbarTone.INFO
    }

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    companion object {
        const val DEFAULT_PIN = "1234"

        private const val MAX_PIN_ATTEMPTS = 5
        private const val PIN_LOCKOUT_MS = 30_000L

        /** 进程重建后恢复上次会话的有效期。 */
        private const val NAV_SESSION_WINDOW_MS = 30L * 60 * 1000
    }
}
