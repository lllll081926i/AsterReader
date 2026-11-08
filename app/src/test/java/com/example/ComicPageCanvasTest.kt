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

    @Test fun pdfPreviewStaysVisibleWhileFullPageLoadsAndAfterItsFailure() {
        val complete = kotlinx.coroutines.CompletableDeferred<Unit>()
        val preview = Bitmap.createBitmap(4, 6, Bitmap.Config.ARGB_8888)
        val fullRequested = java.util.concurrent.atomic.AtomicBoolean()
        compose.setContent {
            PublicationPageCanvas("PDF预览", 1, 1, ReaderLayoutMode.LTR_PAGER, false,
                previewPixels = 600_000,
                loadPage = { _, pixels ->
                    if (pixels == 600_000) preview else {
                        fullRequested.set(true)
                        complete.await()
                        error("高清读取失败")
                    }
                }, onPageChanged = {}, onViewportChanged = { _, _, _, _, _ -> }, onToggleChrome = {})
        }
        compose.waitUntil { fullRequested.get() }
        compose.onNodeWithContentDescription("第 1 页").assertExists()
        compose.runOnIdle { complete.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("第 1 页").assertExists()
        compose.onNodeWithText("第 1 页无法读取，请检查文件").assertDoesNotExist()
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
