package com.asterreader.core.security

import java.io.Closeable

/**
 * Random-access plaintext view over protected content.
 * Implementations must decrypt on demand and must not expose a persistent plaintext path.
 */
interface SecureDocumentSource : Closeable {
    val logicalSize: Long

    suspend fun read(offset: Long, byteCount: Int): ByteArray
}

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

@JvmInline
value class VaultItemId(val value: String)

data class VaultImportRequest(
    val deleteSourceAfterVerifiedCommit: Boolean = false,
    val protectionPolicy: ProtectionPolicy,
)

data class VaultItem(
    val id: VaultItemId,
    val encryptedSize: Long,
    val logicalSize: Long,
)

interface VaultManager {
    suspend fun encryptAndImport(
        source: SecureImportSource,
        request: VaultImportRequest,
    ): VaultItem

    suspend fun open(itemId: VaultItemId): SecureDocumentSource

    suspend fun verify(itemId: VaultItemId): VaultVerifyResult
}

interface SecureImportSource {
    val displayName: String
    val size: Long?
    suspend fun readNext(maxBytes: Int): ByteArray?
}

sealed interface VaultVerifyResult {
    data object Valid : VaultVerifyResult
    data class Corrupt(val reason: String) : VaultVerifyResult
}
