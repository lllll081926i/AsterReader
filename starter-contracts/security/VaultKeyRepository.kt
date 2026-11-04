package com.asterreader.core.security

/**
 * The UI and reader layers must never receive raw key material from this interface.
 * Implementations should keep Android Keystore / password key-slot details internal.
 */
interface VaultKeyRepository {
    suspend fun ensureInitialized()
    suspend fun unlockWithSystemAuth(): KeyUnlockResult
    suspend fun unlockWithAppSecret(secret: CharArray): KeyUnlockResult
    suspend fun createOrReplaceRecoveryPassword(password: CharArray): Result<Unit>
    fun lock()
}

sealed interface KeyUnlockResult {
    data object Unlocked : KeyUnlockResult
    data object Cancelled : KeyUnlockResult
    data class Failed(val reason: String) : KeyUnlockResult
}
