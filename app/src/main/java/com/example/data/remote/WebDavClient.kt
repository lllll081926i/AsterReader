package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

internal data class DavAccount(val id: String, val name: String, val address: String, val username: String,
    val password: String, val allowHttp: Boolean = false)

data class DavEntry(val path: String, val name: String, val directory: Boolean, val size: Long = -1,
    val modifiedAt: Long = 0, val etag: String = "", val mime: String = "application/octet-stream", val revision: String = etag)

/** 只读协议边界：不接受跨源 href、根目录逃逸或自动重定向。 */
internal class WebDavClient(private val client: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(false).followSslRedirects(false).connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS).callTimeout(5, TimeUnit.MINUTES).build()) {

    companion object {
        const val MAX_FILE_BYTES = 512L * 1024 * 1024
        const val MAX_XML_BYTES = 4L * 1024 * 1024
        const val MAX_ENTRIES = 5000
        private const val DAV = "DAV:"

        fun root(address: String, allowHttp: Boolean): HttpUrl {
            val url = try { address.trim().toHttpUrl() } catch (_: IllegalArgumentException) { error("请输入有效的 WebDAV 地址") }
            require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "请分别填写地址和账号，不要在地址中嵌入口令或查询参数" }
            require(url.isHttps || allowHttp) { "请使用 HTTPS；HTTP 需要明确允许明文传输" }
            return if (url.encodedPath.endsWith('/')) url else url.newBuilder().addPathSegment("").build()
        }

        fun resolve(root: HttpUrl, path: String): HttpUrl {
            val resolved = root.resolve(path) ?: error("远程路径无效")
            require(resolved.scheme == root.scheme && resolved.host == root.host && resolved.port == root.port &&
                resolved.username.isEmpty() && resolved.password.isEmpty() && resolved.query == null && resolved.fragment == null &&
                resolved.encodedPath.startsWith(root.encodedPath)) { "服务器返回了越界路径" }
            require(resolved.pathSegments.none { it.contains('/') || it.contains('\\') || it.contains('\u0000') || it == "." || it == ".." }) { "远程路径无效" }
            return resolved
        }
    }

    suspend fun list(account: DavAccount, path: String): List<DavEntry> = withContext(Dispatchers.IO) {
        val base = root(account.address, account.allowHttp)
        val directory = resolve(base, path)
        val request = request(account, directory).header("Depth", "1").method("PROPFIND",
            """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/><d:displayname/><d:getcontentlength/><d:getlastmodified/><d:getetag/><d:getcontenttype/></d:prop></d:propfind>"""
                .toRequestBody("application/xml; charset=utf-8".toMediaType())).build()
        withResponse(request) { response ->
            checkStatus(response, expected = 207)
            val body = response.body ?: error("服务器返回空目录响应")
            require(body.contentLength() <= MAX_XML_BYTES) { "目录过大，请缩小连接根目录" }
            body.byteStream().use { parseListing(it, base, directory) }
        }
    }

    internal fun parseListing(input: InputStream, base: HttpUrl, directory: HttpUrl): List<DavEntry> {
        val parser = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(LimitedInput(input, MAX_XML_BYTES), null)
        var href = ""
        var properties = mutableMapOf<String, String>()
        val successful = mutableMapOf<String, String>()
        var collection = false
        var successfulCollection = false
        var status = ""
        var field: String? = null
        var text = StringBuilder()
        var responses = 0
        var multistatus = false
        var successfulResponse = false
        val entries = linkedMapOf<String, DavEntry>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            require(parser.depth <= 32) { "目录 XML 层级过深" }
            when (event) {
                XmlPullParser.DOCDECL -> error("目录响应不允许 XML 实体声明")
                XmlPullParser.START_TAG -> {
                    if (parser.depth == 1) {
                        require(parser.namespace == DAV && parser.name == "multistatus") { "服务器未返回 WebDAV 目录" }
                        multistatus = true
                    }
                    if (parser.namespace == DAV) when (parser.name) {
                        "response" -> { require(++responses <= MAX_ENTRIES + 1) { "目录条目过多，请缩小范围" }; href = ""; successful.clear(); successfulCollection = false; successfulResponse = false }
                        "propstat" -> { properties = mutableMapOf(); status = ""; collection = false }
                        "collection" -> collection = true
                        "href", "status", "displayname", "getcontentlength", "getlastmodified", "getetag", "getcontenttype" -> { field = parser.name; text = StringBuilder() }
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> if (field != null) {
                    text.append(parser.text.orEmpty()); require(text.length <= 16384) { "目录字段过长" }
                }
                XmlPullParser.END_TAG -> if (parser.namespace == DAV) {
                    if (parser.name == field) {
                        when (field) { "href" -> href = text.toString().trim(); "status" -> status = text.toString(); else -> properties[field!!] = text.toString() }
                        field = null
                    }
                    when (parser.name) {
                        "propstat" -> if (Regex("HTTP/\\S+\\s+2\\d\\d(?:\\s.*)?").matches(status.trim())) { successful.putAll(properties); successfulCollection = successfulCollection || collection; successfulResponse = true }
                        "response" -> {
                            if (href.isNotBlank() && successfulResponse) {
                                val url = resolve(base, directory.resolve(href)?.toString() ?: error("目录路径无效"))
                                if (url.encodedPath.trimEnd('/') != directory.encodedPath.trimEnd('/')) {
                                    val relative = url.encodedPath.removePrefix(base.encodedPath)
                                    val parent = relative.trimEnd('/').substringBeforeLast('/', "")
                                    require(parent == directory.encodedPath.removePrefix(base.encodedPath).trimEnd('/')) { "服务器没有返回当前目录的直接子项" }
                                    val name = url.pathSegments.lastOrNull(String::isNotEmpty).orEmpty()
                                    val modified = runCatching { java.time.ZonedDateTime.parse(successful["getlastmodified"], java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrDefault(0)
                                    val path = if (successfulCollection) relative.trimEnd('/') + "/" else relative
                                    entries[path] = DavEntry(path, name, successfulCollection, successful["getcontentlength"]?.toLongOrNull() ?: -1,
                                        modified, successful["getetag"].orEmpty(), successful["getcontenttype"]?.substringBefore(';') ?: "application/octet-stream")
                                    require(entries.size <= MAX_ENTRIES) { "目录条目过多，请缩小范围" }
                                }
                            }
                        }
                    }
                }
            }
            event = parser.nextToken()
        }
        require(multistatus && parser.name == null) { "目录响应不完整" }
        return entries.values.sortedWith(compareByDescending<DavEntry> { it.directory }.thenBy { it.name.lowercase(java.util.Locale.ROOT) })
    }

    suspend fun download(account: DavAccount, entry: DavEntry, target: File) = withContext(Dispatchers.IO) {
        require(!entry.directory && entry.size <= MAX_FILE_BYTES) { "单文件上限为 512 MB" }
        val url = resolve(root(account.address, account.allowHttp), entry.path)
        val builder = request(account, url)
        if (entry.etag.isNotBlank() && !entry.etag.startsWith("W/")) builder.header("If-Match", entry.etag)
        withResponse(builder.get().build()) { response ->
            checkStatus(response, 200)
            val body = response.body ?: error("服务器未返回文件")
            require(body.contentLength() <= MAX_FILE_BYTES) { "单文件上限为 512 MB" }
            var total = 0L
            body.byteStream().use { input -> target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_FILE_BYTES) { "单文件上限为 512 MB" }
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            } }
            require(total > 0 && (entry.size < 0 || total == entry.size) && (body.contentLength() < 0 || total == body.contentLength())) { "下载不完整或文件已变化，请刷新目录" }
        }
    }

    private fun request(account: DavAccount, url: HttpUrl) = Request.Builder().url(url).header("Accept-Encoding", "identity").apply {
        if (account.username.isNotEmpty() || account.password.isNotEmpty()) header("Authorization", Credentials.basic(account.username, account.password, Charsets.UTF_8))
    }

    private fun checkStatus(response: Response, expected: Int) {
        if (response.code == expected) return
        error(when (response.code) {
            401, 403 -> "账号无效或没有读取权限"
            404 -> "远程文件或目录不存在，请刷新"
            301, 302, 303, 307, 308 -> "服务器要求跳转，请填写最终 WebDAV 地址"
            412 -> "远程文件已变化，请刷新目录"
            429 -> "服务器请求过多，请稍后重试"
            else -> "服务器读取失败（HTTP ${response.code}）"
        })
    }

    private suspend fun <T> withResponse(request: Request, block: suspend (Response) -> T): T = coroutineScope {
        val call = client.newCall(request)
        val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
        }
        try { execute(call).use { block(it) } } finally { cancellation.cancel() }
    }

    private suspend fun execute(call: Call): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (!continuation.isCancelled) continuation.resumeWithException(IOException("网络连接失败，请检查网络、地址与证书")) }
            override fun onResponse(call: Call, response: Response) { continuation.resume(response) { _, value, _ -> value.close() } }
        })
    }

    private class LimitedInput(input: InputStream, private val limit: Long) : FilterInputStream(input) {
        private var count = 0L
        override fun read(): Int = super.read().also { if (it >= 0) checkLimit(1) }
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int = `in`.read(bytes, offset, length).also { if (it > 0) checkLimit(it) }
        private fun checkLimit(size: Int) { count += size; require(count <= limit) { "目录 XML 超过大小上限" } }
    }
}
