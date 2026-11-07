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
