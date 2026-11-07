package com.example

import android.os.Bundle
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.asterreader.core.vault.VaultCryptography
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** 固定合成工作量；不读取用户文件，不把 debug 耗时当作正式版基准。 */
@RunWith(AndroidJUnit4::class)
class VaultReadPerformanceDeviceTest {
    @Test fun sequentialSmallReadsRemainCorrect() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File.createTempFile("vault-perf-", ".cipher", instrumentation.targetContext.cacheDir)
        val key = VaultCryptography.randomKey()
        val plain = ByteArray(VaultCryptography.CHUNK_SIZE * 2) { (it % 251).toByte() }
        try {
            VaultCryptography.write(file, key, plain.size.toLong(), plain.inputStream())
            val rounds = (0..3).map {
                VaultCryptography.Reader(file, key).use { reader ->
                    val started = SystemClock.elapsedRealtimeNanos()
                    repeat(256) { block ->
                        val position = block * 8192
                        val bytes = reader.read(position.toLong(), 8192)
                        assertEquals(plain[position], bytes.first())
                        assertEquals(plain[position + 8191], bytes.last())
                        bytes.fill(0)
                    }
                    (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0
                }
            }
            instrumentation.sendStatus(0, Bundle().apply {
                putString("stream", "\n合成密文顺序读取：2 MiB / 256 次 8 KiB；毫秒（首轮预热）：$rounds\n")
            })
        } finally { key.fill(0); plain.fill(0); file.delete() }
    }
}
