package com.example

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.asterreader.core.database.BookEntity
import com.asterreader.core.database.ReaderStateEntity
import com.example.data.local.vault.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** 独立测试目录及独立 Keystore 别名，不访问用户保险库和数据库。 */
@RunWith(AndroidJUnit4::class)
class VaultDeviceIntegrationTest {
    @Test fun hardwareSlotProxyZipPdfAndCrossDeviceExportRoundtrip() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "vault-device-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(app) {
            override fun getNoBackupFilesDir() = directory
            override fun getApplicationContext(): Context = this
        }
        val prefix = "aster.test.${UUID.randomUUID()}"
        val store = FileVaultStore.forTest(isolated, AndroidVaultDeviceKey(prefix))
        val password = "tU8-Vm3_Xy6-Qr9_eK2".toCharArray()
        try {
            store.open(password, true)
            val bitmap = Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
            val image = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            bitmap.recycle()
            val zipBytes = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use { zip ->
                repeat(3) { zip.putNextEntry(ZipEntry("${it + 1}.png")); zip.write(image); zip.closeEntry() }
            } }.toByteArray()
            val saved = store.create(record("CBZ", 3), zipBytes.size.toLong(), { zipBytes.inputStream() })
            VaultComicArchive(store, saved).use { archive ->
                assertEquals(3, archive.pageCount); assertArrayEquals(image, archive.openPage(2).use { it.readBytes() })
            }
            val tarBytes = ByteArrayOutputStream().also { out ->
                org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(out).use { tar ->
                    val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry("1.png").apply { size = image.size.toLong() }
                    tar.putArchiveEntry(entry); tar.write(image); tar.closeArchiveEntry()
                }
            }.toByteArray()
            val channel = org.apache.commons.compress.utils.SeekableInMemoryByteChannel()
            val sevenOutput = org.apache.commons.compress.archivers.sevenz.SevenZOutputFile(channel)
            sevenOutput.putArchiveEntry(org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry().apply {
                name = "1.png"; size = image.size.toLong()
            })
            sevenOutput.write(image); sevenOutput.closeArchiveEntry(); sevenOutput.finish()
            val sevenBytes = channel.array().copyOf(channel.size().toInt())
            sevenOutput.close()
            for ((format, bytes) in listOf("TAR" to tarBytes, "7Z" to sevenBytes, "RAR" to storedRar(image))) {
                val encrypted = store.create(record(format, 1), bytes.size.toLong(), { bytes.inputStream() })
                VaultComicArchive(store, encrypted).use { archive ->
                    assertEquals(format, 1, archive.pageCount)
                    assertArrayEquals(format, image, archive.openPage(1).use { it.readBytes() })
                }
            }
            val pdf = ByteArrayOutputStream()
            val document = PdfDocument()
            try {
                val page = document.startPage(PdfDocument.PageInfo.Builder(200, 300, 1).create())
                page.canvas.drawColor(Color.RED); document.finishPage(page); document.writeTo(pdf)
            } finally { document.close() }
            val pdfBytes = pdf.toByteArray()
            val savedPdf = store.create(record("PDF", 1), pdfBytes.size.toLong(), { pdfBytes.inputStream() })
            VaultProxySource(app, store, savedPdf.book.id).use { proxy ->
                PdfRenderer(ParcelFileDescriptor.dup(proxy.descriptor.fileDescriptor)).use { renderer ->
                    assertEquals(1, renderer.pageCount)
                    val rendered = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888)
                    renderer.openPage(0).use { it.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                    assertEquals(Color.RED, rendered.getPixel(50, 75)); rendered.recycle()
                }
            }
            val exported = ByteArrayOutputStream()
            store.export(saved.book.id, password, exported)
            store.lock()
            assertTrue(store.records.value.isEmpty())
            store.open(password, false)
            val restored = store.restore(exported.toByteArray().inputStream(), password)
            store.openReader(restored.book.id).use { assertArrayEquals(zipBytes, it.stream().readBytes()) }
            assertTrue(directory.walkTopDown().filter { it.isFile }.none { it.readBytes().toString(Charsets.UTF_8).contains("设备测试私密标题") })
            store.lock()
            repeat(5) { assertThrows(Exception::class.java) { store.open("wrong-vault-secret".toCharArray(), false) } }
            assertThrows(VaultCooldownException::class.java) { store.open(password, false) }
        } finally {
            store.lock(); password.fill('\u0000')
            val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            listOf("$prefix.device.v2", "$prefix.source.v2").forEach { keys.deleteEntry(it) }
            // 仅回收本测试生成的临时目录。
            directory.deleteRecursively()
        }
    }

    /** 使用自有测试图构造 RAR4 的存储条目，不依赖外部版权样本。 */
    private fun storedRar(image: ByteArray): ByteArray {
        fun header(body: ByteArray): ByteArray {
            val crc = java.util.zip.CRC32().apply { update(body) }.value.toInt()
            return java.nio.ByteBuffer.allocate(body.size + 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort(crc.toShort()).put(body).array()
        }
        val main = java.nio.ByteBuffer.allocate(11).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put(0x73).putShort(0).putShort(13).putShort(0).putInt(0).array()
        val name = "1.png".toByteArray()
        val crc = java.util.zip.CRC32().apply { update(image) }.value.toInt()
        val file = java.nio.ByteBuffer.allocate(30 + name.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put(0x74).putShort(0x8000.toShort()).putShort((32 + name.size).toShort())
            .putInt(image.size).putInt(image.size).put(2).putInt(crc).putInt(0).put(20).put(0x30)
            .putShort(name.size.toShort()).putInt(0x20).put(name).array()
        val end = java.nio.ByteBuffer.allocate(5).order(java.nio.ByteOrder.LITTLE_ENDIAN).put(0x7b).putShort(0x4000).putShort(7).array()
        return byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0) + header(main) + header(file) + image + header(end)
    }

    private fun record(format: String, pages: Int): VaultRecord {
        val id = UUID.randomUUID().toString()
        val book = BookEntity(id, "设备测试私密标题", "", "", format, "", currentPage = 1, totalPages = pages,
            progressPercent = 0, currentChapterTitle = "", statusBadge = "未读", seriesBadge = "", fileSizeText = "",
            filePath = "", repositoryName = "", updatedTimeText = "", synopsis = "", tagsCsv = "", shelfIdsCsv = "")
        return VaultRecord(book, "", emptyList(), emptyList(), ReaderStateEntity(id))
    }
}
