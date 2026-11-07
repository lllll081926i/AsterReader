package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

data class DavConnection(val id: String, val name: String, val address: String, val username: String, val allowHttp: Boolean)

class WebDavRepository internal constructor(private val context: Context,
    private val accounts: WebDavAccounts = WebDavAccounts(context), suppliedClient: WebDavClient? = null) {
    private val client by lazy { suppliedClient ?: WebDavClient() }
    private val cacheMutex = Mutex()
    private val directory get() = File(context.cacheDir, "webdav").apply { mkdirs() }
    private val entries = object : LinkedHashMap<String, DavEntry>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DavEntry>?) = size > 10000
    }

    suspend fun connections(): List<DavConnection> = withContext(Dispatchers.IO) { accounts.read().map { it.summary() } }

    suspend fun save(id: String?, name: String, address: String, username: String, password: CharArray, allowHttp: Boolean, persist: Boolean, keepPassword: Boolean = false): DavConnection = withContext(Dispatchers.IO) {
        try {
            require(name.trim().length in 1..80 && username.length <= 512 && password.size <= 4096) { "请填写名称，账号或密码过长" }
            require(address.length <= 4096 && !username.contains(':')) { "地址过长或用户名包含不支持的冒号" }
            val old = id?.let { account(it) }
            val root = WebDavClient.root(address, allowHttp)
            require(!keepPassword || old == null || old.address == root.toString()) { "更换地址后请重新输入密码" }
            val candidate = DavAccount(old?.id ?: UUID.randomUUID().toString(), name.trim(), root.toString(), username,
                if (keepPassword && old != null) old.password else String(password), allowHttp)
            client.list(candidate, "")
            currentCoroutineContext().ensureActive()
            if (persist) {
                cacheMutex.withLock {
                    accounts.save(candidate)
                    directory.listFiles()?.filter { it.name.startsWith("${candidate.id}-") }?.forEach { it.delete() }
                    synchronized(entries) { entries.keys.removeAll { it.startsWith("${candidate.id}:") } }
                }
            }
            candidate.summary()
        } finally { password.fill('\u0000') }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            accounts.remove(id)
            directory.listFiles()?.filter { it.name.startsWith("$id-") }?.forEach { it.delete() }
            synchronized(entries) { entries.keys.removeAll { it.startsWith("$id:") } }
        }
    }

    suspend fun list(id: String, path: String): List<DavEntry> = withContext(Dispatchers.IO) {
        val result = client.list(account(id), path).map { entry ->
            if (!entry.directory && entry.etag.isBlank() && entry.modifiedAt <= 0)
                entry.copy(revision = "refresh:${UUID.randomUUID()}") else entry
        }
        synchronized(entries) { result.forEach { entries[documentId(id, it.path)] = it } }
        result
    }

    internal suspend fun entry(documentId: String): DavEntry {
        synchronized(entries) { entries[documentId] }?.let { return it }
        val (id, path) = decodeId(documentId)
        if (path.isEmpty()) return DavEntry("", account(id).name, true, mime = DocumentsContract.Document.MIME_TYPE_DIR)
        val parent = path.trimEnd('/').substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }
        return list(id, parent).firstOrNull { it.path.trimEnd('/') == path.trimEnd('/') } ?: error("远程文件不存在，请刷新目录")
    }

    internal suspend fun open(documentId: String): File = withCachedFile(documentId) { it }

    internal suspend fun openDescriptor(documentId: String): android.os.ParcelFileDescriptor = withCachedFile(documentId) {
        android.os.ParcelFileDescriptor.open(it, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private suspend fun <T> withCachedFile(documentId: String, consume: (File) -> T): T = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            val (id, path) = decodeId(documentId)
            // 先确认连接仍存在，再允许读取缓存。
            val connection = account(id)
            val entry = try { entry(documentId) } catch (failure: java.io.IOException) {
                val cached = directory.listFiles()?.filter { it.isFile && it.name.startsWith(cachePrefix(id, path)) && !it.name.endsWith(".part") && it.length() > 0 }
                    ?.maxByOrNull { it.lastModified() }
                if (cached != null) { cached.setLastModified(System.currentTimeMillis()); return@withLock consume(cached) }
                throw failure
            }
            require(!entry.directory) { "目录不能作为文件读取" }
            val target = cacheFile(id, entry)
            if (target.isFile && target.length() > 0 && (entry.size < 0 || target.length() == entry.size)) {
                target.setLastModified(System.currentTimeMillis()); return@withLock consume(target)
            }
            val temp = File(directory, "${target.name}.part")
            try {
                client.download(connection, entry, temp)
                currentCoroutineContext().ensureActive()
                check(!target.exists() || target.delete()) { "损坏缓存无法替换" }
                check(temp.renameTo(target)) { "缓存写入失败，请检查可用空间" }
                prune(target)
                consume(target)
            } finally { temp.delete() }
        }
    }

    suspend fun cached(id: String, entry: DavEntry): Boolean = withContext(Dispatchers.IO) { cacheFile(id, entry).isFile }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) { cacheMutex.withLock {
        directory.listFiles()?.sumOf { file -> val size = file.length(); if (file.delete()) size else 0L } ?: 0L
    } }

    private fun prune(keep: File) {
        val files = directory.listFiles()?.filter { it.isFile && it != keep }.orEmpty().sortedBy { it.lastModified() }
        var total = keep.length() + files.sumOf { it.length() }
        files.forEach { if (it.name.endsWith(".part") || total > CACHE_BYTES) { val size = it.length(); if (it.delete()) total -= size } }
    }

    private fun cacheFile(id: String, entry: DavEntry): File {
        val revision = "${entry.path}:${entry.revision}:${entry.size}:${entry.modifiedAt}"
        val digest = MessageDigest.getInstance("SHA-256").digest(revision.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(directory, cachePrefix(id, entry.path) + digest)
    }

    private fun cachePrefix(id: String, path: String): String = "$id-" + MessageDigest.getInstance("SHA-256")
        .digest(path.toByteArray()).joinToString("") { "%02x".format(it) } + "-"

    private fun account(id: String): DavAccount = accounts.read().firstOrNull { it.id == id } ?: error("WebDAV 连接已移除，请重新添加")
    private fun DavAccount.summary() = DavConnection(id, name, address, username, allowHttp)

    companion object {
        private const val CACHE_BYTES = 768L * 1024 * 1024
        const val AUTHORITY = "com.asterreader.app.webdav.documents"
        @Volatile private var instance: WebDavRepository? = null
        fun get(context: Context): WebDavRepository = instance ?: synchronized(this) {
            instance ?: WebDavRepository(context.applicationContext).also { instance = it }
        }
        fun isRemote(uri: Uri) = uri.scheme == "content" && uri.authority == AUTHORITY
        fun documentId(id: String, path: String): String {
            require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) { "连接 ID 无效" }
            validatePath(path)
            return "$id:${Base64.getUrlEncoder().withoutPadding().encodeToString(path.toByteArray())}"
        }
        fun decodeId(documentId: String): Pair<String, String> {
            require(documentId.length <= 16384) { "路径过长" }
            val id = documentId.substringBefore(':')
            require(runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)) { "连接 ID 无效" }
            val path = try { String(Base64.getUrlDecoder().decode(documentId.substringAfter(':')), Charsets.UTF_8) } catch (_: IllegalArgumentException) { error("路径编码无效") }
            validatePath(path)
            return id to path
        }
        private fun validatePath(path: String) {
            require(path.length <= 8192 && !path.startsWith('/') && !path.contains('?') && !path.contains('#')) { "路径无效" }
            path.split('/').forEach { segment ->
                val decoded = try { java.net.URI("/$segment").path.removePrefix("/") } catch (_: java.net.URISyntaxException) { error("路径编码无效") }
                require(decoded != "." && decoded != ".." && !decoded.contains('/') && !decoded.contains('\\') && !decoded.contains('\u0000')) { "路径不能越过授权目录" }
            }
        }
        fun treeUri(id: String, path: String = ""): Uri = DocumentsContract.buildTreeDocumentUri(AUTHORITY, documentId(id, path))
        fun documentUri(id: String, path: String): Uri = DocumentsContract.buildDocumentUri(AUTHORITY, documentId(id, path))
    }
}
