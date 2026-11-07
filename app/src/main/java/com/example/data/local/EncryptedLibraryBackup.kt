package com.example.data.local

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** 公开清单的认证加密封装。正文只在有界内存中出现，不生成明文临时文件。 */
object EncryptedLibraryBackup {
    private val magic = "ASTERB01".toByteArray(Charsets.US_ASCII)
    private const val ITERATIONS = 600_000
    private const val MAX_PLAINTEXT = 16 * 1024 * 1024
    private const val HEADER_SIZE = 8 + 4 + 16 + 12 + 4

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        require(password.size in 12..256) { "备份口令需为 12–256 个字符" }
        require(plaintext.size <= MAX_PLAINTEXT) { "书库清单超过 16 MB 上限" }
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_SIZE).put(magic).putInt(ITERATIONS)
            .put(salt).put(nonce).putInt(plaintext.size + 16).array()
        val key = derive(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(header)
            header + cipher.doFinal(plaintext)
        } finally { key.fill(0) }
    }

    fun decrypt(input: InputStream, password: CharArray): ByteArray {
        require(password.size in 1..256) { "请输入备份口令" }
        val source = DataInputStream(input)
        val header = ByteArray(HEADER_SIZE)
        try { source.readFully(header) } catch (_: IOException) { throw IOException("加密清单不完整或格式不正确") }
        val fields = ByteBuffer.wrap(header)
        val fileMagic = ByteArray(8).also(fields::get)
        require(fileMagic.contentEquals(magic)) { "不是支持的加密书库清单" }
        require(fields.int == ITERATIONS) { "不支持的加密清单参数" }
        val salt = ByteArray(16).also(fields::get)
        val nonce = ByteArray(12).also(fields::get)
        val size = fields.int
        require(size in 16..MAX_PLAINTEXT + 16) { "加密清单长度无效" }
        val ciphertext = ByteArray(size)
        try { source.readFully(ciphertext) } catch (_: IOException) { throw IOException("加密清单不完整") }
        require(source.read() == -1) { "加密清单包含多余数据" }
        val key = derive(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(header)
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            throw IOException("口令不正确或加密清单已损坏")
        } finally { key.fill(0); ciphertext.fill(0) }
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
