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
