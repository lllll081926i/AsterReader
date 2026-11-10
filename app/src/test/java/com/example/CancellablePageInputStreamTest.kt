package com.example

import com.example.data.local.CancellablePageInputStream
import java.io.ByteArrayInputStream
import java.io.InterruptedIOException
import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Test

class CancellablePageInputStreamTest {
    @Test fun cancelledPageStopsAllReadsAndStillClosesSource() {
        val job = Job()
        var reads = 0
        var closed = false
        val source = object : ByteArrayInputStream(ByteArray(100)) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                reads++
                return super.read(buffer, offset, length)
            }
            override fun close() { closed = true; super.close() }
        }
        val input = CancellablePageInputStream(source, job)
        assertEquals(8, input.read(ByteArray(8)))
        job.cancel()
        assertThrows(InterruptedIOException::class.java) { input.read() }
        assertThrows(InterruptedIOException::class.java) { input.read(ByteArray(8)) }
        assertThrows(InterruptedIOException::class.java) { input.skip(8) }
        assertEquals(1, reads)
        input.close()
        assertTrue(closed)
    }
}
