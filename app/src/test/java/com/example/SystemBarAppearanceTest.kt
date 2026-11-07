package com.example

import android.app.Activity
import androidx.core.view.WindowCompat
import com.example.ui.theme.applySystemBarAppearance
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemBarAppearanceTest {
    @Test fun systemBarIconsFollowAppThemeAfterRuntimeChanges() {
        Robolectric.buildActivity(Activity::class.java).setup().use { activity ->
            val window = activity.get().window
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            applySystemBarAppearance(window, darkTheme = true)
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
            applySystemBarAppearance(window, darkTheme = false)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
            applySystemBarAppearance(window, darkTheme = true)
            assertFalse(controller.isAppearanceLightStatusBars)
        }
    }

    @Test @Config(sdk = [31])
    fun minimumSupportedSystemUsesTheSameIconContrast() = systemBarIconsFollowAppThemeAfterRuntimeChanges()
}
