package com.example

import android.app.Application
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.request.Options
import com.example.data.local.vault.FileVaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AsterApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader() = ImageLoader.Builder(this).components {
        add(object : Fetcher.Factory<Uri> {
            override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
                if (data.scheme != "vault-cover") return null
                return Fetcher {
                    withContext(Dispatchers.IO) {
                        val store = FileVaultStore.get(this@AsterApplication)
                        val token = store.sessionToken()
                        val record = store.records.value[data.host] ?: error("保险库已锁定")
                        val bytes = android.util.Base64.decode(record.cover, android.util.Base64.NO_WRAP)
                        try {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "封面不可用" }
                            var sample = 1
                            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: error("封面不可用")
                            if (!store.isSessionValid(token)) { bitmap.recycle(); error("保险库已锁定") }
                            DrawableResult(BitmapDrawable(resources, bitmap), false, DataSource.MEMORY)
                        } finally { bytes.fill(0) }
                    }
                }
            }
        })
    }.build()
}
