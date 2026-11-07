package com.example.data.remote

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

/** 仅供本应用使用，统一向现有 SAF 解析器提供只读文档。 */
class WebDavDocumentsProvider internal constructor(private val suppliedRepository: WebDavRepository? = null) : DocumentsProvider() {
    private val repository get() = suppliedRepository ?: WebDavRepository.get(requireNotNull(context))
    override fun onCreate() = true
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(projection ?: arrayOf("root_id", "document_id", "title", "flags"))

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = runBlocking {
        cursor(projection).apply { addEntry(documentId, repository.entry(documentId)) }
    }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor = runBlocking {
        val (id, path) = WebDavRepository.decodeId(parentDocumentId)
        cursor(projection).apply { repository.list(id, path).forEach { entry -> addEntry(WebDavRepository.documentId(id, entry.path), entry) } }
    }

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("WebDAV 仅支持读取")
        return runBlocking {
            val job = coroutineContext[Job]!!
            signal?.setOnCancelListener { job.cancel() }
            try { repository.openDescriptor(documentId) }
            finally { signal?.setOnCancelListener(null) }
        }
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        val (parentId, parentPath) = WebDavRepository.decodeId(parentDocumentId)
        val (id, path) = WebDavRepository.decodeId(documentId)
        return parentId == id && path != parentPath && path.startsWith(if (parentPath.isEmpty()) "" else parentPath.trimEnd('/') + "/")
    }

    private fun cursor(projection: Array<out String>?) = MatrixCursor(projection ?: arrayOf(Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS))

    private fun MatrixCursor.addEntry(id: String, entry: DavEntry) {
        val mime = if (entry.directory) Document.MIME_TYPE_DIR else when (entry.name.substringAfterLast('.', "").lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "avif" -> "image/avif"
            else -> entry.mime
        }
        newRow().apply { columnNames.forEach { column -> add(column, when (column) {
            Document.COLUMN_DOCUMENT_ID -> id
            Document.COLUMN_DISPLAY_NAME -> entry.name
            Document.COLUMN_MIME_TYPE -> mime
            Document.COLUMN_SIZE -> entry.size.takeIf { it >= 0 }
            Document.COLUMN_LAST_MODIFIED -> entry.modifiedAt
            Document.COLUMN_FLAGS -> 0
            "webdav_etag" -> entry.etag
            else -> null
        }) } }
    }
}
