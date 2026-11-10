package com.example

import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import com.example.ui.ReaderLayoutMode
import com.example.ui.components.PublicationPageCanvas
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class ComicPageCanvasTest {
    @get:Rule val compose = createComposeRule()

    @Test fun evictedPageKeepsKnownHeightWhileItsBitmapLoadsAgain() {
        val bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("尺寸复用", 1, 2, ReaderLayoutMode.VERTICAL_SCROLL, false,
                cachedPageSize = { 400 to 100 },
                loadPage = { page, _ -> if (page == 1) awaitCancellation() else bitmap },
                onPageChanged = {}, onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = {})
        }
        val canvas = compose.onNodeWithTag("publication_canvas").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithContentDescription("第 2 页").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals("第一页加载期间仍使用原始高度", canvas.width / 4f, second.top - canvas.top, 1f)
    }

    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun prefetchedShortPageDisplaysWithoutAnAsyncLoadOrPlaceholderHeight() {
        val bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        var loads = 0
        lateinit var rootView: android.view.View
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            PublicationPageCanvas("预载首帧", 1, 1, ReaderLayoutMode.VERTICAL_SCROLL, false,
                cachedPage = { _, _ -> bitmap },
                loadPage = { _, _ -> loads++; awaitCancellation() },
                onPageChanged = {}, onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = {})
        }
        val bounds = compose.onNodeWithContentDescription("第 1 页").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(bounds.width / 4f, bounds.height, 1f)
        compose.runOnIdle { assertEquals("命中内存缓存不得重新查询或等待解码", 0, loads) }
        compose.runOnIdle {
            val image = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            rootView.draw(android.graphics.Canvas(image))
            val directory = java.io.File("build/ui-review").apply { mkdirs() }
            java.io.File(directory, "cached-short-page-api${android.os.Build.VERSION.SDK_INT}.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
        }
    }

    @Test fun scrollingWithinOnePageUpdatesViewportWithoutRepeatingPageChange() {
        val bitmap = Bitmap.createBitmap(100, 2000, Bitmap.Config.ARGB_8888)
        var changes = 0
        var moved = false
        compose.setContent {
            PublicationPageCanvas("页内滚动", 1, 2, ReaderLayoutMode.VERTICAL_SCROLL, false,
                cachedPage = { _, _ -> bitmap }, loadPage = { _, _ -> bitmap },
                onPageChanged = { changes++ },
                onViewportChanged = { _, fraction, _, _, _ -> if (fraction > 0f) moved = true }, onToggleChrome = {})
        }
        compose.waitForIdle()
        compose.runOnIdle { changes = 0 }
        compose.onNodeWithTag("publication_canvas").performTouchInput { swipeUp() }
        compose.runOnIdle {
            org.junit.Assert.assertTrue("仍需保存页内滚动位置", moved)
            assertEquals("同页滚动不应反复通知换页", 0, changes)
        }
    }

    @Test fun cachedPageRestoresItsOffsetWithoutWaitingForAnotherLayoutChange() {
        val bitmap = Bitmap.createBitmap(100, 1000, Bitmap.Config.ARGB_8888)
        var restoredFraction = 0f
        compose.setContent {
            PublicationPageCanvas("缓存位置恢复", 1, 2, ReaderLayoutMode.VERTICAL_SCROLL, false,
                initialOffsetFraction = 0.25f,
                cachedPage = { _, _ -> bitmap }, loadPage = { _, _ -> bitmap },
                onPageChanged = {},
                onViewportChanged = { _, fraction, _, _, _ -> restoredFraction = fraction }, onToggleChrome = {})
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0.25f, restoredFraction, 0.01f) }
    }

    @Test fun openingPageRequestsNormalResolutionOnly() {
        val requests = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val bitmap = Bitmap.createBitmap(4, 6, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("正常清晰度", 1, 1, ReaderLayoutMode.LTR_PAGER, false,
                loadPage = { _, pixels -> requests.add(pixels); bitmap },
                onPageChanged = {}, onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = {})
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("第 1 页").assertExists()
        compose.runOnIdle { assertEquals(listOf(2_500_000), requests.toList()) }
    }

    @Test fun centralTapsRespondOnReleaseAndNeverScheduleAnotherToggle() {
        var toggles = 0
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("点击", 1, 1, ReaderLayoutMode.LTR_PAGER, false,
                loadPage = { _, _ -> bitmap }, onPageChanged = {},
                onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = { toggles++ })
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, toggles) }
        canvas.performTouchInput { advanceEventTime(60); click(center) }
        compose.runOnIdle { assertEquals(2, toggles) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(2, toggles) }
    }

    @Test fun loadingAndShortPageBlankSpaceStillAcceptCentralTaps() {
        var toggles = 0
        val loading = mutableStateOf(true)
        val bitmap = Bitmap.createBitmap(4, 1, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas(if (loading.value) "加载" else "短图", 1, 1, ReaderLayoutMode.VERTICAL_SCROLL, false,
                loadPage = { _, _ -> if (loading.value) awaitCancellation() else bitmap },
                onPageChanged = {}, onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = { toggles++ })
        }
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, toggles); loading.value = false }
        compose.waitForIdle()
        canvas.performTouchInput { click(Offset(centerX, height * 0.9f)) }
        compose.runOnIdle { assertEquals(2, toggles) }
    }

    @Test fun scrollingPinchingAndCancelledTouchesDoNotToggleControls() {
        var toggles = 0
        val bitmap = Bitmap.createBitmap(4, 12, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("手势", 1, 10, ReaderLayoutMode.VERTICAL_SCROLL, false,
                loadPage = { _, _ -> bitmap }, onPageChanged = {},
                onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = { toggles++ })
        }
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals("滚动", 0, toggles) }
        canvas.performTouchInput {
            down(0, Offset(centerX - 20f, centerY))
            down(1, Offset(centerX + 20f, centerY))
            moveTo(0, Offset(centerX - 80f, centerY))
            moveTo(1, Offset(centerX + 80f, centerY))
            up(0); up(1)
        }
        compose.runOnIdle { assertEquals("双指", 0, toggles) }
        canvas.performTouchInput { down(center); cancel() }
        compose.runOnIdle { assertEquals("取消", 0, toggles) }
        canvas.performTouchInput { longClick(center) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test fun sideDoubleTapZoomsWithoutTurningPageOrTogglingControls() {
        var toggles = 0
        var scale = 1f
        val page = mutableIntStateOf(2)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("双击", page.intValue, 5, ReaderLayoutMode.LTR_PAGER, false,
                loadPage = { _, _ -> bitmap }, onPageChanged = { page.intValue = it },
                onViewportChanged = { _, _, zoom, _, _ -> scale = zoom }, onToggleChrome = { toggles++ })
        }
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { doubleClick(Offset(width * 0.85f, centerY)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(2.2f, scale, 0.01f); assertEquals(2, page.intValue); assertEquals(0, toggles) }
        canvas.performTouchInput { doubleClick(Offset(width * 0.85f, centerY)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(1f, scale, 0.01f); assertEquals(2, page.intValue) }
    }

    @Test fun rtlOuterLeftTapAdvancesAndOuterRightTapReturns() {
        val page = mutableIntStateOf(2)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("RTL", page.intValue, 5, ReaderLayoutMode.RTL_PAGER, false,
                loadPage = { _, _ -> bitmap }, onPageChanged = { page.intValue = it },
                onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = {})
        }
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { click(Offset(width * 0.1f, centerY)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(3, page.intValue) }
        canvas.performTouchInput { click(Offset(width * 0.9f, centerY)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(2, page.intValue) }
    }

    @Test fun spreadTapZonesUseTheWholeCanvasWidth() {
        val page = mutableIntStateOf(3)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("双页热区", page.intValue, 8, ReaderLayoutMode.DUAL_PAGE, true,
                coverSingle = false, loadPage = { _, _ -> bitmap }, onPageChanged = { page.intValue = it },
                onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = {})
        }
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { click(Offset(width * 0.8f, centerY)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(5, page.intValue) }
        canvas.performTouchInput { click(Offset(width * 0.2f, centerY)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle { assertEquals(3, page.intValue) }
    }

    @Test fun lastSpreadReportsCompletionEvenWhenItsFirstPageIsNotTheLastPage() {
        var completed = false
        var reported = 0
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("末页双页", 3, 4, ReaderLayoutMode.DUAL_PAGE, true,
                coverSingle = false, loadPage = { _, _ -> bitmap }, onPageChanged = { reported = it },
                onViewportChanged = { _, _, _, _, _ -> }, onReachedEnd = { completed = it }, onToggleChrome = {})
        }
        compose.waitForIdle()
        compose.runOnIdle { org.junit.Assert.assertEquals(3, reported); org.junit.Assert.assertTrue(completed) }
    }

    @Test fun verticalReadingReportsCompletionWhenTheLastShortPageIsFullyVisible() {
        var completed = false
        val bitmap = Bitmap.createBitmap(4, 2, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("末页滚动", 6, 6, ReaderLayoutMode.VERTICAL_SCROLL, false,
                loadPage = { _, _ -> bitmap }, onPageChanged = {}, onViewportChanged = { _, _, _, _, _ -> },
                onReachedEnd = { completed = it }, onToggleChrome = {})
        }
        compose.waitForIdle()
        compose.runOnIdle { org.junit.Assert.assertTrue(completed) }
        val bounds = compose.onNodeWithContentDescription("第 6 页").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(bounds.width / 2f, bounds.height, 1f)
    }

    @Test
    fun seekingAndChangingSpreadModeKeepRequestedComicPageVisible() {
        val page = mutableIntStateOf(1)
        val mode = mutableStateOf(ReaderLayoutMode.LTR_PAGER)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("漫画", page.intValue, 20, mode.value, true,
                loadPage = { _, _ -> bitmap },
                onPageChanged = { page.intValue = it },
                onViewportChanged = { _, _, _, _, _ -> },
                onToggleChrome = {})
        }
        compose.runOnIdle { page.intValue = 7 }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("第 7 页").assertExists()
        compose.runOnIdle { mode.value = ReaderLayoutMode.DUAL_PAGE }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("第 7 页").assertExists()
        compose.runOnIdle { mode.value = ReaderLayoutMode.VERTICAL_SCROLL; page.intValue = 15 }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("第 15 页").assertExists()
    }
}
