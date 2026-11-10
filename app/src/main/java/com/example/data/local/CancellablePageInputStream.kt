package com.example.data.local

import java.io.FilterInputStream
import java.io.InputStream
import java.io.InterruptedIOException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.isActive

/** 取消翻页任务后，在下一次读取压缩流时停止，释放解码线程。 */
internal class CancellablePageInputStream(input: InputStream, private val context: CoroutineContext) : FilterInputStream(input) {
    private fun checkActive() {
        if (!context.isActive) throw InterruptedIOException("页面读取已取消")
    }

    override fun read(): Int { checkActive(); return `in`.read() }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        checkActive()
        return `in`.read(buffer, offset, length)
    }
    override fun skip(count: Long): Long { checkActive(); return `in`.skip(count) }
}
