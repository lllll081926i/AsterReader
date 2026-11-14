package com.example.data.remote

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class UpdateDownloadState(
    val asset: UpdateAsset? = null,
    val downloading: Boolean = false,
    val progress: Float? = null,
    val ready: Boolean = false,
    val message: String? = null
)

/** 系统负责后台下载，应用只保存下载任务标识和安装包元数据。 */
class UpdateDownloadRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(DownloadManager::class.java)
    private var verifiedFile: Triple<String, Long, Long>? = null
    private var verifiedPackage = false

    suspend fun start(asset: UpdateAsset): UpdateDownloadState = withContext(Dispatchers.IO) {
        val existing = refresh()
        if (existing.downloading || existing.ready) return@withContext existing
        cancel()
        val filename = "updates/${UUID.randomUUID()}.apk"
        val request = DownloadManager.Request(Uri.parse(asset.url))
            .setTitle("Aster Reader ${asset.version}")
            .setDescription("正在下载安装包")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
        val id = manager.enqueue(request)
        val record = JSONObject().put("id", id).put("file", filename)
            .put("version", asset.version).put("name", asset.name).put("url", asset.url).put("size", asset.size)
        if (!preferences.edit().putString("record", record.toString()).commit()) {
            manager.remove(id)
            error("无法保存下载任务")
        }
        UpdateDownloadState(asset = asset, downloading = true, message = "正在下载")
    }

    suspend fun refresh(): UpdateDownloadState = withContext(Dispatchers.IO) {
        val record = record() ?: return@withContext UpdateDownloadState()
        val installedDownload = preferences.getLong("verified_version_code", 0)
        if (installedDownload > 0 && installedDownload <= installedVersionCode()) {
            cancel()
            return@withContext UpdateDownloadState()
        }
        val asset = asset(record)
        manager.query(DownloadManager.Query().setFilterById(record.getLong("id")))?.use { cursor ->
            if (!cursor.moveToFirst()) return@withContext UpdateDownloadState(asset = asset, message = "下载任务已失效，请重新下载")
            fun number(column: String) = cursor.getLong(cursor.getColumnIndexOrThrow(column))
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val file = downloadFile(record)
                    val stamp = Triple(file.absolutePath, file.length(), file.lastModified())
                    if (verifiedFile != stamp) {
                        verifiedPackage = validPackage(file, asset.size)
                        verifiedFile = stamp
                    }
                    if (!verifiedPackage) return@withContext UpdateDownloadState(asset = asset, message = "安装包校验失败，请重新下载")
                    UpdateDownloadState(asset = asset, ready = true, message = "下载完成，可以安装")
                }
                DownloadManager.STATUS_FAILED -> UpdateDownloadState(asset = asset, message = "下载失败，请重试")
                else -> {
                    val total = number(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val done = number(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    UpdateDownloadState(asset = asset, downloading = true,
                        progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null,
                        message = if (status == DownloadManager.STATUS_PAUSED)
                            "下载已暂停，等待网络恢复" else "正在下载")
                }
            }
        } ?: UpdateDownloadState(asset = asset, message = "无法读取下载任务，请重试")
    }

    suspend fun cancel() = withContext(Dispatchers.IO) {
        record()?.let { manager.remove(it.getLong("id")) }
        preferences.edit().remove("record").remove("verified_version_code").commit()
        verifiedFile = null
        verifiedPackage = false
        Unit
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    suspend fun installationIntent(): Intent = withContext(Dispatchers.IO) {
        check(refresh().ready)
        if (!canInstall()) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        } else {
            val uri = requireNotNull(manager.getUriForDownloadedFile(requireNotNull(record()).getLong("id")))
            require(uri.scheme == "content")
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun record(): JSONObject? = preferences.getString("record", null)?.let(::JSONObject)
    private fun asset(record: JSONObject) = UpdateAsset(record.getString("version"), record.getString("name"),
        record.getString("url"), record.getLong("size"))
    private fun downloadFile(record: JSONObject): File {
        val root = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
        val file = File(root, record.getString("file")).canonicalFile
        require(file.parentFile == File(root, "updates").canonicalFile)
        return file
    }

    @Suppress("DEPRECATION")
    private fun installedVersionCode() = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode

    @Suppress("DEPRECATION")
    private fun validPackage(file: File, expectedSize: Long): Boolean {
        if (!file.isFile || file.length() != expectedSize) return false
        val pm = context.packageManager
        val apk = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES) ?: return false
        val installed = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        if (apk.packageName != context.packageName || apk.longVersionCode <= installed.longVersionCode) return false
        val candidate = apk.signingInfo?.apkContentsSigners?.toSet().orEmpty()
        val trusted = installed.signingInfo?.apkContentsSigners?.toSet().orEmpty()
        val valid = candidate.isNotEmpty() && candidate == trusted
        if (valid) preferences.edit().putLong("verified_version_code", apk.longVersionCode).apply()
        return valid
    }
}
