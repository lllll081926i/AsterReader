package com.example

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
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

    @Test fun backAndTabSwitchRestoreIndependentScrollPositions() {
        val library = NavigationFrame(null, MainNavDestination.LIBRARY, 0)
        val shelves = NavigationFrame(null, MainNavDestination.SHELVES, 0)
        val detail = NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1)
        val current = mutableStateOf(library)
        var visibleIndex = -1
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAsterReduceMotion provides true) {
                    AsterNavigation(current.value, true, {}) { frame ->
                        if (frame.screen == null) {
                            val list = rememberLazyListState()
                            SideEffect { visibleIndex = list.firstVisibleItemIndex }
                            LazyColumn(state = list, modifier = Modifier.testTag("列表")) {
                                items(100) { Text("书籍 $it") }
                            }
                        } else Text("详情")
                    }
                }
            }
        }
        compose.onNodeWithTag("列表").performScrollToIndex(40)
        compose.runOnIdle { current.value = detail }
        compose.onNodeWithText("详情").assertExists()
        compose.runOnIdle { current.value = library }
        compose.runOnIdle { assertEquals(40, visibleIndex); current.value = shelves }
        compose.onNodeWithTag("列表").performScrollToIndex(20)
        compose.runOnIdle { current.value = library }
        compose.runOnIdle { assertEquals(40, visibleIndex); current.value = shelves }
        compose.runOnIdle { assertEquals(20, visibleIndex) }
    }

    @Test fun cancelledPredictiveGestureKeepsPageAndCompletedGesturePopsOnce() {
        val current = mutableStateOf(NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1))
        lateinit var dispatcher: OnBackPressedDispatcher
        var backs = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                AsterNavigation(current.value, true, {
                    backs++
                    current.value = NavigationFrame(null, MainNavDestination.LIBRARY, 0)
                }) { Text(if (it.screen == null) "书库" else "设置") }
            }
        }
        compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 50f, 0f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(80f, 50f, 0.6f, BackEventCompat.EDGE_LEFT)) }
        compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        compose.onNodeWithText("设置").assertExists()
        compose.runOnIdle { assertEquals(0, backs) }
        compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(400f, 50f, 0f, BackEventCompat.EDGE_RIGHT)) }
        compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(300f, 50f, 0.8f, BackEventCompat.EDGE_RIGHT)) }
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.onNodeWithText("书库").assertExists()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test fun localBackHandlerHasPriorityOverPageNavigation() {
        lateinit var dispatcher: OnBackPressedDispatcher
        var pageBacks = 0
        var localBacks = 0
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            MaterialTheme {
                AsterNavigation(NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1), true,
                    { pageBacks++ }) {
                    BackHandler { localBacks++ }
                    Text("未保存编辑")
                }
            }
        }
        compose.runOnIdle { dispatcher.onBackPressed() }
        compose.runOnIdle { assertEquals(1, localBacks); assertEquals(0, pageBacks) }
    }

    @Test fun protectedPageDoesNotRetainSaveableDraft() {
        val home = NavigationFrame(null, MainNavDestination.LIBRARY, 0)
        val protected = NavigationFrame(SubScreen.HiddenBooks, MainNavDestination.LIBRARY, 1)
        val current = mutableStateOf(protected)
        var draft: MutableState<String>? = null
        compose.setContent {
            MaterialTheme {
                AsterNavigation(current.value, true, {}, retainState = current.value != protected) {
                    if (it == protected) {
                        val text = rememberSaveable { mutableStateOf("") }
                        SideEffect { draft = text }
                        Text(text.value)
                    } else Text("书库")
                }
            }
        }
        compose.runOnIdle { draft!!.value = "私人草稿" }
        compose.runOnIdle { current.value = home }
        compose.runOnIdle { current.value = protected }
        compose.runOnIdle { assertEquals("", draft!!.value) }
    }

    @Test fun poppedEntryDoesNotReuseDiscardedDraft() {
        val home = NavigationFrame(null, MainNavDestination.LIBRARY, 0)
        val editor = NavigationFrame(SubScreen.Settings, MainNavDestination.LIBRARY, 1)
        val current = mutableStateOf(editor)
        var draft: MutableState<String>? = null
        compose.setContent {
            MaterialTheme {
                AsterNavigation(current.value, true, {}, backStack = if (current.value == editor) listOf(home, editor) else listOf(home)) {
                    if (it == editor) {
                        val text = rememberSaveable { mutableStateOf("") }
                        SideEffect { draft = text }
                        Text(text.value)
                    } else Text("书库")
                }
            }
        }
        compose.runOnIdle { draft!!.value = "已放弃的草稿" }
        compose.runOnIdle { current.value = home }
        compose.runOnIdle { current.value = editor }
        compose.runOnIdle { assertEquals("", draft!!.value) }
    }
}
