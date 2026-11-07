package com.example.data.local.vault

import com.asterreader.core.vault.VaultCryptography
import com.example.data.local.ComicArchive
import com.example.data.local.ComicArchives
import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.NonWritableChannelException
import java.nio.channels.SeekableByteChannel

/** 归档直接使用随机解密通道，不尝试把代理 FD 重新打开为文件路径。 */
internal class VaultComicArchive(private val store: FileVaultStore, record: VaultRecord) : ComicArchive {
    private val reader = store.openReader(record.book.id)
    private val channel = ReaderChannel(reader)
    private var zip: org.apache.commons.compress.archivers.zip.ZipFile? = null
    private var seven: org.apache.commons.compress.archivers.sevenz.SevenZFile? = null
    private var rar: com.github.junrar.Archive? = null
    private val names: List<String>
    private val tarOffsets = mutableMapOf<String, Pair<Long, Long>>()
    override val concurrentReads = false
    override val pageCount get() = names.size
    override val pageNames get() = names

    init {
        try {
            val available = when (record.book.format) {
                "ZIP", "CBZ", "EPUB" -> {
                    zip = org.apache.commons.compress.archivers.zip.ZipFile.builder().setSeekableByteChannel(channel)
                        .setCharset("GB18030").get()
                    zip!!.entries.asSequence().onEachIndexed { index, _ -> require(index < 100000) { "归档条目过多" } }
                        .filter { !it.isDirectory }.map { it.name }.toList()
                }
                "7Z", "CB7" -> {
                    seven = org.apache.commons.compress.archivers.sevenz.SevenZFile.builder().setSeekableByteChannel(channel)
                        .setMaxMemoryLimitKb(65536).get()
                    seven!!.entries.filter { !it.isDirectory }.map { it.name }
                }
                "RAR", "CBR" -> {
                    val manager = com.github.junrar.volume.VolumeManager { archive, previous ->
                        if (previous != null) null else object : com.github.junrar.volume.Volume {
                            override fun getArchive() = archive
                            override fun getLength() = reader.length
                            override fun getChannel() = object : com.github.junrar.io.SeekableReadOnlyByteChannel {
                                override fun getPosition(): Long = this@VaultComicArchive.channel.position()
                                override fun setPosition(value: Long) { this@VaultComicArchive.channel.position(value) }
                                override fun read(): Int { val one = ByteArray(1); return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 255 }
                                override fun read(bytes: ByteArray, off: Int, len: Int): Int = this@VaultComicArchive.channel.read(ByteBuffer.wrap(bytes, off, len))
                                override fun readFully(bytes: ByteArray, length: Int): Int {
                                    var count = 0
                                    while (count < length) { val next = read(bytes, count, length - count); if (next < 0) throw EOFException(); count += next }
                                    return count
                                }
                                override fun close() { this@VaultComicArchive.channel.close() }
                            }
                        }
                    }
                    rar = com.github.junrar.Archive(manager, null, null)
                    rar!!.fileHeaders.filter { !it.isDirectory }.map { it.fileName }
                }
                "TAR", "CBT" -> {
                    validateTar()
                    org.apache.commons.compress.archivers.tar.TarArchiveInputStream(reader.stream()).use { input ->
                        var count = 0
                        while (true) {
                            val entry = input.nextEntry ?: break
                            require(++count <= 20000 && entry.isCheckSumOK && !entry.isSparse)
                            require(entry.size in 0..reader.length - input.bytesRead)
                            if (entry.isFile && ComicArchives.isImageEntry(entry.name)) tarOffsets[entry.name] = input.bytesRead to entry.size
                        }
                    }
                    tarOffsets.keys.toList()
                }
                "IMAGE" -> listOf("image")
                else -> error("不支持的加密漫画格式")
            }
            names = if (record.pageNames.isNotEmpty()) record.pageNames else if (record.book.format == "IMAGE") available else ComicArchives.sortedImageNames(available)
            require(names.size == record.book.totalPages && names.all { it in available }) { "加密漫画页索引不匹配" }
        } catch (failure: Throwable) { close(); throw failure }
    }

    override fun openPage(number: Int): InputStream {
        val name = names.getOrNull(number - 1) ?: error("页码超出范围")
        zip?.let { return it.getInputStream(it.getEntry(name)) }
        seven?.let { archive -> return archive.getInputStream(archive.entries.first { it.name == name }) }
        rar?.let { archive -> return archive.getInputStream(archive.fileHeaders.first { it.fileName == name }) }
        tarOffsets[name]?.let { (offset, size) -> return reader.stream(offset, size) }
        return reader.stream()
    }

    private fun validateTar() {
        var position = 0L
        var count = 0
        var chain = 0
        while (position < reader.length) {
            require(reader.length - position >= 512)
            val bytes = reader.read(position, 512)
            if (bytes.all { it == 0.toByte() }) return
            val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry(bytes)
            require(++count <= 20000 && entry.isCheckSumOK && !entry.isSparse)
            val available = reader.length - position - 512
            require(entry.size in 0..available)
            if (bytes[156].toInt().toChar() in listOf('L', 'K', 'x', 'g')) require(entry.size <= 65536 && ++chain <= 16) else chain = 0
            val padding = (512 - entry.size % 512) % 512
            require(padding <= available - entry.size)
            position += 512 + entry.size + padding
            bytes.fill(0)
        }
    }

    override fun close() {
        try { zip?.close(); seven?.close(); rar?.close() } finally { channel.close(); store.release(reader) }
    }
}

internal class ReaderChannel(private val reader: VaultCryptography.Reader) : SeekableByteChannel {
    private var location = 0L
    private var open = true
    @Synchronized override fun read(dst: ByteBuffer): Int {
        check(open)
        if (!dst.hasRemaining()) return 0
        if (location >= reader.length) return -1
        val bytes = reader.read(location, minOf(dst.remaining().toLong(), reader.length - location, VaultCryptography.CHUNK_SIZE.toLong()).toInt())
        try { dst.put(bytes); location += bytes.size; return bytes.size } finally { bytes.fill(0) }
    }
    @Synchronized override fun position() = location
    @Synchronized override fun position(newPosition: Long): SeekableByteChannel { require(newPosition >= 0); location = newPosition; return this }
    override fun size() = reader.length
    override fun isOpen() = open
    override fun close() { open = false }
    override fun write(src: ByteBuffer): Int = throw NonWritableChannelException()
    override fun truncate(size: Long): SeekableByteChannel = throw NonWritableChannelException()
}
