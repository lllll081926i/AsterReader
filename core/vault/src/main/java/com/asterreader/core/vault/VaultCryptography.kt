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

    internal fun crypt(mode: Int, key: ByteArray, nonce: ByteArray, aad: ByteArray, value: ByteArray,
        offset: Int = 0, count: Int = value.size - offset): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(value, offset, count)
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
                    output.write(crypt(Cipher.ENCRYPT_MODE, dek, nonce(prefix, index), aad(header, index, count), buffer, 0, count))
                    buffer.fill(0, 0, count)
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
            val buffer = ByteArray(CHUNK_SIZE)
            var position = 0L
            try {
                while (position < reader.length) {
                    checkActive()
                    val count = minOf(CHUNK_SIZE.toLong(), reader.length - position).toInt()
                    reader.readInto(position, buffer, 0, count)
                    digest.update(buffer, 0, count)
                    position += count
                }
            } finally { buffer.fill(0) }
            if (expectedDigest != null) require(MessageDigest.isEqual(expectedDigest, digest.digest())) { "密文内容校验失败" }
        }
    }

    class Reader(file: File, master: ByteArray, private val offset: Long = 0,
        private val checkActive: () -> Unit = {}) : Closeable {
        private val source = RandomAccessFile(file, "r")
        private val header = ByteArray(104)
        private var dek = byteArrayOf()
        private var closed = false
        private val chunks = LinkedHashMap<Int, ByteArray>(2, 0.75f, true)
        internal var authenticatedChunkReads = 0L
            private set
        @Synchronized internal fun cachedPlainBytes() = chunks.values.sumOf { it.size }
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
            require(count in 0..CHUNK_SIZE) { "读取范围无效" }
            return ByteArray(count).also { readInto(position, it, 0, count) }
        }

        @Synchronized fun readInto(position: Long, destination: ByteArray, destinationOffset: Int, count: Int): Int {
            check(!closed) { "保险库会话已关闭" }
            require(position in 0..length && count in 0..CHUNK_SIZE && count.toLong() <= length - position) { "读取范围无效" }
            require(destinationOffset >= 0 && destinationOffset <= destination.size - count) { "目标范围无效" }
            try {
                checkActive()
                var copied = 0
                while (copied < count) {
                    checkActive()
                    val location = position + copied
                    val index = (location / CHUNK_SIZE).toInt()
                    val within = (location % CHUNK_SIZE).toInt()
                    val plainLength = minOf(CHUNK_SIZE.toLong(), length - index.toLong() * CHUNK_SIZE).toInt()
                    val plain = authenticatedChunk(index, plainLength)
                    val take = minOf(count - copied, plain.size - within)
                    plain.copyInto(destination, destinationOffset + copied, within, within + take)
                    copied += take
                }
                checkActive()
                return count
            } catch (failure: Throwable) {
                destination.fill(0, destinationOffset, destinationOffset + count)
                clearChunks()
                throw failure
            }
        }

        private fun authenticatedChunk(index: Int, plainLength: Int): ByteArray {
            chunks[index]?.let { return it }
            val ciphertext = ByteArray(plainLength + 16)
            source.seek(offset + HEADER_SIZE + index.toLong() * (CHUNK_SIZE + 16))
            source.readFully(ciphertext)
            val plain = crypt(Cipher.DECRYPT_MODE, dek, nonce(header, index), aad(header, index, plainLength), ciphertext)
            try { checkActive() } catch (failure: Throwable) { plain.fill(0); throw failure }
            authenticatedChunkReads++
            if (chunks.size == 2) {
                val oldest = chunks.entries.iterator()
                oldest.next().value.fill(0)
                oldest.remove()
            }
            chunks[index] = plain
            return plain
        }

        private fun clearChunks() { chunks.values.forEach { it.fill(0) }; chunks.clear() }

        fun stream(start: Long = 0, size: Long = length - start): InputStream {
            require(start in 0..length && size >= 0 && size <= length - start)
            return object : InputStream() {
                var position = start
                var remaining = size
                private var streamClosed = false
                private val single = ByteArray(1)
                private fun ensureOpen() {
                    check(!streamClosed) { "读取流已关闭" }
                    this@Reader.readInto(position, single, 0, 0)
                }
                override fun read(): Int {
                    return if (read(single) < 0) -1 else (single[0].toInt() and 255).also { single.fill(0) }
                }
                override fun read(bytes: ByteArray, off: Int, len: Int): Int {
                    require(off >= 0 && len >= 0 && off <= bytes.size - len)
                    ensureOpen()
                    if (len == 0) return 0
                    if (remaining == 0L) return -1
                    val count = minOf(len.toLong(), remaining, CHUNK_SIZE.toLong()).toInt()
                    this@Reader.readInto(position, bytes, off, count)
                    position += count; remaining -= count
                    return count
                }
                override fun skip(count: Long): Long {
                    ensureOpen()
                    val skipped = count.coerceIn(0, remaining)
                    position += skipped
                    remaining -= skipped
                    return skipped
                }
                override fun available(): Int { ensureOpen(); return minOf(remaining, Int.MAX_VALUE.toLong()).toInt() }
                override fun close() { streamClosed = true; single.fill(0) }
            }
        }

        @Synchronized override fun close() { if (!closed) { closed = true; clearChunks(); dek.fill(0); source.close() } }
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
