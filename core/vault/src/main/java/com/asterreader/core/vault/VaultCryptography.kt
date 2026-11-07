package com.asterreader.core.vault

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** 不可变密文对象；头与块使用互斥 nonce，完整长度参与认证。 */
object VaultCryptography {
    const val CHUNK_SIZE = 1024 * 1024
    const val MAX_LENGTH = 16L * 1024 * 1024 * 1024
    const val HEADER_SIZE = 120
    private val magic = "ASTERV02".toByteArray(Charsets.US_ASCII)
    private val random = SecureRandom()

    fun randomKey() = ByteArray(32).also(random::nextBytes)
    fun randomBytes(size: Int) = ByteArray(size).also(random::nextBytes)

    fun seal(key: ByteArray, value: ByteArray, aad: ByteArray): ByteArray {
        val nonce = randomBytes(12)
        return nonce + crypt(Cipher.ENCRYPT_MODE, key, nonce, aad, value)
    }

    fun unseal(key: ByteArray, value: ByteArray, aad: ByteArray): ByteArray {
        require(value.size >= 28) { "密文不完整" }
        return crypt(Cipher.DECRYPT_MODE, key, value.copyOfRange(0, 12), aad, value.copyOfRange(12, value.size))
    }

    internal fun crypt(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray, value: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(value)
    }

    /** 返回源摘要；调用者必须重读已落盘密文再发布。 */
    fun write(file: File, master: ByteArray, length: Long, input: InputStream,
        objectId: UUID = UUID.randomUUID(), checkActive: () -> Unit = {}): ByteArray {
        require(length in 0..MAX_LENGTH) { "文件超过 16 GiB 上限" }
        val dek = randomKey()
        val id = objectId
        val prefix = ByteBuffer.allocate(44).put(magic).putLong(id.mostSignificantBits).putLong(id.leastSignificantBits)
            .putLong(length).putInt(CHUNK_SIZE).put(randomBytes(8)).array()
        val header = prefix + seal(master, dek, "dek-v2".toByteArray() + prefix)
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(CHUNK_SIZE)
        try {
            FileOutputStream(file).use { output ->
                output.write(header)
                output.write(crypt(Cipher.ENCRYPT_MODE, dek, nonce(prefix, -1), header, byteArrayOf()))
                var remaining = length
                var index = 0
                while (remaining > 0) {
                    checkActive()
                    val count = minOf(CHUNK_SIZE.toLong(), remaining).toInt()
                    readFully(input, buffer, count)
                    digest.update(buffer, 0, count)
                    val plain = buffer.copyOf(count)
                    try { output.write(crypt(Cipher.ENCRYPT_MODE, dek, nonce(prefix, index), aad(header, index, count), plain)) }
                    finally { plain.fill(0) }
                    remaining -= count
                    index++
                }
                require(input.read() == -1) { "源文件在加密时发生变化" }
                checkActive()
                output.fd.sync()
            }
            return digest.digest()
        } finally { dek.fill(0); buffer.fill(0) }
    }

    fun verify(file: File, master: ByteArray, expectedDigest: ByteArray? = null, offset: Long = 0,
        checkActive: () -> Unit = {}) {
        Reader(file, master, offset, checkActive).use { reader ->
            val digest = MessageDigest.getInstance("SHA-256")
            var position = 0L
            while (position < reader.length) {
                checkActive()
                val bytes = reader.read(position, minOf(CHUNK_SIZE.toLong(), reader.length - position).toInt())
                try { digest.update(bytes) } finally { bytes.fill(0) }
                position += minOf(CHUNK_SIZE.toLong(), reader.length - position)
            }
            if (expectedDigest != null) require(MessageDigest.isEqual(expectedDigest, digest.digest())) { "密文内容校验失败" }
        }
    }

    class Reader(file: File, master: ByteArray, private val offset: Long = 0,
        private val checkActive: () -> Unit = {}) : Closeable {
        private val source = RandomAccessFile(file, "r")
        private val header = ByteArray(104)
        private var dek = byteArrayOf()
        private var closed = false
        val length: Long
        val objectId: UUID
        init {
            try {
                require(offset >= 0 && source.length() - offset >= HEADER_SIZE) { "密文不完整" }
                source.seek(offset)
                source.readFully(header)
                val fields = ByteBuffer.wrap(header)
                require(ByteArray(8).also(fields::get).contentEquals(magic)) { "不支持的密文格式" }
                objectId = UUID(fields.long, fields.long)
                length = fields.long
                require(length in 0..MAX_LENGTH && fields.int == CHUNK_SIZE) { "密文参数无效" }
                val chunks = (length + CHUNK_SIZE - 1) / CHUNK_SIZE
                require(source.length() - offset == HEADER_SIZE + length + chunks * 16) { "密文截断或包含多余数据" }
                checkActive()
                dek = unseal(master, header.copyOfRange(44, 104), "dek-v2".toByteArray() + header.copyOf(44))
                require(dek.size == 32) { "密钥长度无效" }
                val tag = ByteArray(16).also(source::readFully)
                crypt(Cipher.DECRYPT_MODE, dek, nonce(header, -1), header, tag)
            } catch (failure: Throwable) { dek.fill(0); source.close(); throw failure }
        }

        @Synchronized fun read(position: Long, count: Int): ByteArray {
            check(!closed) { "保险库会话已关闭" }
            checkActive()
            require(position in 0..length && count in 0..CHUNK_SIZE && count.toLong() <= length - position) { "读取范围无效" }
            val result = ByteArray(count)
            try {
                var copied = 0
                while (copied < count) {
                    checkActive()
                    val location = position + copied
                    val index = (location / CHUNK_SIZE).toInt()
                    val within = (location % CHUNK_SIZE).toInt()
                    val plainLength = minOf(CHUNK_SIZE.toLong(), length - index.toLong() * CHUNK_SIZE).toInt()
                    val ciphertext = ByteArray(plainLength + 16)
                    source.seek(offset + HEADER_SIZE + index.toLong() * (CHUNK_SIZE + 16))
                    source.readFully(ciphertext)
                    val plain = crypt(Cipher.DECRYPT_MODE, dek, nonce(header, index), aad(header, index, plainLength), ciphertext)
                    try {
                        val take = minOf(count - copied, plain.size - within)
                        plain.copyInto(result, copied, within, within + take)
                        copied += take
                    } finally { plain.fill(0) }
                }
                checkActive()
                return result
            } catch (failure: Throwable) { result.fill(0); throw failure }
        }

        fun stream(start: Long = 0, size: Long = length - start): InputStream {
            require(start in 0..length && size >= 0 && size <= length - start)
            return object : InputStream() {
                var position = start
                var remaining = size
                override fun read(): Int {
                    val one = ByteArray(1)
                    return if (read(one) < 0) -1 else one[0].toInt() and 255
                }
                override fun read(bytes: ByteArray, off: Int, len: Int): Int {
                    require(off >= 0 && len >= 0 && off <= bytes.size - len)
                    if (len == 0) return 0
                    if (remaining == 0L) return -1
                    val count = minOf(len.toLong(), remaining, CHUNK_SIZE.toLong()).toInt()
                    val plain = this@Reader.read(position, count)
                    try { plain.copyInto(bytes, off) } finally { plain.fill(0) }
                    position += count; remaining -= count
                    return count
                }
            }
        }

        @Synchronized override fun close() { if (!closed) { closed = true; dek.fill(0); source.close() } }
    }

    private fun nonce(header: ByteArray, index: Int) = ByteBuffer.allocate(12).put(header, 36, 8).putInt(index).array()
    private fun aad(header: ByteArray, index: Int, size: Int) = header + ByteBuffer.allocate(8).putInt(index).putInt(size).array()
    private fun readFully(input: InputStream, buffer: ByteArray, count: Int) {
        var read = 0
        while (read < count) {
            val next = input.read(buffer, read, count - read)
            require(next > 0) { "源文件不完整" }
            read += next
        }
    }
}

/** 固定参数防止恶意导入提升成本；串行派生控制内存峰值。 */
object VaultPassword {
    private val magic = "ASTERX02".toByteArray(Charsets.US_ASCII)
    const val HEADER_SIZE = 96
    private const val MEMORY = 65536
    private const val PASSES = 3
    private const val LANES = 4

    fun validate(password: CharArray) {
        require(password.size in 12..256) { "口令需为 12–256 个字符" }
        require(password.toSet().size >= 6 && !password.all(Char::isDigit)) { "请使用随机长口令，不要使用重复字符或纯数字" }
        val lowered = password.concatToString().lowercase()
        require(listOf("123456", "password", "qwerty", "abcdef").none(lowered::contains)) { "口令过于常见，请生成随机口令" }
    }

    fun generate(): CharArray {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789-_"
        val random = SecureRandom()
        return CharArray(24) { alphabet[random.nextInt(alphabet.length)] }
    }

    fun wrap(master: ByteArray, password: CharArray): ByteArray {
        validate(password)
        val salt = VaultCryptography.randomBytes(16)
        val nonce = VaultCryptography.randomBytes(12)
        val prefix = ByteBuffer.allocate(48).put(magic).putInt(MEMORY).putInt(PASSES).putInt(LANES).put(salt).put(nonce).array()
        val key = derive(password, salt)
        return try { prefix + VaultCryptography.crypt(Cipher.ENCRYPT_MODE, key, nonce, prefix, master) }
        finally { key.fill(0) }
    }

    fun unwrap(header: ByteArray, password: CharArray): ByteArray {
        require(header.size == HEADER_SIZE && password.size in 1..256) { "密钥槽无效" }
        val fields = ByteBuffer.wrap(header)
        require(ByteArray(8).also(fields::get).contentEquals(magic)) { "不支持的加密格式" }
        require(fields.int == MEMORY && fields.int == PASSES && fields.int == LANES) { "不支持的口令参数" }
        val salt = ByteArray(16).also(fields::get)
        val nonce = ByteArray(12).also(fields::get)
        val key = derive(password, salt)
        return try { VaultCryptography.crypt(Cipher.DECRYPT_MODE, key, nonce, header.copyOf(48), header.copyOfRange(48, 96)) }
        finally { key.fill(0) }
    }

    @Synchronized private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13).withMemoryAsKB(MEMORY)
            .withIterations(PASSES).withParallelism(LANES).withSalt(salt).build()
        return try {
            val generator = Argon2BytesGenerator()
            generator.init(parameters)
            ByteArray(32).also { generator.generateBytes(password, it) }
        } finally { parameters.clear() }
    }
}
