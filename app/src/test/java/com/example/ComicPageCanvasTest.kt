package com.example

import android.graphics.Bitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.example.ui.ReaderLayoutMode
import com.example.ui.components.PublicationPageCanvas
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComicPageCanvasTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun seekingAndChangingSpreadModeKeepRequestedComicPageVisible() {
        val page = mutableIntStateOf(1)
        val mode = mutableStateOf(ReaderLayoutMode.LTR_PAGER)
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        compose.setContent {
            PublicationPageCanvas("漫画", page.intValue, 20, mode.value, true,
                loadPage = { bitmap }, onPageChanged = { page.intValue = it }, onToggleChrome = {})
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
