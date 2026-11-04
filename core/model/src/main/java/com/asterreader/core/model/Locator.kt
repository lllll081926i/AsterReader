package com.asterreader.core.model

data class Locator(
    val publicationId: String,
    val resourceId: String? = null,
    val progression: Double? = null,
    val position: Long? = null,
    val textAnchor: TextAnchor? = null,
    val imagePageId: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

data class TextAnchor(
    val selector: String? = null,
    val before: String? = null,
    val exact: String? = null,
    val after: String? = null,
)

enum class PublicationFormat(val extension: String, val mimeType: String) {
    CBZ("cbz", "application/vnd.comicbook+zip"),
    CBR("cbr", "application/vnd.comicbook-rar"),
    EPUB("epub", "application/epub+zip"),
    PDF("pdf", "application/pdf"),
    TXT("txt", "text/plain"),
    MD("md", "text/markdown"),
    MOBI("mobi", "application/x-mobipocket-ebook"),
    FB2("fb2", "application/x-fictionbook+xml"),
    IMAGE_DIR("", "application/x-directory")
}

data class SourceEntry(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModifiedEpochMs: Long,
)
