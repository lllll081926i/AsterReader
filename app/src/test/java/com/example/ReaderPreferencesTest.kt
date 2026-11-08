package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ReaderPreferences
import com.example.ui.AsterUiState
import com.example.ui.ReaderLayoutMode
import com.example.ui.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 35])
class ReaderPreferencesTest {
    @Test
    fun persistedAppLockStartsLockedWithoutPersistingAuthorization() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val preferences = ReaderPreferences(app)
        preferences.save(AsterUiState(isAppLockEnabled = true, isVaultUnlocked = true,
            isAppLockedNow = false, themeMode = ThemeMode.DARK, reduceMotion = true,
            showStatsTab = false, preloadBefore = 16, preloadAfter = 0,
            readerLayoutMode = ReaderLayoutMode.RTL_PAGER, smartSpreadStitch = false, fitModeLabel = "原始比例"))
        val restored = ReaderPreferences(app).load()
        assertEquals(16, restored.preloadBefore)
        assertEquals(0, restored.preloadAfter)
        assertTrue(restored.settingsLoaded)
        assertTrue(restored.isAppLockedNow)
        assertFalse(restored.isVaultUnlocked)
        assertEquals(ThemeMode.DARK, restored.themeMode)
        assertEquals(ReaderLayoutMode.RTL_PAGER, restored.readerLayoutMode)
        assertTrue(restored.reduceMotion)
        assertFalse(restored.showStatsTab)
        assertFalse(restored.smartSpreadStitch)
        assertEquals("原始比例", restored.fitModeLabel)
    }
}
