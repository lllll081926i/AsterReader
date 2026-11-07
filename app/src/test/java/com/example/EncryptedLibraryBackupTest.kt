package com.example

import com.example.data.local.EncryptedLibraryBackup
import com.example.data.local.PinCredentials
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class EncryptedLibraryBackupTest {
    private val password get() = "测试备份口令-2026-long".toCharArray()

    @Test fun encryptionUsesFreshSaltAndNonceAndAuthenticatesBeforeReturning() {
        val original = "{\"title\":\"公开漫画\"}".toByteArray()
        val first = EncryptedLibraryBackup.encrypt(original, password)
        val second = EncryptedLibraryBackup.encrypt(original, password)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(original, EncryptedLibraryBackup.decrypt(ByteArrayInputStream(first), password))
        assertFalse(String(first).contains("公开漫画"))
        assertFails { EncryptedLibraryBackup.decrypt(ByteArrayInputStream(first), "错误口令".toCharArray()) }
        val corrupted = first.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertFails { EncryptedLibraryBackup.decrypt(ByteArrayInputStream(corrupted), password) }
        assertFails { EncryptedLibraryBackup.decrypt(ByteArrayInputStream(first.copyOf(first.size - 1)), password) }
        assertFails { EncryptedLibraryBackup.decrypt(ByteArrayInputStream(first + byteArrayOf(0)), password) }
        val header = first.copyOf().also { it[12] = (it[12].toInt() xor 1).toByte() }
        assertFails { EncryptedLibraryBackup.decrypt(ByteArrayInputStream(header), password) }
        val maliciousCost = first.copyOf().also { java.nio.ByteBuffer.wrap(it).putInt(8, Int.MAX_VALUE) }
        assertFails { EncryptedLibraryBackup.decrypt(ByteArrayInputStream(maliciousCost), password) }
    }

    @Test fun pinFormatPreservesLegacyVerificationAndBoundsUntrustedParameters() {
        val pin = "4321"
        val hash = PinCredentials.hash(pin)
        assertTrue(hash.startsWith("v3:pbkdf2-sha256:600000:"))
        assertTrue(PinCredentials.verify(pin, hash))
        assertFalse(PinCredentials.verify("0000", hash))
        assertFalse(PinCredentials.needsUpgrade(hash))
        val salt = ByteArray(16) { it.toByte() }
        val spec = PBEKeySpec(pin.toCharArray(), salt, 60_000, 256)
        val old = "v2:${hex(salt)}:${hex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded)}"
        spec.clearPassword()
        assertTrue(PinCredentials.verify(pin, old))
        assertTrue(PinCredentials.needsUpgrade(old))
        val legacy = hex(MessageDigest.getInstance("SHA-256").digest("aster-reader::pin::$pin".toByteArray()))
        assertTrue(PinCredentials.verify(pin, legacy))
        assertFalse(PinCredentials.verify(pin, hash.replace(":600000:", ":2147483647:")))
        assertFalse(PinCredentials.verify(pin, "v2:01:02"))
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun assertFails(action: () -> Unit) {
        try { action(); fail("异常文件或错误口令必须被拒绝") } catch (_: Exception) { }
    }
}
