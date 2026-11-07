package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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
        compose.onNodeWithText("外观与主题").assertIsDisplayed()
        compose.onNodeWithText("应用锁与访问控制").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("关于").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("关于").performClick()
        compose.runOnIdle { check(model.uiState.value.activeSubScreen == com.example.ui.SubScreen.SettingsSection("about")) }
        capture("settings-home-light")
    }

    @Test fun readingOptionsRemainReachableAtDoubleFontScale() {
        content(section = "reading", fontScale = 2f)
        compose.onNodeWithText("音量键翻页").performScrollTo().assertIsDisplayed().performClick()
        capture("reading-font-200")
    }

    @Test fun appearanceUsesDarkTheme() {
        content(section = "appearance", dark = true)
        compose.onNodeWithText("深色模式").assertIsDisplayed()
        compose.onNodeWithText("减少动效").assertIsDisplayed()
        capture("appearance-dark")
    }

    @Test fun privacyUsesTheSamePageStructure() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme { PrivacySecuritySettingsScreen(AsterUiState(settingsLoaded = true), model, {}) }
        }
        compose.onNodeWithText("应用 PIN").assertIsDisplayed()
        compose.onNodeWithText("隐藏最近任务预览").performScrollTo().assertIsDisplayed()
        capture("privacy-light")
    }

    @Test fun settingsRowsHaveTheSameCompactHeight() {
        content()
        val first = compose.onNodeWithText("外观与主题").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("默认阅读配置").getUnclippedBoundsInRoot()
        org.junit.Assert.assertEquals(52f, (first.bottom - first.top).value, 0.5f)
        org.junit.Assert.assertEquals((first.bottom - first.top).value, (second.bottom - second.top).value, 0.5f)
    }

    @Test @Config(qualifiers = "w891dp-h411dp-xhdpi")
    fun landscapeReadingOptionsRemainReachable() {
        content(section = "reading", fontScale = 2f)
        compose.onNodeWithText("封面单独显示").performScrollTo().assertIsDisplayed()
        capture("reading-landscape-font-200")
    }

    @Test fun emptyShelfManagementHasNoPlaceholderActions() {
        content(section = "shelves")
        compose.onNodeWithText("新建书架").assertIsDisplayed()
        capture("shelf-management-empty")
    }

    @Test fun libraryUsesCompactBottomNavigation() {
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            AsterReaderTheme { com.example.AsterReaderAppRoot(model, AsterUiState(settingsLoaded = true, isAppLockedNow = false)) }
        }
        capture("library-empty")
        compose.onNodeWithTag("nav_dock_library").assertIsDisplayed()
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
