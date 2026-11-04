package com.asterreader.core.security

enum class ProtectionMode {
    NONE,
    ACCESS_LOCK,
    VAULT_ENCRYPTED,
}

enum class UnlockMethod {
    SYSTEM_AUTH,
    APP_PIN,
    APP_PASSWORD,
    EITHER_SYSTEM_OR_APP,
    BOTH,
}

data class ProtectionPolicy(
    val mode: ProtectionMode,
    val unlockMethod: UnlockMethod,
    val hideMetadataWhileLocked: Boolean = true,
    val blockScreenshots: Boolean = true,
)
