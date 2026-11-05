package com.example.data.local

import android.content.Context
import com.example.ui.AsterUiState
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ShelvesViewMode
import com.example.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReaderPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)

    suspend fun load(): AsterUiState = withContext(Dispatchers.IO) {
        val appLock = preferences.getBoolean("appLock", false)
        val customPin = preferences.getString("customPin", "") ?: ""
        AsterUiState(
            isAppLockEnabled = appLock,
            isAppLockedNow = appLock,
            customPin = customPin,
            settingsLoaded = true,
            themeMode = enumValue(preferences.getString("theme", null), ThemeMode.SYSTEM),
            useDynamicColor = true,
            reduceMotion = preferences.getBoolean("reduceMotion", false),
            libraryLayoutMode = enumValue(preferences.getString("libraryLayout", null), LibraryLayoutMode.GRID_2),
            shelvesViewMode = enumValue(preferences.getString("shelvesLayout", null), ShelvesViewMode.COLLAGE_GRID),
            showRecentReading = preferences.getBoolean("recentReading", true),
            readerLayoutMode = enumValue(preferences.getString("readerLayout", null), ReaderLayoutMode.VERTICAL_SCROLL),
            autoLockPolicyLabel = preferences.getString("autoLock", "立即锁定") ?: "立即锁定",
            lockOnScreenOff = preferences.getBoolean("screenOff", true),
            flagSecureEnabled = preferences.getBoolean("secure", true),
            hideRecentsPreview = preferences.getBoolean("recents", true),
            volumeKeyPageTurn = preferences.getBoolean("volumeKeys", true),
            smartSpreadStitch = preferences.getBoolean("coverSingle", true),
            fitModeLabel = preferences.getString("fitMode", "适合屏幕").takeIf { it in setOf("适合屏幕", "原始比例") } ?: "适合屏幕"
        )
    }

    suspend fun save(state: AsterUiState) = withContext(Dispatchers.IO) {
        check(preferences.edit()
            .putBoolean("appLock", state.isAppLockEnabled)
            .putString("customPin", state.customPin)
            .putString("theme", state.themeMode.name)
            .putBoolean("reduceMotion", state.reduceMotion)
            .putString("libraryLayout", state.libraryLayoutMode.name)
            .putString("shelvesLayout", state.shelvesViewMode.name)
            .putBoolean("recentReading", state.showRecentReading)
            .putString("readerLayout", state.readerLayoutMode.name)
            .putString("autoLock", state.autoLockPolicyLabel)
            .putBoolean("screenOff", state.lockOnScreenOff)
            .putBoolean("secure", state.flagSecureEnabled)
            .putBoolean("recents", state.hideRecentsPreview)
            .putBoolean("volumeKeys", state.volumeKeyPageTurn)
            .putBoolean("coverSingle", state.smartSpreadStitch)
            .putString("fitMode", state.fitModeLabel)
            .commit()) { "无法保存阅读设置" }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
