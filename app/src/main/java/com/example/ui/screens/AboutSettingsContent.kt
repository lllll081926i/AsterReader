package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
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
fun AboutSettingsContent(onOpenLicenses: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = AsterLayout.ControlSpacing),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
        Box(Modifier.size(AsterLayout.TouchTarget)) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
        }
        Column {
            Text("Aster Reader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text("本地漫画阅读器", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    AsterSettingsGroup("应用") {
        AboutValue("版本", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        AsterGroupDivider()
        AboutValue("构建", if (BuildConfig.DEBUG) "调试版" else "正式版")
        AsterGroupDivider()
        AboutValue("系统", "Android 12 及以上")
    }
    AsterSettingsGroup("格式") {
        AboutValue("漫画", "ZIP/CBZ · RAR/CBR\n7Z/CB7 · TAR/CBT")
        AsterGroupDivider()
        AboutValue("文档", "PDF · EPUB")
        AsterGroupDivider()
        AboutValue("图片", "单图 · 图片文件夹")
    }
    AsterSettingsGroup("隐私") {
        AboutValue("文件", "本地保存，保留原文件")
        AsterGroupDivider()
        AboutValue("隐藏", "应用内访问锁，不加密原文件")
        AsterGroupDivider()
        AboutValue("加密", "保险库保存密文，外部原文件保留")
        AsterGroupDivider()
        AboutValue("备份", "仅公开清单，不含漫画与隐藏作品")
    }
    AsterSettingsGroup("致谢") {
        AsterSettingsRow("开源许可", leadingIcon = Icons.Outlined.Code, onClick = onOpenLicenses)
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
