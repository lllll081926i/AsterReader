package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateCheckState(val loading: Boolean = false, val message: String? = null,
    val openReleases: Boolean = false)

/** 仅在用户点击时匿名查询正式 Release，不携带云端账号或仓库凭据。 */
class GitHubUpdateRepository(private val clientFactory: () -> OkHttpClient = {
        OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS).build()
    }) {
    private val client by lazy(clientFactory)

    suspend fun check(currentVersion: String): UpdateCheckState = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("https://api.github.com/repos/lllll081926i/AsterReader/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2026-03-10").build()
        val call = client.newCall(request)
        coroutineScope {
            val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try { call.execute().use { response ->
                when (response.code) {
                    404 -> UpdateCheckState(message = "GitHub 上还没有发布版本", openReleases = true)
                    403, 429 -> UpdateCheckState(message = "GitHub 暂时限制访问，请稍后重试", openReleases = true)
                    200 -> {
                        val body = requireNotNull(response.body)
                        val source = body.source()
                        require(!source.request(MAX_RESPONSE_BYTES + 1L))
                        parseRelease(source.readUtf8(), currentVersion)
                    }
                    else -> UpdateCheckState(message = "检查失败，请稍后重试", openReleases = true)
                }
            } } finally { cancellation.cancel() }
        }
    }

    companion object {
        const val RELEASES_URL = "https://github.com/lllll081926i/AsterReader/releases"
        private const val MAX_RESPONSE_BYTES = 256 * 1024

        internal fun parseRelease(json: String, currentVersion: String): UpdateCheckState {
            val release = JSONObject(json)
            require(!release.optBoolean("draft") && !release.optBoolean("prerelease"))
            val tag = release.getString("tag_name")
            require(tag.isNotBlank() && tag.length <= 80)
            val latest = versionParts(tag)
            val current = versionParts(currentVersion)
            if (latest == null || current == null) return UpdateCheckState(message = "最新发布：$tag，请核对版本", openReleases = true)
            val comparison = latest.zip(current).firstOrNull { it.first != it.second }
            return if (comparison != null && comparison.first > comparison.second) {
                UpdateCheckState(message = "发现新版本 $tag", openReleases = true)
            } else UpdateCheckState(message = "已是最新版本")
        }

        private fun versionParts(value: String): List<Long>? {
            val match = Regex("^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(value) ?: return null
            return match.groupValues.drop(1).map { it.toLongOrNull() ?: return null }
        }
    }
}
