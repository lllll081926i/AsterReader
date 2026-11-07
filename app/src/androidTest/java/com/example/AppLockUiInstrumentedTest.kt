package com.example

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 真机冒烟：验证应用能启动、Compose 树能渲染。
 * 更细的 PIN 键盘 / 生物识别交互需要在开启了应用锁的设备上手工回归。
 */
@RunWith(AndroidJUnit4::class)
class AppLockUiInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesAndRendersComposeTree() {
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
