package com.asterreader.core.model

data class Locator(
    val publicationId: String,
    val resourceId: String? = null,
    val progression: Double? = null,
    val position: Long? = null,
    val imagePageId: String? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

enum class PublicationFormat(val extension: String, val mimeType: String) {
    CBZ("cbz", "application/vnd.comicbook+zip"),
    ZIP("zip", "application/zip"),
    CBR("cbr", "application/vnd.comicbook-rar"),
    RAR("rar", "application/x-rar-compressed"),
    CB7("cb7", "application/x-cb7-compressed"),
    SEVEN_ZIP("7z", "application/x-7z-compressed"),
    CBT("cbt", "application/x-tar"),
    TAR("tar", "application/x-tar"),
    PDF("pdf", "application/pdf"),
    EPUB("epub", "application/epub+zip"),
    MOBI("mobi", "application/x-mobipocket-ebook"),
    IMAGE_DIR("", "application/x-directory")
}

data class SourceEntry(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModifiedEpochMs: Long,
)
