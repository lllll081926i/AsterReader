package com.asterreader.core.vault

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile

class VaultCryptographyTest {
    @get:Rule val temporary = TemporaryFolder()
    private val key = ByteArray(32) { (it + 1).toByte() }

    @Test fun aes256GcmMatchesPublishedNistVector() {
        val result = VaultCryptography.crypt(javax.crypto.Cipher.ENCRYPT_MODE, ByteArray(32), ByteArray(12), byteArrayOf(), ByteArray(16))
        assertEquals("cea7403d4d606b6e074ec5d3baf39d18d0d1c8a799996bf0265b98b5d48ab919",
            result.joinToString("") { "%02x".format(it) })
    }

    @Test fun emptyFileStillAuthenticates() {
        val file = temporary.newFile()
        VaultCryptography.write(file, key, 0, ByteArrayInputStream(byteArrayOf()))
        VaultCryptography.verify(file, key)
        RandomAccessFile(file, "rw").use { it.seek(119); it.write(7) }
        assertThrows(Exception::class.java) { VaultCryptography.verify(file, key) }
    }

    @Test fun crossChunkRandomReadsMatchOriginalAndSessionRevokes() {
        val bytes = ByteArray(VaultCryptography.CHUNK_SIZE * 2 + 37) { (it % 251).toByte() }
        val file = temporary.newFile()
        val digest = VaultCryptography.write(file, key, bytes.size.toLong(), bytes.inputStream())
        VaultCryptography.verify(file, key, digest)
        var valid = true
        VaultCryptography.Reader(file, key, checkActive = { check(valid) }).use { reader ->
            assertArrayEquals(bytes.copyOfRange(1048571, 1048591), reader.read(1048571, 20))
            assertArrayEquals(bytes.takeLast(37).toByteArray(), reader.read(2097152, 37))
            valid = false
            assertThrows(IllegalStateException::class.java) { reader.read(0, 1) }
        }
    }

    @Test fun wrongKeyHeaderTamperTailAndTruncationFailClosed() {
        val bytes = ByteArray(51) { it.toByte() }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, bytes.size.toLong(), bytes.inputStream())
        val original = file.readBytes()
        assertThrows(Exception::class.java) { VaultCryptography.verify(file, ByteArray(32)) }
        for (position in listOf(0, 24, 32, 36, 44, 104, 120, original.lastIndex)) {
            file.writeBytes(original.copyOf().apply { this[position] = (this[position].toInt() xor 1).toByte() })
            assertThrows(Exception::class.java) { VaultCryptography.verify(file, key) }
        }
        file.writeBytes(original + byteArrayOf(0))
        assertThrows(Exception::class.java) { VaultCryptography.verify(file, key) }
        file.writeBytes(original.copyOf(original.size - 1))
        assertThrows(Exception::class.java) { VaultCryptography.verify(file, key) }
    }

    @Test fun smallReadsReuseAuthenticatedChunksWithBoundedMemoryAndRevocation() {
        val bytes = ByteArray(VaultCryptography.CHUNK_SIZE * 3) { (it % 251).toByte() }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, bytes.size.toLong(), bytes.inputStream())
        var valid = true
        VaultCryptography.Reader(file, key, checkActive = { check(valid) }).use { reader ->
            repeat(256) { block ->
                val position = block * 8192
                assertArrayEquals(bytes.copyOfRange(position, position + 8192), reader.read(position.toLong(), 8192))
            }
            assertEquals(2L, reader.authenticatedChunkReads)
            reader.read(2097152, 1)
            assertEquals(3L, reader.authenticatedChunkReads)
            assertEquals(2 * VaultCryptography.CHUNK_SIZE, reader.cachedPlainBytes())
            reader.read(0, 1)
            assertEquals(4L, reader.authenticatedChunkReads)
            valid = false
            assertThrows(IllegalStateException::class.java) { reader.read(0, 1) }
            assertEquals(0, reader.cachedPlainBytes())
        }
    }

    @Test fun swappedChunksAndWholeChunkTruncationAreRejected() {
        val bytes = ByteArray(VaultCryptography.CHUNK_SIZE * 2) { (it / 1048576).toByte() }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, bytes.size.toLong(), bytes.inputStream())
        val original = file.readBytes()
        val size = VaultCryptography.CHUNK_SIZE + 16
        file.writeBytes(original.copyOfRange(0, 120) + original.copyOfRange(120 + size, original.size) + original.copyOfRange(120, 120 + size))
        assertThrows(Exception::class.java) { VaultCryptography.verify(file, key) }
        file.writeBytes(original.copyOf(original.size - size))
        assertThrows(Exception::class.java) { VaultCryptography.verify(file, key) }
    }

    @Test fun cachedReaderClearsPlaintextAfterAuthenticationFailureAndClose() {
        val bytes = ByteArray(VaultCryptography.CHUNK_SIZE + 32) { (it % 251).toByte() }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, bytes.size.toLong(), bytes.inputStream())
        val reader = VaultCryptography.Reader(file, key)
        reader.use {
            it.read(0, 1)
            assertEquals(VaultCryptography.CHUNK_SIZE, it.cachedPlainBytes())
            RandomAccessFile(file, "rw").use { output ->
                output.seek(file.length() - 1)
                val value = output.read()
                output.seek(file.length() - 1)
                output.write(value xor 1)
            }
            assertThrows(Exception::class.java) { it.read(VaultCryptography.CHUNK_SIZE.toLong(), 1) }
            assertEquals(0, it.cachedPlainBytes())
            it.read(0, 1)
            assertTrue(it.cachedPlainBytes() > 0)
        }
        assertEquals(0, reader.cachedPlainBytes())
    }

    @Test fun cancellationAndSourceSizeChangeDoNotProduceValidObject() {
        val file = temporary.newFile()
        assertThrows(Exception::class.java) { VaultCryptography.write(file, key, 2, byteArrayOf(1).inputStream()) }
        assertThrows(Exception::class.java) { VaultCryptography.write(file, key, 1, byteArrayOf(1, 2).inputStream()) }
        assertThrows(Exception::class.java) { VaultCryptography.write(file, key, 0, byteArrayOf().inputStream()) { error("已取消") } }
    }

    @Test fun directReadsCrossChunksWithoutChangingBytesOutsideDestination() {
        val plain = ByteArray(VaultCryptography.CHUNK_SIZE + 40) { (it % 251).toByte() }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, plain.size.toLong(), plain.inputStream())
        VaultCryptography.Reader(file, key).use { reader ->
            val destination = ByteArray(80) { 77 }
            val start = VaultCryptography.CHUNK_SIZE - 20L
            assertEquals(50, reader.readInto(start, destination, 10, 50))
            assertArrayEquals(plain.copyOfRange(start.toInt(), start.toInt() + 50), destination.copyOfRange(10, 60))
            assertTrue(destination.take(10).all { it == 77.toByte() })
            assertTrue(destination.drop(60).all { it == 77.toByte() })
            assertEquals(2L, reader.authenticatedChunkReads)
            assertThrows(IllegalArgumentException::class.java) { reader.readInto(0, destination, 70, 20) }
        }
    }

    @Test fun directReadsClearPartialPlaintextWhenNextChunkFailsAuthentication() {
        val plain = ByteArray(VaultCryptography.CHUNK_SIZE + 40) { 77 }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, plain.size.toLong(), plain.inputStream())
        RandomAccessFile(file, "rw").use {
            it.seek(file.length() - 1)
            val last = it.read()
            it.seek(file.length() - 1)
            it.write(last xor 1)
        }
        VaultCryptography.Reader(file, key).use { reader ->
            val destination = ByteArray(80) { 99 }
            assertThrows(Exception::class.java) {
                reader.readInto(VaultCryptography.CHUNK_SIZE - 20L, destination, 10, 50)
            }
            assertTrue(destination.copyOfRange(10, 60).all { it == 0.toByte() })
            assertTrue(destination.take(10).all { it == 99.toByte() })
            assertEquals(0, reader.cachedPlainBytes())
        }
    }

    @Test fun streamsSkipWithoutDecryptingSkippedChunksAndRespectRevocationAndClose() {
        val plain = ByteArray(VaultCryptography.CHUNK_SIZE * 3 + 7) { (it % 251).toByte() }
        val file = temporary.newFile()
        VaultCryptography.write(file, key, plain.size.toLong(), plain.inputStream())
        var valid = true
        VaultCryptography.Reader(file, key, checkActive = { check(valid) }).use { reader ->
            val stream = reader.stream()
            assertEquals(0L, stream.skip(-1))
            assertEquals(VaultCryptography.CHUNK_SIZE * 3L, stream.skip(VaultCryptography.CHUNK_SIZE * 3L))
            assertEquals(0L, reader.authenticatedChunkReads)
            assertEquals(7, stream.available())
            assertEquals(plain[VaultCryptography.CHUNK_SIZE * 3].toInt() and 255, stream.read())
            assertEquals(1L, reader.authenticatedChunkReads)
            valid = false
            assertThrows(IllegalStateException::class.java) { stream.skip(1) }
            assertEquals(0, reader.cachedPlainBytes())
            valid = true
            stream.close()
            assertThrows(IllegalStateException::class.java) { stream.read() }
            assertEquals(plain[0], reader.read(0, 1)[0])
        }
    }

    @Test fun passwordEnvelopeAuthenticatesParametersAndRequiresStrongCreationPassword() {
        val password = "pM7-Vc2_Je9-Tx4_kR8".toCharArray()
        val header = VaultPassword.wrap(key, password)
        assertArrayEquals(key, VaultPassword.unwrap(header, password))
        assertThrows(Exception::class.java) { VaultPassword.unwrap(header, "a-different-long-secret".toCharArray()) }
        assertThrows(Exception::class.java) { VaultPassword.unwrap(header.copyOf().apply { this[8] = 127 }, password) }
        assertThrows(Exception::class.java) { VaultPassword.wrap(key, "123456789012".toCharArray()) }
        assertThrows(Exception::class.java) { VaultPassword.wrap(key, "aaaaaaaaaaaa".toCharArray()) }
        VaultPassword.validate(VaultPassword.generate())
        assertFalse(header.contentEquals(VaultPassword.wrap(key, password)))
    }
}
