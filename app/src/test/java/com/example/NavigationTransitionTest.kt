package com.example

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.example.ui.SubScreen
import com.example.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35], qualifiers = "w411dp-h891dp-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class NavigationTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ordinaryTransitionsDisposePreviousPageAndBackPopsOnce() {
        val home = NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1)
        val detail = NavigationFrame(SubScreen.SettingsSection("appearance"), MainNavDestination.LIBRARY, 2)
        val current = mutableStateOf(home)
        lateinit var dispatcher: OnBackPressedDispatcher
        var disposed = 0
        var backs = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                AsterNavigation(current.value, true, { backs++; current.value = home }) { frame ->
                    DisposableEffect(frame) { onDispose { disposed++ } }
                    Text(if (frame == home) "设置页面" else "外观页面")
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { current.value = detail }
        compose.waitForIdle()
        compose.onNodeWithText("设置页面").assertDoesNotExist()
        compose.onNodeWithText("外观页面").assertExists()
        compose.runOnIdle { assertEquals(1, disposed); dispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText("外观页面").assertDoesNotExist()
        compose.onNodeWithText("设置页面").assertExists()
        compose.runOnIdle { assertEquals(1, backs); assertEquals(2, disposed) }
    }

    @Test fun reducedMotionStillSwitchesToExactlyOnePage() {
        val first = NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1)
        val second = NavigationFrame(SubScreen.Reader("测试"), MainNavDestination.LIBRARY, 2)
        val current = mutableStateOf(first)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAsterReduceMotion provides true) {
                    AsterNavigation(current.value, false, {}) { frame -> Text(if (frame == first) "设置" else "阅读") }
                }
            }
        }
        compose.runOnIdle { current.value = second }
        compose.onNodeWithText("设置").assertDoesNotExist()
        compose.onNodeWithText("阅读").assertExists()
    }
}
