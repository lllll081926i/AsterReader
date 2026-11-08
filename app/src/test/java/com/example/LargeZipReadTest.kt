package com.example

import com.example.data.local.ComicArchives
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SeekableByteChannel
import java.nio.channels.ClosedChannelException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LargeZipReadTest {
    @Test fun zip64ReadsImageBeyondFourGiBWithoutReadingTheWholeArchive() {
        val offset = 5L * 1024 * 1024 * 1024
        val name = "001.jpg".toByteArray()
        val image = byteArrayOf(1, 2, 3, 4)
        val crc = CRC32().apply { update(image) }.value
        val bytes = ByteArrayOutputStream()
        fun fields(size: Int, block: ByteBuffer.() -> Unit) {
            bytes.write(ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).apply(block).array())
        }
        fields(30) {
            putInt(0x04034b50); putShort(20); putShort(0); putShort(0); putInt(0)
            putInt(crc.toInt()); putInt(image.size); putInt(image.size); putShort(name.size.toShort()); putShort(0)
        }
        bytes.write(name); bytes.write(image)
        val directoryOffset = offset + bytes.size()
        fields(46) {
            putInt(0x02014b50); putShort(45); putShort(45); putShort(0); putShort(0); putInt(0)
            putInt(crc.toInt()); putInt(image.size); putInt(image.size)
            putShort(name.size.toShort()); putShort(12); putShort(0); putShort(0); putShort(0)
            putInt(0); putInt(-1)
        }
        bytes.write(name)
        fields(12) { putShort(1); putShort(8); putLong(offset) }
        val directorySize = offset + bytes.size() - directoryOffset
        val zip64Offset = offset + bytes.size()
        fields(56) {
            putInt(0x06064b50); putLong(44); putShort(45); putShort(45)
            putInt(0); putInt(0); putLong(1); putLong(1); putLong(directorySize); putLong(directoryOffset)
        }
        fields(20) { putInt(0x07064b50); putInt(0); putLong(zip64Offset); putInt(1) }
        fields(22) {
            putInt(0x06054b50); putShort(0); putShort(0); putShort(1); putShort(1)
            putInt(directorySize.toInt()); putInt(-1); putShort(0)
        }
        val channel = SparseZipChannel(offset, bytes.toByteArray())
        ComicArchives.openZip(channel).use { archive ->
            assertEquals(listOf("001.jpg"), archive.pageNames)
            assertArrayEquals(image, archive.openPage(1).use { it.readBytes() })
            assertTrue("只读取中央目录和目标图片", channel.readBytes < 256 * 1024)
        }
        assertFalse(channel.isOpen)
    }

    @Test fun compressedChineseEntriesRemainReadableAndNaturallySorted() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output, charset("GB18030")).use { zip ->
            listOf("漫画/10.jpg", "漫画/2.jpg").forEach { name ->
                zip.putNextEntry(ZipEntry(name)); zip.write(name.toByteArray()); zip.closeEntry()
            }
        }
        ComicArchives.openZip(SparseZipChannel(0, output.toByteArray())).use { archive ->
            assertEquals(listOf("漫画/2.jpg", "漫画/10.jpg"), archive.pageNames)
            assertEquals("漫画/2.jpg", archive.openPage(1).use { it.readBytes().toString(Charsets.UTF_8) })
        }
    }

    @Test fun invalidZipClosesTheOwnedChannel() {
        val channel = SparseZipChannel(0, byteArrayOf(1, 2, 3))
        assertThrows(java.io.IOException::class.java) { ComicArchives.openZip(channel) }
        assertFalse(channel.isOpen)
    }

    /** 前缀空洞模拟真实 64 位偏移，不分配或读取数 GiB 的填充数据。 */
    private class SparseZipChannel(val offset: Long, val data: ByteArray) : SeekableByteChannel {
        private var cursor = 0L
        private var opened = true
        var readBytes = 0L
        override fun isOpen() = opened
        override fun close() { opened = false }
        override fun size() = offset + data.size
        override fun position() = cursor
        override fun position(newPosition: Long): SeekableByteChannel {
            require(newPosition >= 0); cursor = newPosition; return this
        }
        override fun read(dst: ByteBuffer): Int {
            if (!opened) throw ClosedChannelException()
            if (cursor >= size()) return -1
            val count = minOf(dst.remaining().toLong(), size() - cursor).toInt()
            repeat(count) {
                dst.put(if (cursor < offset) 0.toByte() else data[(cursor - offset).toInt()]); cursor++
            }
            readBytes += count
            check(readBytes < 1024 * 1024) { "ZIP 不应顺序读取大文件" }
            return count
        }
        override fun write(src: ByteBuffer): Int = throw UnsupportedOperationException()
        override fun truncate(size: Long): SeekableByteChannel = throw UnsupportedOperationException()
    }
}
