package com.example

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.UpdateAsset
import com.example.data.remote.UpdateDownloadRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDownloadManager
import org.robolectric.shadows.ShadowSigningInfo
import org.robolectric.util.ReflectionHelpers
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
open class UpdateDownloadRepositoryTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val manager = app.getSystemService(DownloadManager::class.java)
    private val asset = UpdateAsset("v2.0.0", "app-universal.apk",
        "https://github.com/lllll081926i/AsterReader/releases/download/v2.0.0/app-universal.apk", 4)

    @Test fun restoresProgressWithoutEnqueuingDuplicateTask() = runBlocking {
        val repo = UpdateDownloadRepository(app)
        repo.start(asset)
        val request = request()
        request.status = DownloadManager.STATUS_RUNNING
        request.totalSize = 4
        request.bytesSoFar = 2
        val restored = UpdateDownloadRepository(app)
        assertEquals(0.5f, restored.refresh().progress)
        assertTrue(restored.start(asset).downloading)
        assertEquals(1, shadowOf(manager).requestCount)
        assertEquals(asset.url, request.uri.toString())
        assertTrue(request.destination.path!!.replace('\\', '/').contains("/Download/updates/"))
    }

    @Test fun failedTaskCanRetryAndCancelClearsPersistentState() = runBlocking {
        val repo = UpdateDownloadRepository(app)
        repo.start(asset)
        request().status = DownloadManager.STATUS_FAILED
        assertFalse(repo.refresh().downloading)
        assertTrue(repo.refresh().message!!.contains("失败"))
        repo.start(asset)
        assertEquals(1, shadowOf(manager).requestCount)
        repo.cancel()
        assertNull(UpdateDownloadRepository(app).refresh().asset)
        assertEquals(0, shadowOf(manager).requestCount)
    }

    @Test fun missingSystemTaskCanBeRetried() = runBlocking {
        val repo = UpdateDownloadRepository(app)
        repo.start(asset)
        manager.remove(record().getLong("id"))
        assertTrue(repo.refresh().message!!.contains("失效"))
        assertTrue(repo.start(asset).downloading)
    }

    @Test fun completedUntrustedOrTruncatedApkNeverEnablesInstallation() = runBlocking {
        val repo = UpdateDownloadRepository(app)
        repo.start(asset)
        val file = File(request().destination.path!!)
        file.parentFile!!.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        request().status = DownloadManager.STATUS_SUCCESSFUL
        for ((pkg, code, signer) in listOf(Triple("other.app", 99L, "trusted"),
            Triple(app.packageName, 99L, "other"), Triple(app.packageName, 1L, "trusted"))) {
            installMetadata(file, pkg, code, signer)
            assertFalse(UpdateDownloadRepository(app).refresh().ready)
        }
        installMetadata(file, app.packageName, 99L, "trusted")
        file.writeBytes(byteArrayOf(1))
        assertFalse(repo.refresh().ready)
    }

    @Test fun matchingNewerApkRequestsPermissionThenUsesContentUri() = runBlocking {
        val repo = UpdateDownloadRepository(app)
        repo.start(asset)
        val file = File(request().destination.path!!)
        file.parentFile!!.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        installMetadata(file, app.packageName, 99, "trusted")
        request().status = DownloadManager.STATUS_SUCCESSFUL
        assertTrue(repo.refresh().ready)
        shadowOf(app.packageManager).setCanRequestPackageInstalls(false)
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, repo.installationIntent().action)
        shadowOf(app.packageManager).setCanRequestPackageInstalls(true)
        val intent = repo.installationIntent()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("content", intent.data?.scheme)
        assertEquals("downloads", intent.data?.authority)
        assertEquals("application/vnd.android.package-archive", intent.type)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        shadowOf(app.packageManager).getInternalMutablePackageInfo(app.packageName).setLongVersionCode(99)
        assertNull(UpdateDownloadRepository(app).refresh().asset)
        assertEquals(0, shadowOf(manager).requestCount)
    }

    private fun record() = org.json.JSONObject(app.getSharedPreferences("app_update_download", Context.MODE_PRIVATE)
        .getString("record", null)!!)

    private fun request(): ShadowDownloadManager.ShadowRequest =
        Shadow.extract(shadowOf(manager).getRequest(record().getLong("id")))

    private fun installMetadata(file: File, name: String, code: Long, signer: String) {
        fun signing(value: String): SigningInfo = ReflectionHelpers.callConstructor(SigningInfo::class.java).also {
            Shadow.extract<ShadowSigningInfo>(it).setSignatures(arrayOf(Signature(value.toByteArray())))
        }
        shadowOf(app.packageManager).getInternalMutablePackageInfo(app.packageName).apply {
            setLongVersionCode(4)
            signingInfo = signing("trusted")
        }
        shadowOf(app.packageManager).setPackageArchiveInfo(file.absolutePath, PackageInfo().apply {
            packageName = name
            setLongVersionCode(code)
            signingInfo = signing(signer)
        })
    }
}

@Config(sdk = [31])
class UpdateDownloadApi31RepositoryTest : UpdateDownloadRepositoryTest()
