package com.example.ui.theme

import android.view.Window
import androidx.core.view.WindowCompat

/** 应用主题可独立于系统主题，系统栏图标必须跟随实际页面背景。 */
internal fun applySystemBarAppearance(window: Window, darkTheme: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
    }
}
