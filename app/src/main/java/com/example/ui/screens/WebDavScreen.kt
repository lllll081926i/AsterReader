package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asterreader.core.designsystem.AsterLayout
import com.asterreader.core.designsystem.AsterChoice
import com.asterreader.core.designsystem.AsterSettingsChoice
import com.example.data.remote.WebDavService
import com.example.data.remote.DavConnection
import com.example.ui.WebDavViewModel
import com.example.ui.components.AsterSettingsGroup
import com.example.ui.components.AsterSettingsRow
import com.example.ui.components.AsterSubScreenHeader
import com.example.ui.components.AsterSwitchRow

@Composable
fun WebDavScreen(onBack: () -> Unit, onOpenBook: (String) -> Unit = {}, viewModel: WebDavViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) { onDispose { viewModel.cancel() } }
    val back = { if (!viewModel.back()) onBack() }
    BackHandler(onBack = back)
    var removing by remember { mutableStateOf<DavConnection?>(null) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().testTag("webdav_screen")) {
        AsterSubScreenHeader(if (state.editing) if (state.editConnection == null) "添加连接" else "编辑连接" else state.connection?.name ?: "WebDAV", back)
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.busy || state.error != null || state.message != null) Row(
            Modifier.widthIn(max = AsterLayout.SettingsContentMaxWidth).fillMaxWidth().align(Alignment.CenterHorizontally)
                .padding(horizontal = AsterLayout.PagePadding), verticalAlignment = Alignment.CenterVertically) {
            Text(state.error ?: if (state.busy) state.progress ?: "正在读取" else state.message.orEmpty(), Modifier.weight(1f),
                color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall)
            if (state.busy) IconButton(onClick = viewModel::cancel) { Icon(Icons.Outlined.Close, "取消读取") }
            else if (state.error != null && !state.editing) IconButton(onClick = viewModel::retry) { Icon(Icons.Outlined.Refresh, "重试") }
        }
        LazyColumn(Modifier.weight(1f).widthIn(max = AsterLayout.SettingsContentMaxWidth).fillMaxWidth().align(Alignment.CenterHorizontally)
            .navigationBarsPadding().imePadding(), contentPadding = PaddingValues(horizontal = AsterLayout.PagePadding, vertical = AsterLayout.BottomSpacing),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            if (state.editing) item(key = "connection-editor") {
                key(state.editConnection?.id) {
                    WebDavConnectionEditor(state.editConnection, state.busy, viewModel)
                }
            } else if (state.connection == null) {
                item(key = "add") { AsterSettingsGroup { AsterSettingsRow("添加连接", leadingIcon = Icons.Outlined.Add, onClick = { viewModel.edit() }) } }
                if (state.connections.isEmpty() && !state.busy) item(key = "empty") { Text("暂无连接", style = MaterialTheme.typography.bodyMedium) }
                items(state.connections, key = { "connection:${it.id}" }) { connection ->
                    AsterSettingsGroup {
                        AsterSettingsRow(connection.name, leadingIcon = Icons.Outlined.Cloud, onClick = { viewModel.browse(connection) })
                        WebDavActions {
                            OutlinedButton(onClick = { viewModel.edit(connection) }, enabled = !state.busy, modifier = it) { Icon(Icons.Outlined.Edit, null); Text("编辑") }
                            OutlinedButton(onClick = { removing = connection }, enabled = !state.busy, modifier = it) { Icon(Icons.Outlined.LinkOff, null); Text("移除") }
                        }
                    }
                }
            } else {
                item(key = "path") {
                    Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
                        Text(if (state.path.isBlank()) "根目录" else android.net.Uri.decode(state.path.trimEnd('/')), style = MaterialTheme.typography.bodySmall)
                        WebDavActions {
                            OutlinedButton(onClick = viewModel::retry, enabled = !state.busy, modifier = it) { Icon(Icons.Outlined.Refresh, null); Text("刷新") }
                            Button(onClick = viewModel::scan, enabled = !state.busy, modifier = it) { Icon(Icons.Outlined.FolderOpen, null); Text("扫描此目录") }
                        }
                    }
                }
                if (state.entries.isEmpty() && !state.busy && state.error == null) item(key = "empty-folder") { Text("目录为空", style = MaterialTheme.typography.bodyMedium) }
                items(state.entries, key = { "entry:${it.path}" }) { entry ->
                    AsterSettingsGroup {
                        AsterSettingsRow(entry.name, leadingIcon = if (entry.directory) Icons.Outlined.Folder else Icons.Outlined.Description,
                            trailingText = if (entry.directory) "打开" else if (entry.path in state.cachedPaths) "已缓存" else "读取",
                            onClick = { if (!state.busy) { if (entry.directory) viewModel.browse(state.connection!!, entry.path) else viewModel.importFile(entry, onOpenBook) } })
                    }
                }
            }
        }
    }
    removing?.let { connection -> AlertDialog(onDismissRequest = { removing = null }, title = { Text("移除连接") },
        text = { Text("移除连接和下载缓存。云端文件与书库阅读记录保留。") },
        confirmButton = { TextButton(onClick = { removing = null; viewModel.remove(connection) }) { Icon(Icons.Outlined.LinkOff, null); Text("移除") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Icon(Icons.Outlined.Close, null); Text("取消") } }) }
}

@Composable
private fun WebDavActions(content: @Composable (Modifier) -> Unit) {
    if (LocalDensity.current.fontScale >= 1.5f) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) { content(Modifier.fillMaxWidth()) }
    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) { content(Modifier.weight(1f)) }
}

@Composable
private fun WebDavConnectionEditor(connection: DavConnection?, busy: Boolean, viewModel: WebDavViewModel) {
    var name by remember { mutableStateOf(connection?.name.orEmpty()) }
    var address by remember { mutableStateOf(connection?.address ?: "https://") }
    var username by remember { mutableStateOf(connection?.username.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var keepPassword by remember { mutableStateOf(connection != null) }
    var showPassword by remember { mutableStateOf(false) }
    var service by remember { mutableStateOf(WebDavService.fromAddress(connection?.address.orEmpty())) }
    var choosingService by remember { mutableStateOf(false) }
    var http by remember { mutableStateOf(connection?.allowHttp ?: false) }
    Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
        AsterSettingsGroup {
            AsterSettingsRow("服务商", leadingIcon = Icons.Outlined.Cloud, trailingText = service.title,
                onClick = { if (!busy) choosingService = !choosingService })
            if (choosingService) AsterSettingsChoice(
                options = WebDavService.entries.map { AsterChoice(it.title, Icons.Outlined.Cloud) },
                selectedIndex = service.ordinal, onSelect = { index ->
                    if (!busy) {
                        val selected = WebDavService.entries[index]
                        if (selected != service) {
                            name = selected.title
                            address = selected.initialAddress
                            username = ""
                            password = ""
                            keepPassword = false
                            showPassword = false
                            http = false
                            service = selected
                        }
                        choosingService = false
                    }
                })
        }
        Text(service.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, label = { Text("名称") })
        OutlinedTextField(address, { address = it.take(4096); if (address != connection?.address) keepPassword = false }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), label = { Text("WebDAV 地址") })
        OutlinedTextField(username, { username = it.take(512) }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true, label = { Text(if (service == WebDavService.NUTSTORE) "邮箱" else "用户名") })
        if (connection != null) AsterSwitchRow("保留密码", checked = keepPassword, onCheckedChange = { if (!busy) keepPassword = it })
        if (!keepPassword) OutlinedTextField(password, { password = it.take(4096) }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
            label = { Text(if (service == WebDavService.NUTSTORE || service == WebDavService.NEXTCLOUD) "应用密码" else "密码") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (showPassword) "隐藏密码" else "显示密码") } })
        AsterSwitchRow("允许 HTTP", checked = http, onCheckedChange = { if (!busy) http = it })
        if (http) Text("HTTP 会明文传输账号和漫画。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        WebDavActions {
            OutlinedButton(onClick = { viewModel.save(name, address, username, password.toCharArray(), http, false, keepPassword) },
                enabled = !busy && name.isNotBlank() && address.isNotBlank(), modifier = it) { Icon(Icons.Outlined.Wifi, null); Text("测试") }
            Button(onClick = { viewModel.save(name, address, username, password.toCharArray(), http, true, keepPassword); password = ""; showPassword = false },
                enabled = !busy && name.isNotBlank() && address.isNotBlank(), modifier = it) { Icon(Icons.Outlined.Save, null); Text("保存") }
        }
    }
}
