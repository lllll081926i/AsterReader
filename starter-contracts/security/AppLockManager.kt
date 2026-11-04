package com.asterreader.core.security

import kotlinx.coroutines.flow.StateFlow

sealed interface AppLockState {
    data object Disabled : AppLockState
    data object Locked : AppLockState
    data class Unlocked(val sessionId: String) : AppLockState
}

enum class UnlockReason {
    APP_START,
    APP_RESUME,
    OPEN_PROTECTED_ITEM,
    SECURITY_SETTINGS,
    EXPORT_PROTECTED_CONTENT,
}

sealed interface UnlockResult {
    data object Success : UnlockResult
    data object Cancelled : UnlockResult
    data class Failed(val reason: String) : UnlockResult
}

interface AppLockManager {
    val state: StateFlow<AppLockState>

    suspend fun requireUnlock(reason: UnlockReason): UnlockResult

    fun onAppForegrounded()
    fun onAppBackgrounded()
    fun onScreenOff()
    fun lockNow()
}
