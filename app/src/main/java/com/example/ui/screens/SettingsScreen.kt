package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterreader.core.designsystem.AsterSettingsChoice
import com.asterreader.core.designsystem.AsterChoice
import com.asterreader.core.designsystem.AsterLayout
import com.example.data.local.RepositoryEntity
import com.example.data.local.ShelfEntity
import com.example.ui.*
import com.example.ui.components.*

private val sectionTitles = mapOf(
    "appearance" to "外观", "sources" to "来源",
    "shelves" to "书架", "tags" to "标签", "storage" to "存储",
    "backup" to "备份", "stats" to "阅读统计", "about" to "关于", "licenses" to "致谢",
    "encryptedExport" to "加密导出", "encryptedRestore" to "加密恢复"
)

@Composable
fun SettingsScreen(
    uiState: AsterUiState,
    repositories: List<RepositoryEntity>,
    booksCount: Int,
    viewModel: AsterViewModel,
    onTriggerImportFile: () -> Unit,
    onBack: (() -> Unit)? = null,
    section: String? = null
) {
    com.asterreader.core.designsystem.AsterSettingsScale {
        SettingsScreenBody(uiState, repositories, booksCount, viewModel, onTriggerImportFile, onBack, section)
    }
}

@Composable
private fun SettingsScreenBody(
    uiState: AsterUiState,
    repositories: List<RepositoryEntity>,
    booksCount: Int,
    viewModel: AsterViewModel,
    onTriggerImportFile: () -> Unit,
    onBack: (() -> Unit)?,
    section: String?
) {
    if (section == "webdav") {
        WebDavScreen(onBack = { onBack?.invoke() }, onOpenBook = viewModel::openBookReader)
        return
    }
    if (onBack != null) BackHandler(enabled = !com.example.ui.components.LocalRootHandlesBack.current, onBack = onBack)
    val shelves by viewModel.allShelves.collectAsStateWithLifecycle()
    val books by viewModel.allBooks.collectAsStateWithLifecycle()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        uri -> uri?.let(viewModel::exportLibrary)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        uri -> uri?.let(viewModel::importLibrary)
    }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var editShelf by remember { mutableStateOf<ShelfEntity?>(null) }
    var shelfName by remember { mutableStateOf("") }
    var shelfEditor by remember { mutableStateOf(false) }
    var editTag by remember { mutableStateOf<String?>(null) }
    var tagName by remember { mutableStateOf("") }
    val navigate: (String) -> Unit = { viewModel.pushSubScreen(SubScreen.SettingsSection(it)) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()
        .testTag(if (section == null) "settings_screen" else "settings_$section")) {
        AsterSubScreenHeader(if (section?.startsWith("fileExport:") == true) "加密导出" else if (section in setOf("fileVault", "fileEncrypt")) "文件保险库"
            else if (section?.startsWith("license:") == true) "许可" else sectionTitles[section] ?: "设置", onBack)
        Column(Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth()
            .align(androidx.compose.ui.Alignment.CenterHorizontally).verticalScroll(rememberScrollState())
            .navigationBarsPadding().imePadding().padding(horizontal = AsterLayout.PagePadding)
            .padding(top = 4.dp, bottom = AsterLayout.BottomSpacing),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            when (section) {
                "fileVault", "fileEncrypt" -> FileVaultContent(uiState, viewModel, section)
                "encryptedExport", "encryptedRestore" -> EncryptedBackupForm(section == "encryptedExport", uiState, viewModel, { onBack?.invoke() })
                null -> {
                    AsterSettingsGroup("常用") {
                        AsterSettingsRow("外观", leadingIcon = Icons.Outlined.Palette, onClick = { navigate("appearance") })
                        AsterGroupDivider()
                        AsterSettingsRow("阅读统计", leadingIcon = Icons.Outlined.Schedule, onClick = { navigate("stats") })
                    }
                    AsterSettingsGroup("内容") {
                        AsterSettingsRow("来源", leadingIcon = Icons.Outlined.Folder, onClick = { navigate("sources") })
                        AsterGroupDivider()
                        AsterSettingsRow("书架", leadingIcon = Icons.Outlined.CollectionsBookmark, onClick = { navigate("shelves") })
                        AsterGroupDivider()
                        AsterSettingsRow("标签", leadingIcon = Icons.Outlined.Tag, onClick = { navigate("tags") })
                        AsterGroupDivider()
                        AsterSettingsRow("存储", leadingIcon = Icons.Outlined.Storage, onClick = { navigate("storage") })
                    }
                    AsterSettingsGroup("隐私") {
                        AsterSettingsRow("安全", leadingIcon = Icons.Outlined.Lock,
                            onClick = { viewModel.pushSubScreen(SubScreen.PrivacySecuritySettings) })
                    }
                    AsterSettingsGroup("数据") {
                        AsterSettingsRow("备份", leadingIcon = Icons.Outlined.Backup, onClick = { navigate("backup") })
                        AsterGroupDivider()
                        AsterSettingsRow("关于", leadingIcon = Icons.Outlined.Apps, onClick = { navigate("about") })
                    }
                }
                "appearance" -> {
                    AsterSettingsGroup("主题") {
                        AsterSettingsChoice(ThemeMode.entries.map { AsterChoice(it.label, when (it) {
                            ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                        }) }, ThemeMode.entries.indexOf(uiState.themeMode), { viewModel.setThemeMode(ThemeMode.entries[it]) })
                    }
                    AsterSettingsGroup("界面") {
                        AsterSwitchRow("减少动效", checked = uiState.reduceMotion, onCheckedChange = viewModel::setReduceMotion)
                        AsterGroupDivider()
                        AsterSwitchRow("继续阅读", checked = uiState.showRecentReading, onCheckedChange = viewModel::setShowRecentReading)
                    }
                }
                "stats" -> AsterSettingsGroup {
                    AsterSwitchRow("统计页", checked = uiState.showStatsTab, onCheckedChange = viewModel::setShowStatsTab)
                    AsterGroupDivider()
                    AsterSwitchRow("记录时长", checked = uiState.showReadingStats, onCheckedChange = viewModel::setShowReadingStats)
                    if (uiState.showReadingStats) {
                        AsterGroupDivider()
                        AsterSettingsRow("查看数据", onClick = { viewModel.pushSubScreen(SubScreen.ReadingStats) })
                    }
                }
                "sources" -> {
                    AsterSettingsGroup {
                        AsterSettingsRow("导入文件", leadingIcon = Icons.Outlined.Add, onClick = onTriggerImportFile)
                        AsterGroupDivider()
                        AsterSettingsRow("添加目录", leadingIcon = Icons.Outlined.CreateNewFolder,
                            onClick = { viewModel.addRepositoryAndScan("本地书库", com.example.data.local.LibraryRepository.DEFAULT_SCAN_RULE) })
                        AsterGroupDivider()
                        AsterSettingsRow("WebDAV", leadingIcon = Icons.Outlined.Cloud, onClick = { navigate("webdav") })
                    }
                    if (repositories.isNotEmpty()) AsterSettingsGroup("目录") {
                        repositories.forEachIndexed { index, repo ->
                            if (index > 0) AsterGroupDivider()
                            AsterSettingsRow(repo.name, trailingText = "移除", onClick = { confirmation = "repo:${repo.id}" })
                        }
                    }
                }
                "shelves" -> {
                    AsterSettingsGroup {
                        AsterSettingsRow("新建书架", leadingIcon = Icons.Outlined.Add, onClick = {
                            editShelf = null; shelfName = ""; shelfEditor = true
                        })
                    }
                    shelves.forEachIndexed { index, shelf ->
                        AsterSettingsGroup(shelf.name) {
                            AsterSettingsRow("重命名", onClick = { editShelf = shelf; shelfName = shelf.name; shelfEditor = true })
                            AsterGroupDivider()
                            AsterSettingsRow("上移", enabled = index > 0, onClick = { viewModel.moveShelf(shelf, -1) })
                            AsterGroupDivider()
                            AsterSettingsRow("下移", enabled = index < shelves.lastIndex, onClick = { viewModel.moveShelf(shelf, 1) })
                            AsterGroupDivider()
                            AsterSettingsRow("删除书架", isDanger = true, onClick = { confirmation = "shelf:${shelf.id}" })
                        }
                    }
                }
                "tags" -> {
                    val tags = books.flatMap { it.tagsCsv.split(',') }.map(String::trim).filter(String::isNotEmpty).distinct().sorted()
                    if (tags.isEmpty()) Text("暂无标签", style = MaterialTheme.typography.bodyMedium)
                    else AsterSettingsGroup {
                        tags.forEachIndexed { index, tag ->
                            if (index > 0) AsterGroupDivider()
                            AsterSettingsRow(tag, onClick = { editTag = tag; tagName = tag })
                        }
                    }
                }
                "storage" -> AsterSettingsGroup {
                    AsterSettingsRow("清理缓存", onClick = { confirmation = "cache" })
                    AsterGroupDivider()
                    AsterSettingsRow("重建封面", onClick = viewModel::rebuildLibraryCovers)
                }
                "backup" -> {
                    AsterSettingsGroup("备份与恢复") {
                        AsterSettingsRow("加密导出", leadingIcon = Icons.Outlined.Lock,
                            onClick = { viewModel.chooseEncryptedBackupTarget(true) })
                        AsterGroupDivider()
                        AsterSettingsRow("加密恢复", leadingIcon = Icons.Outlined.LockOpen,
                            onClick = { viewModel.chooseEncryptedBackupTarget(false) })
                        AsterGroupDivider()
                        AsterSettingsRow("导出阅读记录", onClick = { confirmation = "export" })
                        AsterGroupDivider()
                        AsterSettingsRow("导入阅读记录", onClick = { confirmation = "restore" })
                    }
                }
                "about" -> AboutSettingsContent(viewModel) { navigate("licenses") }
                "licenses" -> OpenSourceSettingsContent(viewModel, onOpen = { navigate("license:${it.id}") })
                else -> if (section.startsWith("fileExport:")) FileVaultContent(uiState, viewModel, section)
                    else if (section.startsWith("license:")) OpenSourceSettingsContent(viewModel, section.removePrefix("license:"))
                    else Text("设置页面不可用")
            }
        }
    }
    if (confirmation != null) AlertDialog(
        onDismissRequest = { confirmation = null },
        title = { Text(when (confirmation) { "cache" -> "清理缓存"; "export" -> "导出阅读记录"; "restore" -> "导入阅读记录"; else -> "确认移除" }) },
        text = { Text(when (confirmation) {
            "cache" -> "仅清理可再生成的缓存，保留原文件和阅读进度。"
            "export" -> "包含书库、进度、标签、书架与笔记，不含文件本身。"
            "restore" -> "合并进度与笔记，按时间取较新的记录。"
            else -> if (confirmation?.startsWith("tag:") == true) "移除标签「${confirmation?.removePrefix("tag:")}」，作品保留。"
                else "移除索引，保留原文件和阅读进度。"
        }) },
        confirmButton = { TextButton(onClick = {
            val action = confirmation; confirmation = null
            when {
                action == "cache" -> viewModel.clearRegenerableCache()
                action == "export" -> export.launch("aster-library.json")
                action == "restore" -> restore.launch(arrayOf("application/json"))
                action?.startsWith("repo:") == true -> viewModel.deleteRepository(action.removePrefix("repo:"))
                action?.startsWith("shelf:") == true -> viewModel.deleteCustomShelf(action.removePrefix("shelf:"))
                action?.startsWith("tag:") == true -> viewModel.deleteTag(action.removePrefix("tag:"))
            }
        }) { Text("确定") } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text("取消") } }
    )
    if (shelfEditor) AlertDialog(onDismissRequest = { shelfEditor = false }, title = { Text("书架名称") },
        text = { OutlinedTextField(shelfName, { shelfName = it.take(80) }, singleLine = true, label = { Text("名称") }) },
        confirmButton = { TextButton(enabled = shelfName.isNotBlank() && shelves.none { it.id != editShelf?.id && it.name == shelfName.trim() }, onClick = {
            val shelf = editShelf
            if (shelf == null) viewModel.createCustomShelf(shelfName, "", false, onSaved = { shelfEditor = false })
            else viewModel.renameShelf(shelf.id, shelfName, onSaved = { shelfEditor = false })
        }) { Text("保存") } }, dismissButton = { TextButton(onClick = { shelfEditor = false }) { Text("取消") } })
    if (editTag != null) AlertDialog(onDismissRequest = { editTag = null }, title = { Text("编辑标签") },
        text = { OutlinedTextField(tagName, { tagName = it.take(80).replace(",", "") }, singleLine = true, label = { Text("名称") }) },
        confirmButton = { TextButton(enabled = tagName.isNotBlank(), onClick = {
            viewModel.renameTag(editTag!!, tagName.trim()); editTag = null
        }) { Text("保存") } }, dismissButton = {
            Row {
                TextButton(onClick = { confirmation = "tag:$editTag"; editTag = null }) {
                    Text("删除标签", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { editTag = null }) { Text("取消") }
            }
        })
}
