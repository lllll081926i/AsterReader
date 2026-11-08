package com.example

import com.github.junrar.Archive
import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RarSecurityRegressionTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun fixture(name: String): File = temporary.newFile(name).apply {
        writeBytes(requireNotNull(RarSecurityRegressionTest::class.java.getResourceAsStream("/rar/$name")).use { it.readBytes() })
    }

    @Test fun storedRarCanBeReadRepeatedlyAndClosedEarly() {
        Archive(fixture("stored.rar")).use { archive ->
            val header = archive.fileHeaders.first { !it.isDirectory }
            val expected = archive.getInputStream(header).use { it.readBytes() }
            assertTrue(expected.isNotEmpty())
            archive.getInputStream(header).use { assertTrue(it.read() >= 0) }
            assertArrayEquals(expected, archive.getInputStream(header).use { it.readBytes() })
        }
    }

    @Test fun crcFailureReachesStreamConsumer() {
        val file = fixture("stored.rar")
        val bytes = file.readBytes()
        assertEquals('u'.code.toByte(), bytes[60])
        bytes[60] = 'U'.code.toByte()
        file.writeBytes(bytes)
        Archive(file).use { archive ->
            assertThrows(IOException::class.java) {
                archive.getInputStream(archive.fileHeaders.first()).use { it.readBytes() }
            }
        }
    }

    @Test(timeout = 10_000) fun maliciousVmFilterTerminatesWithoutGenericInterpreter() {
        Archive(fixture("vm-trigger.rar")).use { archive ->
            archive.fileHeaders.filterNot { it.isDirectory }.forEach { header ->
                archive.getInputStream(header).use { it.copyTo(java.io.OutputStream.nullOutputStream()) }
            }
        }
    }

    @Test fun extendedTimeHeadersAreReadable() {
        Archive(fixture("extended-time.rar")).use { archive ->
            assertTrue(archive.fileHeaders.isNotEmpty())
            archive.fileHeaders.filterNot { it.isDirectory }.forEach { header ->
                archive.getInputStream(header).use { it.readBytes() }
            }
        }
    }
}
