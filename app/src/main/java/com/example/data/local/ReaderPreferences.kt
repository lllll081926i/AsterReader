package com.example.data.local

import android.content.Context
import com.example.ui.AsterUiState
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ShelvesViewMode
import com.example.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 应用锁 PIN 凭据。
 *
 * - 新格式 v2：每次设置生成随机 16 字节盐 + PBKDF2-HMAC-SHA256（6 万次迭代），
 *   4~8 位数字 PIN 也不至于被暴力枚举；绝不保存明文。
 * - 兼容旧版：静态盐 SHA-256 的 64 位十六进制摘要仍可校验，首次改 PIN 时自动升级。
 */
object PinCredentials {
    private const val LEGACY_SALT = "aster-reader::pin::"
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        return "v2:${hex(salt)}:${hex(derive(pin, salt))}"
    }

    fun verify(pin: String, stored: String): Boolean {
        if (stored.isBlank()) return false
        val parts = stored.split(':')
        if (parts.size == 3 && parts[0] == "v2") {
            val salt = runCatching { unhex(parts[1]) }.getOrNull() ?: return false
            val expected = runCatching { unhex(parts[2]) }.getOrNull() ?: return false
            return MessageDigest.isEqual(expected, derive(pin, salt))
        }
        // 旧格式：直接比较静态盐摘要，仍用常量时间比较
        val expected = runCatching { unhex(stored) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, sha256(LEGACY_SALT + pin.trim()))
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.trim().toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun sha256(text: String): ByteArray = sha256(text.toByteArray())

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(bytes)

    private fun hex(bytes: ByteArray): String = buildString(bytes.size * 2) {
        bytes.forEach { append("%02x".format(it.toInt() and 0xff)) }
    }

    private fun unhex(text: String): ByteArray {
        require(text.length % 2 == 0)
        return ByteArray(text.length / 2) { i ->
            text.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}

class ReaderPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)

    suspend fun load(): AsterUiState = withContext(Dispatchers.IO) {
        val appLock = preferences.getBoolean("appLock", false)
        val legacyPin = preferences.getString("customPin", "") ?: ""
        val storedHash = preferences.getString("pinHash", "") ?: ""
        val pinHash = when {
            storedHash.isNotBlank() -> storedHash
            legacyPin.isNotBlank() -> PinCredentials.hash(legacyPin)
            else -> ""
        }
        if (legacyPin.isNotBlank()) {
            preferences.edit().remove("customPin").putString("pinHash", pinHash).apply()
        }
        AsterUiState(
            isAppLockEnabled = appLock,
            isAppLockedNow = appLock,
            pinHash = pinHash,
            biometricUnlockEnabled = preferences.getBoolean("biometricUnlock", true),
            settingsLoaded = true,
            themeMode = enumValue(preferences.getString("theme", null), ThemeMode.SYSTEM),
            reduceMotion = preferences.getBoolean("reduceMotion", false),
            libraryLayoutMode = enumValue(preferences.getString("libraryLayout", null), LibraryLayoutMode.GRID_2),
            shelvesViewMode = enumValue(preferences.getString("shelvesLayout", null), ShelvesViewMode.COLLAGE_GRID),
            showRecentReading = preferences.getBoolean("recentReading", true),
            showReadingStats = preferences.getBoolean("readingStats", true),
            readerLayoutMode = enumValue(preferences.getString("readerLayout", null), ReaderLayoutMode.VERTICAL_SCROLL),
            autoLockPolicyLabel = preferences.getString("autoLock", "立即锁定") ?: "立即锁定",
            lockOnScreenOff = preferences.getBoolean("screenOff", true),
            flagSecureEnabled = preferences.getBoolean("secure", true),
            hideRecentsPreview = preferences.getBoolean("recents", true),
            volumeKeyPageTurn = preferences.getBoolean("volumeKeys", true),
            smartSpreadStitch = preferences.getBoolean("coverSingle", true),
            fitModeLabel = preferences.getString("fitMode", "适合屏幕").takeIf { it in setOf("适合屏幕", "原始比例") } ?: "适合屏幕",
            readerBrightness = preferences.getFloat("brightness", 0.95f).coerceIn(0.2f, 1f),
            recentSearches = preferences.getString("recentSearches", "")
                .orEmpty().split('\n').filter { it.isNotBlank() }.take(8)
        )
    }

    suspend fun save(state: AsterUiState) = withContext(Dispatchers.IO) {
        check(preferences.edit()
            .putBoolean("appLock", state.isAppLockEnabled)
            .putString("pinHash", state.pinHash)
            .putBoolean("biometricUnlock", state.biometricUnlockEnabled)
            .putString("theme", state.themeMode.name)
            .putBoolean("reduceMotion", state.reduceMotion)
            .putString("libraryLayout", state.libraryLayoutMode.name)
            .putString("shelvesLayout", state.shelvesViewMode.name)
            .putBoolean("recentReading", state.showRecentReading)
            .putBoolean("readingStats", state.showReadingStats)
            .putString("readerLayout", state.readerLayoutMode.name)
            .putString("autoLock", state.autoLockPolicyLabel)
            .putBoolean("screenOff", state.lockOnScreenOff)
            .putBoolean("secure", state.flagSecureEnabled)
            .putBoolean("recents", state.hideRecentsPreview)
            .putBoolean("volumeKeys", state.volumeKeyPageTurn)
            .putBoolean("coverSingle", state.smartSpreadStitch)
            .putString("fitMode", state.fitModeLabel)
            .putFloat("brightness", state.readerBrightness)
            .putString("recentSearches", state.recentSearches.joinToString("\n"))
            .commit()) { "无法保存阅读设置" }
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    // ------------------------------------------------------------------
    // 导航会话：进程被回收后回到上次的页面，而不是退回首页
    // ------------------------------------------------------------------

    /**
     * 导航会话：进程被回收后回到离开时的页面，而不是退回首页。
     * 子页面栈由 ViewModel 编码成字符串，这里只负责持久化，避免数据层依赖 UI 类型。
     */
    data class NavSession(
        val tab: String,
        val stackEncoding: String,
        val searchQuery: String,
        val legacyBookId: String?,
        val savedAt: Long
    )

    suspend fun saveSession(tab: String, stackEncoding: String, searchQuery: String) = withContext(Dispatchers.IO) {
        preferences.edit()
            .putString("sessionTab", tab)
            .putString("sessionStack", stackEncoding)
            .putString("sessionQuery", searchQuery)
            .remove("sessionBook")
            .putLong("sessionAt", System.currentTimeMillis())
            .apply()
    }

    suspend fun loadSession(): NavSession? = withContext(Dispatchers.IO) {
        val tab = preferences.getString("sessionTab", null) ?: return@withContext null
        NavSession(
            tab = tab,
            stackEncoding = preferences.getString("sessionStack", "").orEmpty(),
            searchQuery = preferences.getString("sessionQuery", "").orEmpty(),
            legacyBookId = preferences.getString("sessionBook", null),
            savedAt = preferences.getLong("sessionAt", 0L)
        )
    }
}
