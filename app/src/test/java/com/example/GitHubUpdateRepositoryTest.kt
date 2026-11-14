package com.example

import com.example.data.remote.GitHubUpdateRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GitHubUpdateRepositoryTest {
    @Test fun comparesNumericVersionInsteadOfTextOrPublicationDate() {
        assertTrue(release("v2.10.0").message!!.startsWith("发现新版本"))
        assertEquals("已是最新版本", release("v2.4.0").message)
        assertEquals("已是最新版本", release("v2.3.9").message)
    }

    @Test fun unknownVersionNeverClaimsUpToDate() {
        val state = release("release-2026-11")
        assertTrue(state.openReleases)
        assertTrue(state.message!!.contains("请核对版本"))
    }

    @Test fun excludesDraftAndPreviewReleases() {
        for (flag in listOf("draft", "prerelease")) {
            assertThrows(IllegalArgumentException::class.java) {
                GitHubUpdateRepository.parseRelease("""{"tag_name":"v9.0.0","$flag":true}""", "2.4.0")
            }
        }
    }

    @Test fun ignoresUntrustedReleaseUrl() {
        val state = GitHubUpdateRepository.parseRelease(
            """{"tag_name":"v3.0.0","html_url":"https://example.com/malware.apk"}""", "2.4.0")
        assertTrue(state.openReleases)
        assertEquals("https://github.com/lllll081926i/AsterReader/releases", GitHubUpdateRepository.RELEASES_URL)
    }

    @Test fun missingPrivateOrUnpublishedReleaseNeverMeansLatest() = runBlocking {
        val state = repository(404).check("2.4.0")
        assertTrue(state.message!!.contains("还没有发布版本"))
        assertTrue(state.openReleases)
    }

    @Test fun rateLimitCanBeRetriedOrOpenedInBrowser() = runBlocking {
        for (code in listOf(403, 429)) {
            val state = repository(code).check("2.4.0")
            assertTrue(state.message!!.contains("稍后重试"))
            assertTrue(state.openReleases)
        }
    }

    @Test fun rejectsOversizedResponse() = runBlocking {
        try {
            repository(200, "x".repeat(256 * 1024 + 1)).check("2.4.0")
            fail("应拒绝超大响应")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun choosesMatchingArchitectureBeforeUniversal() {
        assertEquals("AsterReader-3.0.0-arm64-v8a.apk", assets(listOf("arm64-v8a", "armeabi-v7a")).asset?.name)
        assertEquals("AsterReader-3.0.0-armeabi-v7a.apk", assets(listOf("armeabi-v7a")).asset?.name)
        assertEquals("AsterReader-3.0.0-universal.apk", assets(listOf("x86_64")).asset?.name)
    }

    @Test fun rejectsExternalAndOtherRepositoryDownloads() {
        for (url in listOf("http://github.com/lllll081926i/AsterReader/releases/download/v3.0.0/app-universal.apk",
            "https://evil.example/app-universal.apk",
            "https://github.com/other/repo/releases/download/v3.0.0/app-universal.apk",
            "https://github.com/lllll081926i/AsterReader/releases/download/v3.0.0/sub/app-universal.apk",
            "https://github.com/lllll081926i/AsterReader/releases/download/v2.0.0/app-universal.apk")) {
            val json = """{"tag_name":"v3.0.0","assets":[{"name":"app-universal.apk","size":123,"browser_download_url":"$url"}]}"""
            assertNull(GitHubUpdateRepository.parseRelease(json, "2.4.0").asset)
        }
    }

    @Test fun doesNotOfferScriptsWrongArchitectureOrEmptyFiles() {
        val json = """{"tag_name":"v3.0.0","assets":[
            {"name":"app-arm64-v8a.apk","size":123,"browser_download_url":"https://github.com/lllll081926i/AsterReader/releases/download/v3.0.0/app-arm64-v8a.apk"},
            {"name":"app-universal.apk","size":0,"browser_download_url":"https://github.com/lllll081926i/AsterReader/releases/download/v3.0.0/app-universal.apk"},
            {"name":"setup-universal.sh","size":123,"browser_download_url":"https://github.com/lllll081926i/AsterReader/releases/download/v3.0.0/setup-universal.sh"}]}"""
        assertNull(GitHubUpdateRepository.parseRelease(json, "2.4.0", listOf("armeabi-v7a")).asset)
    }

    @Test fun alreadyInstalledReleaseDoesNotOfferDownload() {
        assertNull(assets(listOf("arm64-v8a"), "3.0.0").asset)
    }

    private fun assets(abis: List<String>, current: String = "2.4.0"): com.example.data.remote.UpdateCheckState {
        val assets = listOf("universal", "armeabi-v7a", "arm64-v8a").joinToString(",") { abi ->
            val name = "AsterReader-3.0.0-$abi.apk"
            """{"name":"$name","size":123,"browser_download_url":"https://github.com/lllll081926i/AsterReader/releases/download/v3.0.0/$name"}"""
        }
        return GitHubUpdateRepository.parseRelease("""{"tag_name":"v3.0.0","assets":[$assets]}""", current, abis)
    }

    private fun repository(code: Int, body: String = "") = GitHubUpdateRepository {
        OkHttpClient.Builder().addInterceptor { chain ->
            assertNull(chain.request().header("Authorization"))
            assertEquals("api.github.com", chain.request().url.host)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("测试响应").body(body.toResponseBody()).build()
        }.build()
    }

    private fun release(tag: String) = GitHubUpdateRepository.parseRelease("""{"tag_name":"$tag"}""", "2.4.0")
}
