package com.example

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.ui.SubScreen
import com.example.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class PredictiveNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelledGestureKeepsRouteAndCompletedGesturePopsExactlyOnce() {
        val origin = NavigationFrame(SubScreen.SettingsSection("appearance"), MainNavDestination.LIBRARY, 2)
        val previous = NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1)
        val current = mutableStateOf(origin)
        lateinit var dispatcher: OnBackPressedDispatcher
        var commits = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                AsterPredictiveNavigation(current.value, previous, true, false, {
                    commits++
                    current.value = previous
                }) { frame -> Text(if (frame == origin) "外观页面" else "设置页面") }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 200f, 0.5f, BackEventCompat.EDGE_LEFT)) }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onNodeWithText("外观页面").assertExists()
        compose.onNodeWithText("设置页面").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, commits); dispatcher.dispatchOnBackCancelled() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("外观页面").assertExists()
        compose.onNodeWithText("设置页面").assertDoesNotExist()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(180f, 200f, 0.8f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("外观页面").assertDoesNotExist()
        compose.onNodeWithText("设置页面").assertExists()
        assertEquals(1, commits)
    }
}
