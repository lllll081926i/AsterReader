package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.asterreader.core.designsystem.AsterLayout
import com.example.ui.AsterViewModel
import com.example.data.remote.UpdateDownloadState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AppUpdatePrompt(model: AsterViewModel, enabled: Boolean) {
    val owner = LocalLifecycleOwner.current
    val visible by model.showUpdate.collectAsStateWithLifecycle()
    val update by model.updateCheck.collectAsStateWithLifecycle()
    val download by model.updateDownload.collectAsStateWithLifecycle()
    LaunchedEffect(owner, enabled, download.downloading) {
        if (enabled) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                model.refreshUpdateOnForeground()
                delay(if (model.updateDownload.value.downloading) 1_000 else 30_000)
            }
        }
    }
    if (enabled && visible) AlertDialog(
        onDismissRequest = model::dismissUpdate,
        title = { Text("Aster Reader ${download.asset?.version ?: update.asset?.version.orEmpty()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
                Text("新版本已发布，可直接下载安装。")
                UpdateDownloadStatus(download)
            }
        },
        confirmButton = { UpdateDownloadAction(download, update.asset != null,
            model::downloadUpdate, model::cancelUpdateDownload, model::installUpdate) },
        dismissButton = { TextButton(onClick = model::dismissUpdate) { Text("稍后") } }
    )
}

@Composable
fun UpdateDownloadStatus(state: UpdateDownloadState) {
    state.message?.let { message ->
        Text(if (state.downloading && state.progress != null) "$message ${(state.progress!! * 100).toInt()}%" else message,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    if (state.downloading) {
        val progress = state.progress
        if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        else LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
fun UpdateDownloadAction(state: UpdateDownloadState, available: Boolean,
    onDownload: () -> Unit, onCancel: () -> Unit, onInstall: () -> Unit) {
    when {
        state.downloading -> TextButton(onClick = onCancel) { Text("取消下载") }
        state.ready -> Button(onClick = onInstall) { Text("安装更新") }
        available || state.asset != null -> Button(onClick = onDownload) {
            Text(if (state.message != null) "重新下载" else "下载更新")
        }
    }
}
