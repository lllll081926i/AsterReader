package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.ReaderLayoutMode
import com.example.ui.components.PublicationPageCanvas
import com.example.ui.components.ReaderZoomCommand
import com.example.ui.theme.AsterReaderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderZoomTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pagerZoomUpdatesViewportWithoutRepeatingPageChanges() = verifyZoomReports(ReaderLayoutMode.LTR_PAGER)

    @Test fun verticalZoomUpdatesViewportWithoutRepeatingPageChanges() = verifyZoomReports(ReaderLayoutMode.VERTICAL_SCROLL)

    private fun verifyZoomReports(mode: ReaderLayoutMode) {
        var pageChanges = 0
        var reportedScale = 1f
        var reportedPanX = 0f
        val bitmap = Bitmap.createBitmap(20, 60, Bitmap.Config.ARGB_8888)
        compose.setContent {
            AsterReaderTheme {
                PublicationPageCanvas("持续视口-$mode", 1, 2, mode, false,
                    cachedPage = { _, _ -> bitmap }, loadPage = { _, _ -> bitmap },
                    onPageChanged = { pageChanges++ },
                    onViewportChanged = { _, _, scale, x, _ -> reportedScale = scale; reportedPanX = x },
                    onToggleChrome = {})
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { pageChanges = 0 }
        val canvas = compose.onNodeWithTag("publication_canvas")
        canvas.performTouchInput { doubleClick(androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.2f)) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.runOnIdle {
            assertEquals(2.2f, reportedScale, 0.01f)
            org.junit.Assert.assertTrue("触点缩放后的平移需要保存", reportedPanX < 0f)
            assertEquals("缩放不应重复发送换页通知", 0, pageChanges)
        }
        canvas.performTouchInput { swipeLeft() }
        compose.runOnIdle {
            assertEquals(2.2f, reportedScale, 0.01f)
            org.junit.Assert.assertTrue(reportedPanX <= -0.9f)
            assertEquals(0, pageChanges)
        }
    }

    @Test fun explicitZoomAffectsCurrentPageAndResetClearsPan() {
        var reportedScale = 1f
        var reportedX = 0f
        val bitmap = Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888)
        compose.setContent {
            var page by remember { mutableIntStateOf(1) }
            var command by remember { mutableStateOf(ReaderZoomCommand()) }
            AsterReaderTheme {
                CompositionLocalProvider(com.asterreader.core.designsystem.LocalAsterReduceMotion provides true) {
                Column {
                    TextButton(onClick = { command = ReaderZoomCommand(command.sequence + 1, 0.5f) }) { Text("放大") }
                    TextButton(onClick = { command = ReaderZoomCommand(command.sequence + 1, 0f) }) { Text("重置") }
                    TextButton(onClick = { page = 2 }) { Text("跳到第二页") }
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        PublicationPageCanvas(
                            publicationId = "缩放回归", page = page, totalPages = 2,
                            mode = ReaderLayoutMode.LTR_PAGER, expanded = false,
                            zoomCommand = command, loadPage = { _, _ -> bitmap },
                            onPageChanged = { page = it },
                            onViewportChanged = { _, _, scale, x, _ -> reportedScale = scale; reportedX = x },
                            onToggleChrome = {}
                        )
                    }
                }
                }
            }
        }
        compose.onNodeWithText("放大").performClick()
        compose.runOnIdle { assertEquals(1.5f, reportedScale, 0.01f) }
        compose.onNodeWithText("重置").performClick()
        compose.runOnIdle { assertEquals(1f, reportedScale, 0.01f); assertEquals(0f, reportedX, 0.01f) }
        compose.onNodeWithText("放大").performClick()
        compose.onNodeWithText("跳到第二页").performClick()
        compose.runOnIdle { assertEquals(1f, reportedScale, 0.01f) }
    }
}
