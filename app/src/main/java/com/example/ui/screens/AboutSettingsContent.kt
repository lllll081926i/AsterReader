package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterreader.core.designsystem.AsterLayout
import com.example.BuildConfig
import com.example.R
import com.example.data.local.OpenSourceNotice
import com.example.ui.AsterViewModel
import com.example.ui.components.*

@Composable
fun AboutSettingsContent(model: AsterViewModel, onOpenLicenses: () -> Unit) {
    val update by model.updateCheck.collectAsStateWithLifecycle()
    val download by model.updateDownload.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    AsterSettingsGroup {
        Row(Modifier.fillMaxWidth().padding(AsterLayout.PagePadding),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            Image(painterResource(R.drawable.aster_logo), null, Modifier.size(AsterLayout.TouchTarget))
            Column(Modifier.weight(1f)) {
                Text("Aster Reader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    AsterSettingsGroup {
        AsterSettingsRow(if (update.loading) "正在检查…" else "检查更新",
            leadingIcon = Icons.Outlined.SystemUpdate, trailingText = "GitHub", onClick = model::checkForUpdate)
        if (update.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth().padding(horizontal = AsterLayout.PagePadding),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
            UpdateDownloadStatus(download)
            UpdateDownloadAction(download, update.asset != null,
                model::downloadUpdate, model::cancelUpdateDownload, model::installUpdate)
        }
        update.message?.let { message ->
            Column(Modifier.fillMaxWidth().padding(horizontal = AsterLayout.PagePadding)
                .padding(bottom = AsterLayout.ControlSpacing)) {
                Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (update.openReleases) TextButton(onClick = {
                    try { uriHandler.openUri(com.example.data.remote.GitHubUpdateRepository.RELEASES_URL) }
                    catch (_: Exception) { model.showSnackbar("无法打开 GitHub") }
                }) { Text("前往 GitHub") }
            }
        }
        AsterGroupDivider()
        AsterSettingsRow("致谢", leadingIcon = Icons.Outlined.FavoriteBorder, onClick = onOpenLicenses)
    }
}

@Composable
fun OpenSourceSettingsContent(model: AsterViewModel, selectedId: String? = null,
    onOpen: (OpenSourceNotice) -> Unit = {}) {
    val state by model.openSource.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { model.loadOpenSourceNotices() }
    when {
        state.loading || (state.entries.isEmpty() && state.error == null) -> LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error != null -> {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = model::loadOpenSourceNotices) { Text("重试") }
        }
        selectedId != null -> {
            val entry = state.entries.find { it.id == selectedId }
            if (entry == null) Text("许可不存在", color = MaterialTheme.colorScheme.onSurface) else NoticeContent(entry, model)
        }
        state.entries.isNotEmpty() -> AsterSettingsGroup {
            state.entries.forEachIndexed { index, entry ->
                if (index > 0) AsterGroupDivider()
                Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpen(entry) }
                    .heightIn(min = AsterLayout.SettingsRowHeight)
                    .padding(horizontal = AsterLayout.PagePadding, vertical = AsterLayout.ControlSpacing),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(entry.license, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(AsterLayout.HeaderIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun NoticeContent(entry: OpenSourceNotice, model: AsterViewModel) {
    val uriHandler = LocalUriHandler.current
    AsterSettingsGroup {
        AboutValue("组件", entry.name)
        AsterGroupDivider()
        AboutValue("版本", entry.version)
        AsterGroupDivider()
        AboutValue("许可", entry.license)
        if (entry.url.startsWith("https://")) {
            AsterGroupDivider()
            AsterSettingsRow("许可来源", leadingIcon = Icons.Outlined.OpenInNew, onClick = {
                try { uriHandler.openUri(entry.url) } catch (_: Exception) { model.showSnackbar("无法打开链接") }
            })
        }
    }
    if (entry.notice.isNotBlank()) SelectionContainer {
        Text(entry.notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AboutValue(label: String, value: String) {
        val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
        if (largeFont) Column(Modifier.fillMaxWidth().heightIn(min = AsterLayout.SettingsRowHeight)
            .padding(AsterLayout.PagePadding), verticalArrangement = Arrangement.spacedBy(AsterLayout.ControlSpacing)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Row(Modifier.fillMaxWidth().heightIn(min = AsterLayout.SettingsRowHeight)
            .padding(horizontal = AsterLayout.PagePadding, vertical = AsterLayout.ControlSpacing),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(value, Modifier.weight(3f), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
}
