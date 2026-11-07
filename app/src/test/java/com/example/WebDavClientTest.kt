package com.example

import com.example.data.remote.DavAccount
import com.example.data.remote.DavEntry
import com.example.data.remote.WebDavClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class WebDavClientTest {
    private lateinit var server: HttpServer
    private lateinit var account: DavAccount
    private val client = WebDavClient()
    private var status = 207
    private var payload = ""
    private val requests = AtomicInteger()
    private var authorization: String? = null
    private var method: String? = null
    private var depth: String? = null
    private var ifMatch: String? = null
    private var slow = false

    @Before fun setup() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.incrementAndGet()
            authorization = exchange.requestHeaders.getFirst("Authorization")
            method = exchange.requestMethod
            depth = exchange.requestHeaders.getFirst("Depth")
            ifMatch = exchange.requestHeaders.getFirst("If-Match")
            if (slow) Thread.sleep(1500)
            val bytes = payload.toByteArray()
            if (status in 300..399) exchange.responseHeaders.add("Location", "/dav/redirected")
            try { exchange.sendResponseHeaders(status, bytes.size.toLong()); exchange.responseBody.use { it.write(bytes) } }
            catch (_: java.io.IOException) { exchange.close() }
        }
        server.start()
        account = DavAccount(UUID.randomUUID().toString(), "测试", "http://127.0.0.1:${server.address.port}/dav/", "reader", "test-only", true)
    }
    @After fun close() { server.stop(0) }

    private fun response(href: String, properties: String = "<d:getcontentlength>3</d:getcontentlength>", status: Int = 200) =
        "<d:response><d:href>$href</d:href><d:propstat><d:prop>$properties</d:prop><d:status>HTTP/1.1 $status OK</d:status></d:propstat></d:response>"
    private fun listing(vararg responses: String) = "<?xml version=\"1.0\"?><d:multistatus xmlns:d=\"DAV:\">${responses.joinToString("")}</d:multistatus>"

    @Test fun propfindUsesDepthAndHandlesRootChineseNamesAndCollections() = runBlocking {
        payload = listing(response("/dav/", "<d:resourcetype><d:collection/></d:resourcetype>"),
            response("/dav/%E6%BC%AB%E7%94%BB.cbz", "<d:getcontentlength>123</d:getcontentlength><d:getetag>&quot;v1&quot;</d:getetag>"),
            response("folder/", "<d:resourcetype><d:collection/></d:resourcetype>"), response("denied.cbz", status = 403))
        val entries = client.list(account, "")
        assertEquals("PROPFIND", method); assertEquals("1", depth)
        assertEquals(Credentials.basic("reader", "test-only", Charsets.UTF_8), authorization)
        assertEquals(2, entries.size); assertTrue(entries[0].directory)
        assertEquals("漫画.cbz", entries[1].name); assertEquals("\"v1\"", entries[1].etag)
    }

    @Test fun rejectsCrossOriginAndEscapedPaths() = runBlocking {
        for (href in listOf("https://other.invalid/dav/book.cbz", "/outside/book.cbz", "/dav/%2Fsecret.cbz", "/dav/folder/deep.cbz", "/dav/../secret.cbz")) {
            payload = listing(response(href))
            assertTrue("必须拒绝越界 href", runCatching { client.list(account, "") }.isFailure)
        }
    }

    @Test fun rejectsEntityExpansionAndNonDavXmlAndOversizedFields() = runBlocking {
        for (xml in listOf("<!DOCTYPE d:multistatus [<!ENTITY x SYSTEM 'file:///private'>]>" + listing(response("&x;")).removePrefix("<?xml version=\"1.0\"?>"),
            "<html>login</html>", listing(response("/dav/book.cbz", "<d:displayname>${"x".repeat(17000)}</d:displayname>")),
            listing(response("/dav/book.cbz", "<d:prop>${"<d:a>".repeat(40)}${"</d:a>".repeat(40)}</d:prop>")))) {
            payload = xml
            assertTrue(runCatching { client.list(account, "") }.isFailure)
        }
    }

    @Test fun rejectsOverBudgetDirectory() = runBlocking {
        payload = listing(*(0..WebDavClient.MAX_ENTRIES + 1).map { response("/dav/$it.cbz") }.toTypedArray())
        assertTrue(runCatching { client.list(account, "") }.isFailure)
    }

    @Test fun reportsAuthenticationAndRedirectWithoutFollowing() = runBlocking {
        payload = "denied"
        for (code in listOf(401, 403, 302, 404, 429, 500)) {
            status = code
            val before = requests.get()
            assertTrue(runCatching { client.list(account, "") }.isFailure)
            assertEquals(before + 1, requests.get())
        }
    }

    @Test fun downloadsUsingRevisionAndRejectsLengthMismatch() = runBlocking {
        status = 200; payload = "abc"
        val file = File.createTempFile("dav-test-", ".tmp")
        try {
            client.download(account, DavEntry("book.cbz", "book.cbz", false, 3, etag = "\"v1\""), file)
            assertEquals("GET", method); assertEquals("\"v1\"", ifMatch); assertEquals("abc", file.readText())
            assertTrue(runCatching { client.download(account, DavEntry("book.cbz", "book.cbz", false, 4), file) }.isFailure)
            assertTrue(runCatching { client.download(account, DavEntry("book.cbz", "book.cbz", false, WebDavClient.MAX_FILE_BYTES + 1), file) }.isFailure)
        } finally { file.delete() }
    }

    @Test fun cancellationStopsPendingRequest() = runBlocking {
        slow = true; payload = listing()
        val task = async { client.list(account, "") }
        delay(100); kotlinx.coroutines.withTimeout(1000) { task.cancelAndJoin() }; assertTrue(task.isCancelled)
    }

    @Test fun requiresExplicitHttpAndRejectsEmbeddedCredentials() {
        assertTrue(runCatching { WebDavClient.root("http://example.org/dav", false) }.isFailure)
        assertTrue(runCatching { WebDavClient.root("https://user:pass@example.org/dav", false) }.isFailure)
        assertTrue(runCatching { WebDavClient.root("https://example.org/dav?token=secret", false) }.isFailure)
        assertEquals("/dav/", WebDavClient.root("https://example.org/dav", false).encodedPath)
    }
}
