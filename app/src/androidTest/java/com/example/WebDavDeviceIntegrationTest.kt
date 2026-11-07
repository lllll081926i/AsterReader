package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.ComicArchives
import com.example.data.remote.WebDavAccounts
import com.example.data.remote.WebDavDocumentsProvider
import com.example.data.remote.WebDavRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.ServerSocket
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

/** 仅使用独立目录、测试密钥与本机只读服务器，不读写用户连接或漫画。 */
@RunWith(AndroidJUnit4::class)
class WebDavDeviceIntegrationTest {
    @Test fun realKeystoreAndNetworkReadProduceSeekableComicAndReusableCache() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(app.cacheDir, "webdav-device-test-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : ContextWrapper(app) {
            override fun getCacheDir() = File(directory, "cache").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(directory, "private").apply { mkdirs() }
            override fun getApplicationContext(): Context = this
        }
        val alias = "aster.webdav.test.${UUID.randomUUID()}"
        val bitmap = Bitmap.createBitmap(12, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        bitmap.recycle()
        val archive = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip ->
            repeat(2) { zip.putNextEntry(ZipEntry("${it + 1}.png")); zip.write(png); zip.closeEntry() }
        } }.toByteArray()
        val socket = ServerSocket(0)
        val running = AtomicBoolean(true)
        val gets = AtomicInteger()
        val server = thread(name = "webdav-device-fixture", isDaemon = true) {
            while (running.get()) try {
                socket.accept().use { connection ->
                    connection.soTimeout = 5000
                    val reader = connection.getInputStream().bufferedReader()
                    val method = reader.readLine()?.substringBefore(' ').orEmpty()
                    var authorization = ""
                    var headerBytes = 0
                    while (true) {
                        val header = reader.readLine() ?: break
                        if (header.isEmpty()) break
                        headerBytes += header.length
                        check(headerBytes < 32768)
                        if (header.startsWith("Authorization:", true)) authorization = header.substringAfter(':').trim()
                    }
                    val authorized = authorization == "Basic " + java.util.Base64.getEncoder().encodeToString("reader:fixture-only".toByteArray())
                    val status = if (!authorized) "401 Unauthorized" else if (method == "PROPFIND") "207 Multi-Status" else "200 OK"
                    val body = if (!authorized) ByteArray(0) else if (method == "PROPFIND") {
                        """<d:multistatus xmlns:d="DAV:"><d:response><d:href>/dav/book.cbz</d:href><d:propstat><d:prop><d:getcontentlength>${archive.size}</d:getcontentlength><d:getetag>"fixture-v1"</d:getetag></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response></d:multistatus>""".toByteArray()
                    } else { gets.incrementAndGet(); archive }
                    connection.getOutputStream().apply {
                        write("HTTP/1.1 $status\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(body); flush()
                    }
                }
            } catch (_: java.io.IOException) { if (running.get()) throw AssertionError("测试服务器读取失败") }
        }
        try {
            val store = WebDavAccounts(isolated, alias = alias)
            val remote = WebDavRepository(isolated, store)
            val account = remote.save(null, "设备测试", "http://127.0.0.1:${socket.localPort}/dav/", "reader", "fixture-only".toCharArray(), true, true)
            assertEquals(account, WebDavRepository(isolated, WebDavAccounts(isolated, alias = alias)).connections().single())
            assertFalse(String(File(isolated.noBackupFilesDir, "webdav-accounts.gcm").readBytes()).contains("fixture-only"))
            val entry = remote.list(account.id, "").single()
            val id = WebDavRepository.documentId(account.id, entry.path)
            val provider = WebDavDocumentsProvider(remote)
            provider.attachInfo(isolated, ProviderInfo().apply { authority = WebDavRepository.AUTHORITY; exported = true; grantUriPermissions = true
                readPermission = "android.permission.MANAGE_DOCUMENTS"; writePermission = readPermission })
            provider.openDocument(id, "r", null).use { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(descriptor.fileDescriptor)).use { assertArrayEquals(archive, it.readBytes()) }
            }
            val downloaded = remote.open(id)
            ComicArchives.open(downloaded, "CBZ").use { comic ->
                assertEquals(2, comic.pageCount); assertArrayEquals(png, comic.openPage(2).use { it.readBytes() })
            }
            assertEquals(1, gets.get())
            remote.remove(account.id)
            assertTrue(remote.connections().isEmpty())
            assertTrue(File(isolated.cacheDir, "webdav").listFiles().orEmpty().isEmpty())
        } finally {
            running.set(false); socket.close(); server.join(5000)
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
            directory.deleteRecursively()
        }
    }
}
