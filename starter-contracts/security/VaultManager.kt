package com.asterreader.core.security

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
