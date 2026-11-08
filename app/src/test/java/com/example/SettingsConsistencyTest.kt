package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.PrivacySecuritySettingsScreen
import com.example.ui.theme.AsterReaderTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
open class SettingsConsistencyTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    private lateinit var rootView: android.view.View
    private val model by lazy {
        AsterViewModel(ApplicationProvider.getApplicationContext<Application>()).also { store.put("settings", it) }
    }
    @After fun close() { store.clear() }

    @Test fun settingsHomeHasOnlyCategoryNavigation() {
        content()
        compose.onNodeWithText("默认阅读配置").assertDoesNotExist()
        compose.onNodeWithText("诊断与帮助").assertDoesNotExist()
        compose.onNodeWithText("外观").assertIsDisplayed()
        compose.onNodeWithText("安全").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("关于").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("关于").performClick()
        compose.runOnIdle { check(model.uiState.value.activeSubScreen == com.example.ui.SubScreen.SettingsSection("about")) }
        capture("settings-home-light")
    }

    @Test fun fileVaultCreationRequiresConfirmationAndRecoveryAcknowledgement() {
        content(section = "fileVault")
        compose.onNode(hasSetTextAction() and hasText("口令")).performTextInput("rT8-Vp3_Zm7_Qe4-nK9")
        compose.onNode(hasSetTextAction() and hasText("确认口令")).performTextInput("rT8-Vp3_Zm7_Qe4-nK9")
        compose.onAllNodesWithText("建立保险库").filter(hasClickAction()).onFirst().performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("我已保存口令并了解恢复条件").performScrollTo().performClick()
        compose.onAllNodesWithText("建立保险库").filter(hasClickAction()).onFirst().performScrollTo().assertIsEnabled()
        capture("file-vault-create")
    }

    @Test fun lockedFileVaultControlsRemainReachableInDarkLargeFont() {
        content(section = "fileVault", dark = true, fontScale = 2f)
        compose.onNodeWithText("生成随机口令").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("建立保险库").filter(hasClickAction()).onFirst().performScrollTo().assertIsDisplayed()
        capture("file-vault-dark-font-200")
    }

    @Test fun readingOptionsRemainReachableAtDoubleFontScale() {
        readerContent(fontScale = 2f)
        compose.onNodeWithText("音量键翻页").performScrollTo().assertIsDisplayed().performClick()
        capture("reading-font-200")
    }

    @Test fun preloadControlsAllowSixteenPagesInBothDirections() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val state by model.uiState.collectAsState()
            AsterReaderTheme {
                androidx.compose.material3.Surface {
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    com.example.ui.components.ReaderSettingsContent(state, model, state.readerLayoutMode,
                        state.fitModeLabel, state.smartSpreadStitch, false, {}, {}, {}, {}, {}, true, {})
                }
                }
            }
        }
        compose.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            model.uiState.value.settingsLoaded
        }
        for (label in listOf("向上预加载", "向下预加载")) {
            compose.onNodeWithContentDescription(label).performScrollTo()
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(16f) }
        }
        compose.runOnIdle {
            org.junit.Assert.assertEquals(16, model.uiState.value.preloadBefore)
            org.junit.Assert.assertEquals(16, model.uiState.value.preloadAfter)
        }
        capture("reading-preload-sixteen")
    }

    @Test fun appearanceUsesDarkTheme() {
        content(section = "appearance", dark = true)
        compose.onNodeWithText("深色模式").assertIsDisplayed()
        compose.onNodeWithText("减少动效").assertIsDisplayed()
        capture("appearance-dark")
    }

    @Test fun globalColorsAlwaysUseSystemMonetInBothThemes() {
        var dark by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            AsterReaderTheme(darkTheme = dark) {
                val expected = if (dark) androidx.compose.material3.dynamicDarkColorScheme(context)
                    else androidx.compose.material3.dynamicLightColorScheme(context)
                val actual = androidx.compose.material3.MaterialTheme.colorScheme
                val extended = com.example.ui.theme.LocalAsterColors.current
                check(actual.primary == expected.primary)
                check(actual.secondaryContainer == expected.secondaryContainer)
                check(actual.surfaceContainer == expected.surfaceContainer)
                check(extended.surfaceContainerHigh == expected.surfaceContainerHigh)
                check(extended.isDark == dark)
            }
        }
        compose.runOnIdle { dark = true }
        compose.waitForIdle()
    }

    @Test fun privacyUsesTheSamePageStructure() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme { PrivacySecuritySettingsScreen(AsterUiState(settingsLoaded = true), model, {}) }
        }
        compose.onNodeWithText("应用 PIN").assertIsDisplayed()
        compose.onNodeWithText("隐藏任务预览").performScrollTo().assertIsDisplayed()
        capture("privacy-light")
    }

    @Test fun choicesUseNativeSegmentedButtonsAndLargeFontCheckRows() {
        var selected by androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                com.asterreader.core.designsystem.AsterSettingsChoice(
                    com.example.ui.components.readerFitChoices, selected, { selected = it })
            }
        }
        compose.onNodeWithText("适合屏幕").assertIsSelected()
        compose.onNodeWithText("原始比例").performClick().assertIsSelected()
        capture("settings-native-segmented")
    }

    @Test fun darkThemeChoicesAtDoubleFontScaleRemainReachable() {
        content(section = "appearance", dark = true, fontScale = 2f)
        compose.onNodeWithText("跟随系统").assertIsSelected()
        compose.onNodeWithText("深色模式").assertIsDisplayed()
        capture("settings-native-choices-dark-font-200")
    }

    @Test fun encryptedRestoreShowsTargetAndPasswordErrorStates() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                SettingsScreen(AsterUiState(settingsLoaded = true, encryptedBackupHasTarget = true,
                    encryptedBackupError = "口令不正确或文件损坏"), emptyList(), 0, model, {}, {}, "encryptedRestore")
            }
        }
        compose.onNodeWithText("口令不正确或文件损坏").assertIsDisplayed()
        compose.onNodeWithText("恢复").assertIsNotEnabled()
        capture("encrypted-restore-error")
    }

    @Test fun statsCanBeHiddenFromTheFloatingNavigation() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                com.example.ui.components.AsterMainNavigation(false, com.example.ui.components.MainNavDestination.LIBRARY,
                    {}, showStatsTab = false)
            }
        }
        compose.onNodeWithTag("nav_dock_stats").assertDoesNotExist()
        compose.onNodeWithTag("nav_dock_search").assertIsDisplayed()
    }

    @Test fun readerUsesCompactMonetControlsAndNoSideZoomButtons() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val source = File(app.cacheDir, "布局验收.zip")
        val image = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.LTGRAY) }
        java.util.zip.ZipOutputStream(source.outputStream()).use { zip ->
            repeat(4) { index ->
                zip.putNextEntry(java.util.zip.ZipEntry("${index + 1}.png"))
                image.compress(Bitmap.CompressFormat.PNG, 100, zip)
                zip.closeEntry()
            }
        }
        image.recycle()
        val book = kotlinx.coroutines.runBlocking {
            com.example.data.local.LibraryRepository(app).importDocument(android.net.Uri.fromFile(source), persist = false)
        }
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                com.example.ui.screens.ReaderScreen(book, uiState = AsterUiState(settingsLoaded = true, isAppLockedNow = false),
                    isExpandedTablet = false, viewModel = model, onBack = {})
            }
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("放大页面").assertDoesNotExist()
        compose.onNodeWithContentDescription("缩小页面").assertDoesNotExist()
        compose.onNodeWithText("前 10 页").assertDoesNotExist()
        capture("reader-chrome-hidden")
        compose.onNodeWithTag("publication_canvas").performTouchInput { click(center) }
        compose.onNodeWithText("前 10 页").assertIsDisplayed()
        compose.onNodeWithTag("publication_canvas").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("前 10 页").assertDoesNotExist()
        compose.onNodeWithTag("publication_canvas").performTouchInput { click(center) }
        compose.onNodeWithText("前 10 页").assertIsDisplayed()
        compose.onNodeWithText("后 10 页").assertIsDisplayed()
        val pill = compose.onNodeWithTag("reader_mini_status_pill").getUnclippedBoundsInRoot()
        val bounds = compose.onRoot().getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue((bounds.right - pill.right).value <= 4f)
        org.junit.Assert.assertTrue((pill.bottom - pill.top).value < 20f)
        capture("reader-compact-monet")
        compose.onNodeWithText("后 10 页").performClick()
        try {
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("4 / 4 · 100%").fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            println(compose.onRoot().printToString())
            throw failure
        }
        compose.onNodeWithText("4 / 4 · 100%").assertIsDisplayed()
        compose.onNodeWithText("后 10 页").assertIsNotEnabled()
        compose.onNodeWithText("设置").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithTag("reader_settings_page").assertIsDisplayed()
        val firstMode = compose.onNodeWithText("滚动").getUnclippedBoundsInRoot()
        listOf("RTL", "LTR", "双页").forEach { label ->
            val option = compose.onNodeWithText(label).assertIsDisplayed().getUnclippedBoundsInRoot()
            org.junit.Assert.assertEquals(firstMode.top.value, option.top.value, 0.5f)
            org.junit.Assert.assertEquals((firstMode.right - firstMode.left).value, (option.right - option.left).value, 0.5f)
        }
        org.junit.Assert.assertEquals(48f, (firstMode.bottom - firstMode.top).value, 0.5f)
        compose.onNodeWithContentDescription("右向左翻页").assertIsDisplayed()
        compose.onNodeWithContentDescription("恢复默认").assertIsDisplayed()
        captureDialog("reader-settings-monet")
        compose.onNodeWithText("缩放").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("返回阅读").performClick()
        compose.onNodeWithTag("reader_settings_page").assertDoesNotExist()
        compose.onNodeWithTag("reader_screen_root").assertIsDisplayed()
    }

    @Test fun settingsRowsHaveTheSameCompactHeight() {
        content()
        val first = compose.onNodeWithText("外观").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("阅读统计").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals(52f, (first.bottom - first.top).value, 0.5f)
        org.junit.Assert.assertEquals((first.bottom - first.top).value, (second.bottom - second.top).value, 0.5f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-xhdpi")
    fun landscapeReadingOptionsRemainReachable() {
        readerContent(fontScale = 2f, dark = true)
        compose.onNodeWithText("双页").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("封面单页").performScrollTo().assertIsDisplayed()
        capture("reading-landscape-font-200")
    }

    @Test @Config(qualifiers = "w320dp-h640dp-xhdpi")
    fun narrowReaderModeRowScrollsWithoutWrappingAtLargeFontScale() {
        readerContent(fontScale = 2f)
        val top = compose.onNodeWithText("滚动").getUnclippedBoundsInRoot().top
        compose.onNodeWithText("双页").performScrollTo().performClick().assertIsSelected()
        val last = compose.onNodeWithText("双页").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals(top.value, last.top.value, 0.5f)
        org.junit.Assert.assertTrue((last.bottom - last.top).value >= 48f)
        capture("reader-single-row-narrow-font-200")
    }

    @Test fun emptyShelfManagementHasNoPlaceholderActions() {
        content(section = "shelves")
        compose.onNodeWithText("新建书架").assertIsDisplayed()
        capture("shelf-management-empty")
    }

    @Test @Config(fontScale = 2f)
    fun shelfCreationAtDoubleFontScaleKeepsValidationAndManagementReachable() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme(darkTheme = true) {
                com.example.ui.screens.ShelvesScreen(emptyList(), emptyList(), 0,
                    AsterUiState(settingsLoaded = true), false, model)
            }
        }
        compose.onNodeWithContentDescription("排序与删除").performClick()
        compose.runOnIdle { check(model.uiState.value.activeSubScreen == com.example.ui.SubScreen.SettingsSection("shelves")) }
        compose.onNodeWithTag("create_shelf_button").performClick()
        compose.onNodeWithText("创建").assertIsNotEnabled()
        compose.onNodeWithText("书架名称").performTextInput("旅行漫画")
        compose.onNodeWithText("创建").assertIsEnabled().assertIsDisplayed()
        captureDialog("shelf-create-dark-font-200")
    }

    @Test fun libraryKeepsItsFloatingIslandNavigation() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme { com.example.AsterReaderAppRoot(model, AsterUiState(settingsLoaded = true, isAppLockedNow = false)) }
        }
        awaitLibraryReady()
        capture("library-empty")
        compose.onNodeWithTag("nav_dock_library").assertIsDisplayed()
        compose.onNodeWithTag("nav_dock_search").assertIsDisplayed()
        val island = compose.onNodeWithTag("apple_floating_bottom_dock").getUnclippedBoundsInRoot()
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(island.left > root.left && island.right < root.right)
        org.junit.Assert.assertTrue(island.bottom < root.bottom)
        org.junit.Assert.assertTrue((island.bottom - island.top).value >= 64f)
    }

    @Test fun floatingIslandTabsWorkAtDoubleFontScale() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                AsterReaderTheme {
                    var current by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(com.example.ui.components.MainNavDestination.LIBRARY)
                    }
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                        com.example.ui.components.AsterMainNavigation(false, current, { current = it },
                            Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
                    }
                }
            }
        }
        compose.onNodeWithTag("nav_dock_library").assertIsSelected()
        compose.onNodeWithTag("nav_dock_shelves").performClick().assertIsSelected()
        compose.onNodeWithTag("nav_dock_search").performClick().assertIsSelected()
        capture("floating-island-font-200")
    }

    @Test @Config(qualifiers = "w1100dp-h800dp-xhdpi")
    fun tabletDetailActionsRemainVisibleAtDoubleFontScale() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                AsterReaderTheme {
                    com.example.ui.screens.LibraryHomeScreen(
                        AsterUiState(settingsLoaded = true), listOf(comicFixture("平板布局")),
                        emptyList(), true, model, {}
                    )
                }
            }
        }
        compose.onNodeWithTag("tablet_start_reading").assertIsDisplayed()
        val action = compose.onNodeWithTag("tablet_start_reading").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue((action.bottom - action.top).value >= 48f)
        capture("tablet-detail-font-200")
    }

    @Test @Config(qualifiers = "w600dp-h960dp-xhdpi")
    fun mediumWindowUsesAllThreeRailDestinations() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme { com.example.AsterReaderAppRoot(model, AsterUiState(settingsLoaded = true, isAppLockedNow = false)) }
        }
        awaitLibraryReady()
        compose.onNodeWithTag("main_navigation_rail").assertIsDisplayed()
        compose.onNodeWithTag("nav_dock_library").assertIsDisplayed()
        compose.onNodeWithTag("nav_dock_shelves").assertIsDisplayed()
        compose.onNodeWithTag("nav_dock_search").assertIsDisplayed().performClick()
        compose.runOnIdle { check(model.uiState.value.currentTab == com.example.ui.components.MainNavDestination.SEARCH) }
        capture("tablet-medium-navigation")
    }

    private fun awaitLibraryReady() {
        compose.waitUntil(10000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            model.mainContentReady.value
        }
        compose.waitForIdle()
    }

    @Test @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun tabletSearchConstrainsHeaderAndResultsTogether() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                com.example.ui.screens.SearchAndFilterScreen(AsterUiState(settingsLoaded = true),
                    listOf(comicFixture().copy(title = "平板搜索")), openFilterInitially = false, viewModel = model, onBack = {})
            }
        }
        val header = compose.onNodeWithTag("search_content_header").getUnclippedBoundsInRoot()
        val results = compose.onNodeWithTag("search_results").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals(840f, (header.right - header.left).value, 1f)
        org.junit.Assert.assertEquals(header.left.value, results.left.value, 1f)
        org.junit.Assert.assertEquals(header.right.value, results.right.value, 1f)
        org.junit.Assert.assertTrue(header.left.value > 0f)
        compose.waitUntil(5_000) { compose.onAllNodesWithText("平板搜索", substring = true).fetchSemanticsNodes().isNotEmpty() }
        capture("tablet-search-results")
    }

    @Test fun homeImportActionsHaveEqualBounds() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                com.example.ui.components.AsterEmptyState("空书库", "导入作品开始阅读", actionLabel = "导入书籍", onAction = {},
                    secondaryLabel = "添加目录", onSecondaryAction = {})
            }
        }
        val left = compose.onNodeWithText("导入书籍").getUnclippedBoundsInRoot()
        val right = compose.onNodeWithText("添加目录").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals((left.right - left.left).value, (right.right - right.left).value, 1f)
        org.junit.Assert.assertEquals((left.bottom - left.top).value, (right.bottom - right.top).value, 1f)
        capture("home-paired-actions")
    }

    @Test fun detailActionsReserveTheirOwnSpaceAtDoubleFontScale() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                AsterReaderTheme {
                    com.example.ui.screens.PublicationDetailScreen(comicFixture("布局检查"),
                        AsterUiState(settingsLoaded = true), false, model, {}, {})
                }
            }
        }
        compose.onNodeWithTag("detail_start_reading_cta").assertIsDisplayed()
        val contentBounds = compose.onNodeWithTag("detail_floating_action_island").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue((contentBounds.bottom - contentBounds.top).value >= 48f)
        capture("detail-font-200")
    }

    @Test fun metadataEditorIsAFullPageAtDoubleFontScale() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                AsterReaderTheme {
                    com.example.ui.screens.PublicationPropertiesScreen(comicFixture("编辑布局"), "metadata", model, {})
                }
            }
        }
        compose.onNodeWithText("编辑作品信息").assertIsDisplayed()
        compose.onNodeWithText("保存").performScrollTo().assertIsDisplayed()
        capture("metadata-font-200")
    }

    @Test fun metadataBackAsksBeforeDiscardingEdits() {
        var closed = false
        compose.setContent {
            AsterReaderTheme {
                com.example.ui.screens.PublicationPropertiesScreen(comicFixture("未保存检查"), "metadata", model, { closed = true })
            }
        }
        compose.onNodeWithText("标题").performTextReplacement("修改后的标题")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("放弃未保存的修改？").assertIsDisplayed()
        org.junit.Assert.assertFalse(closed)
        compose.onNodeWithText("继续编辑").performClick()
        compose.onNodeWithText("修改后的标题").assertIsDisplayed()
    }

    @Test fun publicMetadataDraftSurvivesStateRestoration() = checkMetadataDraftRestoration(false, false)

    @Test fun hiddenMetadataDraftIsExcludedFromStateRestoration() = checkMetadataDraftRestoration(true, false)

    @Test fun encryptedMetadataDraftIsExcludedFromStateRestoration() = checkMetadataDraftRestoration(false, true)

    private fun checkMetadataDraftRestoration(hidden: Boolean, encrypted: Boolean) {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        val book = comicFixture("状态恢复检查").copy(isVaultProtected = hidden, isVaultEncrypted = encrypted)
        restoration.setContent {
            AsterReaderTheme {
                com.example.ui.screens.PublicationPropertiesScreen(book, "metadata", model, {})
            }
        }
        compose.onNodeWithText("标题").performTextReplacement("未保存草稿")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction() and hasText(if (hidden || encrypted) book.title else "未保存草稿"))
            .assertIsDisplayed()
        if (hidden || encrypted) compose.onNodeWithText("未保存草稿").assertDoesNotExist()
    }

    @Test fun mainPagesShareTheShelfHeaderHeightAndTopInset() {
        var screen by androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                when (screen) {
                    0 -> com.example.ui.screens.ShelvesScreen(emptyList(), emptyList(), 0, AsterUiState(settingsLoaded = true), false, model)
                    1 -> com.example.ui.screens.LibraryHomeScreen(AsterUiState(settingsLoaded = true), emptyList(), emptyList(), false, model, {})
                    2 -> com.example.ui.screens.SearchAndFilterScreen(AsterUiState(settingsLoaded = true), emptyList(),
                        openFilterInitially = false, viewModel = model, onBack = {})
                    3 -> com.example.ui.screens.ReadingStatsScreen(model, 0, 0)
                    else -> com.example.ui.screens.OnboardingScreen({}, {}, {}, {})
                }
            }
        }
        val tags = listOf("shelves_main_header", "library_main_header", "search_content_header", "stats_main_header", "onboarding_main_header")
        val shelf = compose.onNodeWithTag(tags[0]).getUnclippedBoundsInRoot()
        tags.forEachIndexed { index, tag ->
            compose.runOnIdle { screen = index }
            val header = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            org.junit.Assert.assertEquals(40f, (header.bottom - header.top).value, .5f)
            org.junit.Assert.assertEquals(shelf.top.value, header.top.value, .5f)
            capture("main-header-$index")
        }
        compose.runOnIdle { screen = 2 }
        val filter = compose.onNodeWithTag("toggle_filter_sheet_btn").assertIsDisplayed().getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals(48f, (filter.bottom - filter.top).value, .5f)
        compose.onNodeWithText("札记").performClick().assertIsSelected()
    }

    @Test fun compactHeaderPreservesExpandedTouchTargets() {
        var clicked = false
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                  com.asterreader.core.designsystem.AsterHeaderRow(Modifier.padding(top = 16.dp)) {
                    androidx.compose.material3.IconButton(onClick = { clicked = true },
                        modifier = Modifier.size(width = com.asterreader.core.designsystem.AsterLayout.TouchTarget,
                            height = com.asterreader.core.designsystem.AsterLayout.HeaderHeight).testTag("compact_header_action")) {
                        androidx.compose.material3.Text("返回")
                    }
                  }
                }
            }
        }
        compose.onNodeWithTag("compact_header_action").assertHeightIsEqualTo(40.dp)
            .assertTouchHeightIsEqualTo(48.dp).performTouchInput {
                click(androidx.compose.ui.geometry.Offset(center.x, -2.dp.toPx()))
            }
        compose.runOnIdle { org.junit.Assert.assertTrue(clicked) }
    }

    @Test fun identityPromptOpensFullyAndUsesCompactEqualActions() {
        var dismissed by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme {
                if (!dismissed) com.example.ui.screens.SystemSecurityAuthPromptSheet(model, { dismissed = true })
            }
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithText("验证身份").assertIsDisplayed()
        compose.onNodeWithText("取消").assertIsDisplayed()
        compose.onNodeWithText("确认").assertIsDisplayed().assertIsNotEnabled()
        val panel = compose.onNodeWithTag("identity_auth_panel").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue((panel.bottom - panel.top).value < 440f)
        repeat(3) { compose.onNodeWithText("1").performClick() }
        compose.onNodeWithText("确认").assertIsNotEnabled()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("确认").assertIsEnabled()
        compose.onNodeWithContentDescription("已输入 4 位 PIN").assertIsDisplayed()
        repeat(5) { compose.onNodeWithText("3").performClick() }
        compose.onNodeWithContentDescription("已输入 8 位 PIN").assertIsDisplayed()
        compose.onNodeWithContentDescription("删除一位").performClick()
        compose.onNodeWithContentDescription("已输入 7 位 PIN").assertIsDisplayed()
        val cancel = compose.onNodeWithText("取消").getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithText("确认").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals((cancel.right - cancel.left).value, (confirm.right - confirm.left).value, .5f)
        captureDialog("identity-auth-compact")
        compose.onNodeWithText("确认").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("PIN 码不正确").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("PIN 码不正确").assertIsDisplayed()
        captureDialog("identity-auth-error")
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("验证身份").assertDoesNotExist()
    }

    @Test fun readerDefaultsDoNotOverwriteTheBookOverride() {
        readerContent()
        compose.onNodeWithText("LTR").performScrollTo().performClick()
        compose.onNodeWithText("默认", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("双页").performScrollTo().performClick()
        compose.runOnIdle { check(model.uiState.value.readerLayoutMode == com.example.ui.ReaderLayoutMode.DUAL_PAGE) }
        compose.onNodeWithText("本书").performScrollTo().performClick()
        compose.onNodeWithText("LTR").assertIsSelected()
        capture("reader-book-scope")
    }

    @Test fun aboutShowsOnlyVersionUpdateAndAcknowledgements() {
        content(section = "about")
        compose.onNodeWithText("Aster Reader").assertIsDisplayed()
        compose.onNodeWithText("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})").assertIsDisplayed()
        capture("about-light")
        compose.onNodeWithText("格式").assertDoesNotExist()
        compose.onNodeWithText("隐私").assertDoesNotExist()
        compose.onNodeWithText("构建").assertDoesNotExist()
        compose.onNodeWithText("检查更新").assertIsDisplayed()
        compose.onNodeWithText("GitHub").assertIsDisplayed()
        compose.onNodeWithText("致谢").performScrollTo().performClick()
        compose.runOnIdle { check(model.uiState.value.activeSubScreen == com.example.ui.SubScreen.SettingsSection("licenses")) }
    }

    @Test fun aboutLicensesRemainReachableInDarkLargeFont() {
        content(section = "about", dark = true, fontScale = 2f)
        compose.onNodeWithText("检查更新").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("致谢").performScrollTo().assertIsDisplayed()
        capture("about-dark-font-200")
    }

    @Test fun generatedLicensesOpenTheirOwnDetailPage() {
        content(section = "licenses")
        compose.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            model.openSource.value.entries.isNotEmpty() || model.openSource.value.error != null
        }
        compose.runOnIdle { check(model.openSource.value.error == null) { model.openSource.value.toString() } }
        val entry = model.openSource.value.entries.first()
        compose.onNodeWithText(entry.name).performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { check(model.uiState.value.activeSubScreen == com.example.ui.SubScreen.SettingsSection("license:${entry.id}")) }
        capture("open-source-list")
    }

    private fun readerContent(fontScale: Float = 1f, dark: Boolean = false) {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val density = LocalDensity.current.density
            val state by model.uiState.collectAsState()
            var mode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.example.ui.ReaderLayoutMode.RTL_PAGER) }
            var fit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("适合屏幕") }
            var cover by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                AsterReaderTheme(darkTheme = dark) {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                        com.example.ui.components.ReaderSettingsContent(state, model, mode, fit, cover, true,
                            { mode = it }, { fit = it }, { cover = it }, {},
                            { mode = state.readerLayoutMode; fit = state.fitModeLabel; cover = state.smartSpreadStitch }, true, {})
                    }
                }
            }
        }
    }

    private fun captureDialog(name: String) {
        compose.runOnIdle {
            rootView = android.view.inspector.WindowInspector.getGlobalWindowViews()
                .lastOrNull { it.isShown && it.width > 0 && it.height > 0 } ?: rootView
        }
        capture(name)
    }

    private fun content(section: String? = null, dark: Boolean = false, fontScale: Float = 1f) {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                AsterReaderTheme(darkTheme = dark) {
                    SettingsScreen(AsterUiState(settingsLoaded = true), emptyList(), 0, model, {}, {}, section)
                }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File("build/ui-review").apply { mkdirs() }
        compose.runOnIdle {
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            rootView.draw(android.graphics.Canvas(bitmap))
            val suffix = if (android.os.Build.VERSION.SDK_INT == 35) "" else "-api${android.os.Build.VERSION.SDK_INT}"
            File(directory, "$name$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
