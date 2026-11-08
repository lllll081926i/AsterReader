package com.example

import com.example.data.local.ComicArchives
import com.example.data.local.EpubXmlBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 归档层的安全与格式边界：XXE、Zip Slip、spine 顺序、未知格式。 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComicArchiveSecurityTest {
    @Test fun epubXmlBudgetsRejectExpansionDepthAndNodeFloods() {
        val hostile = listOf(
            "<root>" + "x".repeat(2 * 1024 * 1024) + "</root>",
            "<n>".repeat(65) + "</n>".repeat(65),
            "<root>" + "<n/>".repeat(20_001) + "</root>"
        )
        hostile.forEach { xml ->
            org.junit.Assert.assertThrows(IOException::class.java) {
                EpubXmlBudget().read(xml.byteInputStream())
            }
            val file = epub(mapOf("META-INF/container.xml" to xml, "page.jpg" to "image"))
            try {
                ComicArchives.open(file, "EPUB").use { assertEquals(listOf("page.jpg"), it.pageNames) }
            } finally { file.delete() }
        }
    }

    @Test fun epubXmlBudgetIsSharedAcrossDocuments() {
        val budget = EpubXmlBudget()
        val xml = ("<root>" + "<n/>".repeat(19_999) + "</root>").byteInputStream().readBytes()
        repeat(5) { budget.read(xml.inputStream()) }
        org.junit.Assert.assertThrows(IOException::class.java) { budget.read(xml.inputStream()) }
    }

    private fun tar(name: String = "1.jpg", bytes: ByteArray = byteArrayOf(1, 2, 3, 4)): File {
        val file = File.createTempFile("aster-tar-", ".tar")
        org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(file.outputStream()).use { output ->
            output.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX)
            val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry(name).apply { size = bytes.size.toLong() }
            output.putArchiveEntry(entry)
            output.write(bytes)
            output.closeArchiveEntry()
        }
        return file
    }

    @Test fun tarPaxLongNameKeepsTheCorrectDataOffset() {
        val name = "folder/".repeat(30) + "1.jpg"
        val file = tar(name)
        try {
            ComicArchives.open(file, "TAR").use { archive ->
                assertEquals(listOf(name), archive.pageNames)
                org.junit.Assert.assertArrayEquals(byteArrayOf(1, 2, 3, 4), archive.openPage(1).use { it.readBytes() })
            }
        } finally { file.delete() }
    }

    @Test(expected = IOException::class) fun tarBadChecksumIsRejected() {
        val file = tar()
        try {
            java.io.RandomAccessFile(file, "rw").use { it.writeByte(99) }
            ComicArchives.open(file, "TAR").close()
        } finally { file.delete() }
    }

    @Test(expected = IOException::class) fun tarTruncatedPayloadIsRejected() {
        val file = tar()
        try {
            java.io.RandomAccessFile(file, "rw").use { it.setLength(514) }
            ComicArchives.open(file, "TAR").close()
        } finally { file.delete() }
    }

    @Test(expected = IOException::class) fun tarOversizedMetadataIsRejectedBeforeParsing() {
        val file = File.createTempFile("aster-tar-metadata-", ".tar")
        try {
            org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(file.outputStream()).use { output ->
                val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry("././@LongLink", 'L'.code.toByte())
                    .apply { size = 65_537 }
                output.putArchiveEntry(entry)
                output.write(ByteArray(65_537))
                output.closeArchiveEntry()
            }
            ComicArchives.open(file, "TAR").close()
        } finally { file.delete() }
    }

    @Test(expected = IOException::class) fun tarDeepMetadataChainIsRejectedBeforeRecursion() {
        val file = File.createTempFile("aster-tar-chain-", ".tar")
        try {
            org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(file.outputStream()).use { output ->
                repeat(17) {
                    val entry = org.apache.commons.compress.archivers.tar.TarArchiveEntry("././@LongLink", 'L'.code.toByte())
                        .apply { size = 2 }
                    output.putArchiveEntry(entry)
                    output.write(byteArrayOf(97, 0))
                    output.closeArchiveEntry()
                }
            }
            ComicArchives.open(file, "TAR").close()
        } finally { file.delete() }
    }

    private fun epub(entries: Map<String, String>): File {
        val file = File.createTempFile("aster-epub-", ".epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return file
    }

    @Test
    fun doctypeContainerFallsBackToPlainZipInsteadOfResolvingExternalEntities() {
        val file = epub(
            mapOf(
                "META-INF/container.xml" to
                    "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/hosts\">]>" +
                    "<container><rootfiles><rootfile full-path=\"OEBPS/content.opf\"/></rootfiles></container>",
                "OEBPS/p001.jpg" to "fake-image-bytes"
            )
        )
        try {
            val archive = ComicArchives.open(file, "EPUB")
            assertEquals(1, archive.pageCount)
            assertTrue(archive.pageNames.first().endsWith("p001.jpg"))
            archive.openPage(1).use { stream ->
                assertEquals("fake-image-bytes", String(stream.readBytes(), Charsets.UTF_8))
            }
            archive.close()
        } finally {
            file.delete()
        }
    }

    @Test
    fun spineOrderWinsOverFileNameOrderAndOutsidePathsAreIgnored() {
        val container = "<container xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">" +
            "<rootfiles><rootfile full-path=\"OEBPS/content.opf\"/></rootfiles></container>"
        val opf = "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\">" +
            "<metadata/><manifest>" +
            "<item id=\"z\" href=\"images/z.jpg\" media-type=\"image/jpeg\"/>" +
            "<item id=\"a\" href=\"images/a.jpg\" media-type=\"image/jpeg\"/>" +
            "<item id=\"evil\" href=\"../../outside.jpg\" media-type=\"image/jpeg\"/>" +
            "</manifest><spine>" +
            "<itemref idref=\"a\"/><itemref idref=\"evil\"/><itemref idref=\"z\"/>" +
            "</spine></package>"
        val file = epub(
            mapOf(
                "META-INF/container.xml" to container,
                "OEBPS/content.opf" to opf,
                "OEBPS/images/a.jpg" to "a",
                "OEBPS/images/z.jpg" to "z"
            )
        )
        try {
            val archive = ComicArchives.open(file, "EPUB")
            assertEquals(listOf("OEBPS/images/a.jpg", "OEBPS/images/z.jpg"), archive.pageNames)
            archive.close()
        } finally {
            file.delete()
        }
    }

    @Test(expected = IOException::class)
    fun unknownContainerIsRejected() {
        val file = epub(mapOf("a.jpg" to "x"))
        try {
            ComicArchives.open(file, "NOT_A_FORMAT").close()
        } finally {
            file.delete()
        }
    }
}
