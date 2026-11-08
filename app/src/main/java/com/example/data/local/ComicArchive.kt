package com.example.data.local

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * 统一的漫画归档读取接口。页序固定为「自然排序去噪后的图片条目」。
 * 所有实现都按页码随机读取，不把整本解压到磁盘。
 */
internal interface ComicArchive : Closeable {
    val pageCount: Int
    val pageNames: List<String>
    fun openPage(number: Int): InputStream

    /** 是否允许在多个线程上同时取页。ZIP/目录类可以，7z/RAR/PDF 的随机读必须串行。 */
    val concurrentReads: Boolean get() = true
}

internal object ComicArchives {

    private val IMAGE_EXTENSIONS = LibraryRepository.IMAGE_EXTENSIONS.map { it.lowercase(Locale.ROOT) }.toSet()

    fun isImageEntry(name: String): Boolean {
        val normalized = name.replace('\\', '/')
        if (normalized.endsWith("/")) return false
        val lower = normalized.lowercase(Locale.ROOT)
        if (lower.startsWith("__macosx/") || lower.contains("/__macosx/")) return false
        val leaf = lower.substringAfterLast('/')
        if (leaf.startsWith("._") || leaf == ".ds_store" || leaf.startsWith(".")) return false
        return leaf.substringAfterLast('.', "") in IMAGE_EXTENSIONS
    }

    fun sortedImageNames(names: List<String>): List<String> =
        names.filter(::isImageEntry).sortedWith { a, b -> LibraryRepository.naturalCompare(a, b) }

    /** 依据格式打开对应归档；格式不受支持时抛出可读错误。 */
    fun open(file: File, format: String): ComicArchive = when (format.uppercase(Locale.ROOT)) {
        "CBZ", "ZIP" -> ZipComicArchive(file)
        // EPUB 必须按 OPF spine 排序，否则图片会按文件名乱序、封面重复算一页
        "EPUB" -> runCatching { EpubComicArchive(file) }.getOrElse { ZipComicArchive(file) }
        "CBT", "TAR" -> TarComicArchive(file)
        "CB7", "7Z" -> SevenZComicArchive(file)
        "CBR", "RAR" -> RarComicArchive(file)
        "IMAGE" -> SingleImageArchive(file)
        else -> throw IOException("不支持的漫画容器：$format")
    }
}

/** 旧中文 ZIP 常用 GBK 文件名；先按 UTF-8 校验，再兼容 GB18030。 */
private fun openComicZip(file: File): ZipFile = try {
    ZipFile(file)
} catch (_: java.util.zip.ZipException) {
    ZipFile(file, java.nio.charset.Charset.forName("GB18030"))
}

/**
 * EPUB 阅读顺序来自 OPF spine，而不是文件名：
 * META-INF/container.xml → OPF → manifest(id→href) → spine(idref…) →
 * 图片直接成页，XHTML 按文档内 img/image 出现顺序展开。
 * 解析失败时由 [ComicArchives.open] 退化为普通 ZIP。
 */
private class EpubComicArchive(file: File) : ComicArchive {
    private val zip = openComicZip(file)
    private val xmlBudget = EpubXmlBudget()
    private val entries: List<ZipEntry> = try { resolveSpinePages(zip)
        .mapNotNull { name -> zip.getEntry(name) }
        .ifEmpty {
            ComicArchives.sortedImageNames(zip.entries().asSequence().map { it.name }.toList())
                .mapNotNull { name -> zip.getEntry(name) }
        }
    } catch (failure: Throwable) { zip.close(); throw failure }

    override val pageCount get() = entries.size
    override val pageNames get() = entries.map { it.name }

    override fun openPage(number: Int): InputStream {
        val entry = entries.getOrNull(number - 1) ?: throw IOException("页面不存在：$number")
        return zip.getInputStream(entry)
    }

    override fun close() = zip.close()

    private fun resolveSpinePages(zip: ZipFile): List<String> {
        val container = zip.getEntry("META-INF/container.xml") ?: return emptyList()
        val containerDoc = parse(zip.getInputStream(container)) ?: return emptyList()
        val rootPath = containerDoc
            .getElementsByTagNameNS("*", "rootfile")
            .item(0)?.attributes?.getNamedItem("full-path")?.nodeValue
            ?: return emptyList()

        val opf = zip.getEntry(rootPath) ?: return emptyList()
        val opfDoc = parse(zip.getInputStream(opf)) ?: return emptyList()
        val base = rootPath.substringBeforeLast('/', "")

        val manifest = HashMap<String, Pair<String, String>>()
        val items = opfDoc.getElementsByTagNameNS("*", "item")
        for (index in 0 until items.length) {
            val item = items.item(index) ?: continue
            val id = item.attributes?.getNamedItem("id")?.nodeValue ?: continue
            val href = item.attributes?.getNamedItem("href")?.nodeValue ?: continue
            val type = item.attributes?.getNamedItem("media-type")?.nodeValue.orEmpty()
            manifest[id] = resolve(base, href) to type
        }

        val ordered = ArrayList<String>()
        val spine = opfDoc.getElementsByTagNameNS("*", "itemref")
        for (index in 0 until spine.length) {
            val ref = spine.item(index) ?: continue
            val idref = ref.attributes?.getNamedItem("idref")?.nodeValue ?: continue
            val (path, type) = manifest[idref] ?: continue
            when {
                type.startsWith("image/") -> ordered.add(path)
                type.contains("xhtml") || type.contains("html") -> {
                    val entry = zip.getEntry(path) ?: continue
                    ordered.addAll(imagesInDocument(zip.getInputStream(entry), path.substringBeforeLast('/', "")))
                }
            }
        }
        return ordered.filter { zip.getEntry(it) != null && ComicArchives.isImageEntry(it) }
            .distinct()
    }

    /** 抽出 XHTML 里出现的图片，保持文档顺序；忽略 SVG 内联与缺失资源。 */
    private fun imagesInDocument(stream: InputStream, base: String): List<String> {
        val document = parse(stream) ?: return emptyList()
        val found = ArrayList<String>()
        for (tag in listOf("img", "image")) {
            val nodes = document.getElementsByTagNameNS("*", tag)
            for (index in 0 until nodes.length) {
                val node = nodes.item(index) ?: continue
                val src = node.attributes?.getNamedItem("src")?.nodeValue
                    ?: node.attributes?.getNamedItemNS("http://www.w3.org/1999/xlink", "href")?.nodeValue
                    ?: node.attributes?.getNamedItem("href")?.nodeValue
                    ?: continue
                if (src.startsWith("data:") || src.startsWith("http")) continue
                found.add(resolve(base, src))
            }
        }
        return found
    }

    private fun parse(stream: InputStream): org.w3c.dom.Document? {
        val bytes = xmlBudget.read(stream)
        return try {
            val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            // 完整关闭 DOCTYPE / 外部实体 / XInclude：任何一项设置失败都拒绝解析，
            // 让 ComicArchives.open 回退到普通 ZIP，绝不带着 XXE 风险继续。
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false
            factory.newDocumentBuilder().parse(bytes.inputStream())
        } catch (_: Exception) {
            null
        }
    }

    /** 解析相对路径，处理 ../ 与 URL 编码，得到 ZIP 内部条目名。 */
    private fun resolve(base: String, href: String): String {
        val decoded = java.net.URLDecoder.decode(href.substringBefore('#'), "UTF-8")
        val raw = if (decoded.startsWith("/")) decoded.drop(1)
        else if (base.isBlank()) decoded else "$base/$decoded"
        val stack = ArrayDeque<String>()
        for (part in raw.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }
}

/** 先限制展开字节、深度和节点，再构造 DOM；预算覆盖整本 EPUB。 */
internal class EpubXmlBudget {
    private var remainingBytes = 16 * 1024 * 1024
    private var remainingNodes = 100_000

    fun read(stream: InputStream): ByteArray {
        val bytes = stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            val limit = minOf(2 * 1024 * 1024, remainingBytes)
            while (true) {
                val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size() + 1))
                if (count < 0) break
                if (output.size() + count > limit) throw IOException("EPUB XML 展开数据超过上限")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        remainingBytes -= bytes.size
        val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setInput(bytes.inputStream(), null)
        var nodes = 0
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.depth > 64) throw IOException("EPUB XML 层级过深")
            if (event == org.xmlpull.v1.XmlPullParser.DOCDECL) throw IOException("EPUB 不允许 XML 实体声明")
            if (event == org.xmlpull.v1.XmlPullParser.START_TAG) {
                if (++nodes > 20_000 || --remainingNodes < 0) throw IOException("EPUB XML 节点过多")
            }
            event = parser.nextToken()
        }
        return bytes
    }
}

private class ZipComicArchive(file: File) : ComicArchive {
    private val zip = openComicZip(file)
    private val entries: List<ZipEntry> = ComicArchives.sortedImageNames(
        zip.entries().asSequence().map { it.name }.toList()
    ).mapNotNull { name -> zip.getEntry(name) }

    override val pageCount get() = entries.size
    override val pageNames get() = entries.map { it.name }

    override fun openPage(number: Int): InputStream {
        val entry = entries.getOrNull(number - 1) ?: throw IOException("页面不存在：$number")
        return zip.getInputStream(entry)
    }

    override fun close() = zip.close()
}

private class SevenZComicArchive(file: File) : ComicArchive {
    private val sevenZ = SevenZFile.builder().setFile(file).setMaxMemoryLimitKb(65536).get()
    private val entries: List<SevenZArchiveEntry> = run {
        val all = sevenZ.entries.filter { !it.isDirectory }.map { it.name }
        val ordered = ComicArchives.sortedImageNames(all)
        val byName = sevenZ.entries.filter { !it.isDirectory }.associateBy { it.name }
        ordered.mapNotNull { byName[it] }
    }

    override val pageCount get() = entries.size
    override val pageNames get() = entries.map { it.name }
    override val concurrentReads get() = false

    override fun openPage(number: Int): InputStream {
        val entry = entries.getOrNull(number - 1) ?: throw IOException("页面不存在：$number")
        return sevenZ.getInputStream(entry)
    }

    override fun close() = sevenZ.close()
}

private class RarComicArchive(file: File) : ComicArchive {
    private val archive = try {
        com.github.junrar.Archive(file)
    } catch (failure: Throwable) {
        val name = failure.javaClass.name
        val hint = if (name.contains("RarV5", true) || name.contains("Unsupported", true))
            "RAR5 暂不支持，请转换为 CBZ / ZIP"
        else "RAR 文件无法读取，可能已加密或损坏"
        throw IOException(hint, failure)
    }
    private val headers = run {
        val images = archive.fileHeaders.filter { !it.isDirectory && ComicArchives.isImageEntry(it.fileName) }
        images.sortedWith { a, b -> LibraryRepository.naturalCompare(a.fileName, b.fileName) }
    }

    override val pageCount get() = headers.size
    override val pageNames get() = headers.map { it.fileName }
    override val concurrentReads get() = false

    override fun openPage(number: Int): InputStream {
        val header = headers.getOrNull(number - 1) ?: throw IOException("页面不存在：$number")
        return archive.getInputStream(header)
    }

    override fun close() = archive.close()
}

/**
 * 校验物理文件头后解析 USTAR/GNU/PAX；读取时按已验证偏移打开独立流。
 */
private class TarComicArchive(file: File) : ComicArchive {
    private val source = file
    private val records: List<TarRecord> = index(file)

    override val pageCount get() = records.size
    override val pageNames get() = records.map { it.name }

    override fun openPage(number: Int): InputStream {
        val record = records.getOrNull(number - 1) ?: throw IOException("页面不存在：$number")
        val stream = FileInputStream(source)
        try {
            var skipped = 0L
            while (skipped < record.offset) {
                val step = stream.skip(record.offset - skipped)
                if (step <= 0) throw IOException("TAR 条目数据不完整")
                skipped += step
            }
        } catch (failure: Throwable) {
            stream.close()
            throw failure
        }
        return BoundedStream(stream, record.size)
    }

    override fun close() = Unit

    private data class TarRecord(val name: String, val offset: Long, val size: Long)

    private fun index(file: File): List<TarRecord> {
        validateHeaders(file)
        val found = ArrayList<TarRecord>()
        org.apache.commons.compress.archivers.tar.TarArchiveInputStream(file.inputStream()).use { tar ->
            var count = 0
            while (true) {
                val entry = tar.nextEntry ?: break
                if (++count > 20_000) throw IOException("归档条目过多")
                if (!entry.isCheckSumOK || entry.isSparse) throw IOException("TAR 校验失败或使用了不支持的稀疏条目")
                val offset = tar.bytesRead
                if (entry.size < 0 || entry.size > file.length() - offset) throw IOException("TAR 条目不完整")
                if (entry.isFile && ComicArchives.isImageEntry(entry.name)) {
                    found.add(TarRecord(entry.name, offset, entry.size))
                }
            }
        }
        return found.sortedWith { a, b -> LibraryRepository.naturalCompare(a.name, b.name) }
    }

    /** 先验证物理头，限制 GNU/PAX 元数据，避免解析器为恶意长度分配大块内存。 */
    private fun validateHeaders(file: File) = RandomAccessFile(file, "r").use { raf ->
        val length = raf.length()
        val header = ByteArray(512)
        var position = 0L
        var count = 0
        var metadataChain = 0
        while (position < length) {
            if (length - position < 512) throw IOException("TAR 文件头不完整")
            raf.seek(position)
            raf.readFully(header)
            if (header.all { it == 0.toByte() }) return@use
            if (++count > 20_000) throw IOException("归档条目过多")
            val entry = try { org.apache.commons.compress.archivers.tar.TarArchiveEntry(header) }
                catch (failure: IllegalArgumentException) { throw IOException("TAR 文件头无效", failure) }
            if (!entry.isCheckSumOK || entry.isSparse) throw IOException("TAR 文件头校验失败")
            val size = entry.size
            val available = length - position - 512
            if (size < 0 || size > available) throw IOException("TAR 条目长度无效")
            val type = header[156].toInt().toChar()
            if (type in listOf('L', 'K', 'x', 'g')) {
                if (size > 65_536 || ++metadataChain > 16) throw IOException("TAR 扩展元数据过大或嵌套过深")
            } else metadataChain = 0
            val padding = (512 - size % 512) % 512
            if (padding > available - size) throw IOException("TAR 条目填充不完整")
            position += 512 + size + padding
        }
    }
}

private class SingleImageArchive(private val file: File) : ComicArchive {
    override val pageCount = 1
    override val pageNames = listOf(file.name)
    override fun openPage(number: Int): InputStream {
        require(number == 1) { "页面不存在：$number" }
        return FileInputStream(file)
    }
    override fun close() = Unit
}

internal class BoundedStream(private val inner: InputStream, size: Long) : InputStream() {
    private var remaining = size
    override fun read(): Int {
        if (remaining <= 0) return -1
        val value = inner.read()
        if (value >= 0) remaining--
        return value
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0) return -1
        val read = inner.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (read > 0) remaining -= read
        return read
    }
    override fun available(): Int = minOf(inner.available().toLong(), remaining).toInt()
    override fun close() = inner.close()
}
