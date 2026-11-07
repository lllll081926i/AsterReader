package com.example.data.local.vault

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.system.ErrnoException
import android.system.OsConstants
import com.asterreader.core.vault.VaultCryptography
import java.io.Closeable

/** PDF 使用代理只读描述符在内存中解密，不创建明文文件。 */
internal class VaultProxySource(context: Context, private val store: FileVaultStore, id: String) : Closeable {
    private val reader = store.openReader(id)
    private val thread = HandlerThread("vault-read").apply { start() }
    val descriptor: ParcelFileDescriptor
    init {
        try {
            descriptor = context.getSystemService(StorageManager::class.java).openProxyFileDescriptor(
                ParcelFileDescriptor.MODE_READ_ONLY,
                object : ProxyFileDescriptorCallback() {
                    override fun onGetSize() = reader.length
                    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
                        try {
                            require(offset >= 0)
                            if (offset >= reader.length) return 0
                            val count = minOf(size.toLong(), reader.length - offset, VaultCryptography.CHUNK_SIZE.toLong()).toInt()
                            val bytes = reader.read(offset, count)
                            try { bytes.copyInto(data) } finally { bytes.fill(0) }
                            return count
                        } catch (_: Exception) { data.fill(0); throw ErrnoException("vault-read", OsConstants.EIO) }
                    }
                    override fun onRelease() { store.release(reader); thread.quitSafely() }
                }, Handler(thread.looper))
        } catch (failure: Throwable) { store.release(reader); thread.quitSafely(); throw failure }
    }
    override fun close() { descriptor.close(); store.release(reader); thread.quitSafely() }
}
