package com.example

import android.content.*
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AsterDatabase
import com.example.data.local.LibraryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first

/** PDF 原生解码必须在 Android 运行时验证；测试资产与用户书库隔离。 */
@RunWith(AndroidJUnit4::class)
class PdfScanDeviceTest {
    @Test fun pdfIndexDefersCoverUntilRequested() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "pdf-scan-${UUID.randomUUID()}").apply { mkdirs() }
        val source = File(directory, "漫画.pdf")
        val pdf = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        listOf("<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Count 3 /Kids [3 0 R 4 0 R 5 0 R] >>",
            *Array(3) { "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 450] /Resources << >> /Contents 6 0 R >>" },
            "<< /Length 0 >>\nstream\n\nendstream").forEachIndexed { index, body ->
            offsets.add(pdf.length)
            pdf.append("${index + 1} 0 obj\n$body\nendobj\n")
        }
        val xref = pdf.length
        pdf.append("xref\n0 7\n0000000000 65535 f \n")
        offsets.drop(1).forEach { pdf.append(it.toString().padStart(10, '0')).append(" 00000 n \n") }
        pdf.append("trailer\n<< /Size 7 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        source.writeBytes(pdf.toString().toByteArray(Charsets.US_ASCII))
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun getType(uri: Uri) = "application/pdf"
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
                if (uri.lastPathSegment == "children") return MatrixCursor(arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME)).apply {
                    addRow(arrayOf("book", "application/pdf", source.name))
                }
                return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED)).apply {
                    addRow(arrayOf<Any>(source.name, source.length(), source.lastModified()))
                }
            }
            override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
            override fun insert(uri: Uri, values: ContentValues?): Uri? = error("只读测试来源")
            override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = error("只读测试来源")
            override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = error("只读测试来源")
        }
        provider.attachInfo(app, ProviderInfo().apply { authority = "local.direct.test" })
        val resolver = ContentResolver.wrap(provider)
        val isolated = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getContentResolver() = resolver
            override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(directory, "private").apply { mkdirs() }
        }
        val db = Room.inMemoryDatabaseBuilder(app, AsterDatabase::class.java).build()
        val repository = LibraryRepository(isolated, db.asterDao())
        try {
            val book = repository.importDocument(Uri.parse("content://local.direct.test/book"),
                persist = false, includePdfCover = false)
            assertEquals(3, book.totalPages)
            assertTrue(book.coverUrl.isBlank())
            assertTrue(repository.renderPage(book.id, 1, 600_000).width > 0)
            assertEquals(1, repository.refreshMissingCovers())
            val updated = repository.observeAllBooks().first().single()
            assertTrue(File(updated.coverUrl).length() > 0)
        } finally {
            repository.closeReadSession()
            db.close()
        }
    }
}
