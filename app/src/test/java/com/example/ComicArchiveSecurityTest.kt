package com.example

import com.example.data.local.ComicArchives
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
