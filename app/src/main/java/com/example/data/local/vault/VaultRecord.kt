package com.example.data.local.vault

import com.asterreader.core.database.*
import org.json.JSONArray
import org.json.JSONObject

internal data class VaultRecord(
    val book: BookEntity,
    val dataName: String,
    val chapters: List<ChapterEntity>,
    val annotations: List<AnnotationEntity>,
    val reader: ReaderStateEntity,
    val pageSizes: List<Long> = emptyList(),
    val cover: String = "",
    val pageNames: List<String> = emptyList(),
    val originalUri: String = ""
) {
    fun encode(): ByteArray {
        val b = book
        val metadata = JSONObject().apply {
            put("id", b.id); put("title", b.title); put("author", b.author); put("series", b.series)
            put("format", b.format); put("page", b.currentPage); put("total", b.totalPages); put("percent", b.progressPercent)
            put("chapter", b.currentChapterTitle); put("badge", b.statusBadge); put("seriesBadge", b.seriesBadge)
            put("sizeText", b.fileSizeText); put("updated", b.updatedTimeText); put("synopsis", b.synopsis)
            put("tags", b.tagsCsv); put("shelves", b.shelfIdsCsv); put("favorite", b.isFavorite)
            put("lastRead", b.lastReadTimestamp); put("size", b.sizeBytes); put("added", b.addedAt)
            put("coverPage", b.coverPage); put("order", b.seriesOrder)
        }
        val r = reader
        return JSONObject().apply {
            put("version", 2); put("book", metadata); put("data", dataName); put("cover", cover)
            put("pages", JSONArray(pageSizes))
            put("pageNames", JSONArray(pageNames))
            put("originalUri", originalUri)
            put("chapters", JSONArray(chapters.map { c -> JSONObject().apply {
                put("id", c.id); put("number", c.chapterNumber); put("title", c.title)
                put("start", c.startPage); put("end", c.endPage); put("done", c.isCompleted); put("current", c.isCurrent)
            } }))
            put("annotations", JSONArray(annotations.map { a -> JSONObject().apply {
                put("id", a.id); put("chapter", a.chapterTitle); put("page", a.pageNumber); put("quote", a.quoteText)
                put("note", a.noteText); put("color", a.colorName); put("type", a.type); put("created", a.createdAtText)
            } }))
            put("reader", JSONObject().apply {
                put("page", r.page); put("offset", r.offsetFraction); put("scale", r.scale); put("x", r.panX); put("y", r.panY)
                put("mode", r.layoutMode); put("fit", r.fitMode); put("cover", r.coverSingle); put("rtl", r.rightToLeft)
            })
        }.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        const val MAX_METADATA = 8 * 1024 * 1024
        fun decode(bytes: ByteArray): VaultRecord {
            require(bytes.size <= MAX_METADATA) { "加密元数据过大" }
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            require(root.getInt("version") == 2)
            val b = root.getJSONObject("book")
            val id = b.getString("id")
            require(id.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "作品标识无效" }
            val name = root.getString("data")
            require(name.matches(Regex("[0-9a-f-]{36}\\.avd"))) { "密文引用无效" }
            val total = b.getInt("total")
            require(total in 1..100000)
            require(b.getString("format") in setOf("CBZ", "ZIP", "CBR", "RAR", "CB7", "7Z", "CBT", "TAR", "EPUB", "PDF", "IMAGE", "IMAGE_DIR"))
            val book = BookEntity(id, b.getString("title"), b.getString("author"), b.getString("series"), b.getString("format"),
                "vault-cover://$id", currentPage = b.getInt("page").coerceIn(1, total), totalPages = total,
                progressPercent = b.getInt("percent").coerceIn(0, 100), currentChapterTitle = b.getString("chapter"),
                statusBadge = b.getString("badge"), seriesBadge = b.getString("seriesBadge"), fileSizeText = b.getString("sizeText"),
                filePath = "vault://$id", repositoryName = "保险库", updatedTimeText = b.getString("updated"), synopsis = b.getString("synopsis"),
                tagsCsv = b.getString("tags"), shelfIdsCsv = b.getString("shelves"), isFavorite = b.getBoolean("favorite"),
                isVaultProtected = true, isVaultEncrypted = true, lastReadTimestamp = b.getLong("lastRead"), sizeBytes = b.getLong("size"),
                addedAt = b.getLong("added"), coverPage = b.getInt("coverPage"), seriesOrder = b.getInt("order"))
            val r = root.getJSONObject("reader")
            require(listOf("offset", "scale", "x", "y").all { r.getDouble(it).isFinite() })
            fun optional(key: String) = if (r.isNull(key)) null else r.getString(key)
            val reader = ReaderStateEntity(id, r.getInt("page").coerceIn(1, total), r.getDouble("offset").toFloat().coerceIn(0f, 1f),
                r.getDouble("scale").toFloat().coerceIn(1f, 8f), r.getDouble("x").toFloat(), r.getDouble("y").toFloat(),
                optional("mode"), optional("fit"), if (r.isNull("cover")) null else r.getBoolean("cover"),
                if (r.isNull("rtl")) null else r.getBoolean("rtl"))
            val chapters = root.getJSONArray("chapters").objects().map { c -> ChapterEntity(c.getString("id"), id,
                c.getInt("number"), c.getString("title"), c.getInt("start"), c.getInt("end"), c.getBoolean("done"), c.getBoolean("current")) }
            val annotations = root.getJSONArray("annotations").objects().map { a -> AnnotationEntity(a.getInt("id"), id,
                a.getString("chapter"), a.getInt("page"), a.getString("quote"), a.getString("note"), a.getString("color"),
                a.getString("type"), a.getString("created")) }
            val pages = root.getJSONArray("pages").let { a -> (0 until a.length()).map { a.getLong(it) } }
            require(pages.all { it in 1..512L * 1024 * 1024 } && (pages.isEmpty() || pages.size == total))
            val names = root.optJSONArray("pageNames")?.let { a ->
                require(a.length() <= 100000); (0 until a.length()).map { a.getString(it) }
            }.orEmpty()
            return VaultRecord(book, name, chapters, annotations, reader, pages, root.getString("cover"), names, root.optString("originalUri"))
        }

        private fun JSONArray.objects(): List<JSONObject> {
            require(length() <= 100000)
            return (0 until length()).map(::getJSONObject)
        }
    }
}
