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
    CBR("cbr", "application/vnd.comicbook-rar"),
    CB7("cb7", "application/x-7z-compressed"),
    PDF("pdf", "application/pdf"),
    IMAGE_DIR("", "application/x-directory")
}

data class SourceEntry(
    val path: String,
    val name: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModifiedEpochMs: Long,
)
