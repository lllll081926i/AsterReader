package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterreader.core.designsystem.AsterLayout
import com.example.data.local.RepositoryEntity
import com.example.data.local.ShelfEntity
import com.example.ui.*
import com.example.ui.components.*

private val sectionTitles = mapOf(
    "appearance" to "外观与主题", "reading" to "默认阅读配置", "sources" to "本地仓库",
    "shelves" to "书架管理", "tags" to "标签管理", "storage" to "缓存与空间",
    "backup" to "备份与恢复", "stats" to "阅读统计", "about" to "关于", "diagnostics" to "诊断与帮助"
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
    if (onBack != null) BackHandler(onBack = onBack)
    val context = LocalContext.current
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
        AsterSubScreenHeader(sectionTitles[section] ?: "设置", onBack)
        Column(Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth()
            .align(androidx.compose.ui.Alignment.CenterHorizontally).verticalScroll(rememberScrollState())
            .navigationBarsPadding().padding(horizontal = AsterLayout.PagePadding)
            .padding(top = 4.dp, bottom = AsterLayout.BottomSpacing),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            when (section) {
                null -> {
                    AsterSettingsGroup("外观与阅读") {
                        AsterSettingsRow("外观与主题", leadingIcon = Icons.Outlined.Palette, onClick = { navigate("appearance") })
                        AsterGroupDivider()
                        AsterSettingsRow("默认阅读配置", leadingIcon = Icons.Outlined.MenuBook, onClick = { navigate("reading") })
                        AsterGroupDivider()
                        AsterSettingsRow("阅读统计", leadingIcon = Icons.Outlined.Schedule, onClick = { navigate("stats") })
                    }
                    AsterSettingsGroup("内容与仓库") {
                        AsterSettingsRow("本地仓库", leadingIcon = Icons.Outlined.Folder, onClick = { navigate("sources") })
                        AsterGroupDivider()
                        AsterSettingsRow("书架管理", leadingIcon = Icons.Outlined.CollectionsBookmark, onClick = { navigate("shelves") })
                        AsterGroupDivider()
                        AsterSettingsRow("标签管理", leadingIcon = Icons.Outlined.Tag, onClick = { navigate("tags") })
                        AsterGroupDivider()
                        AsterSettingsRow("缓存与空间", leadingIcon = Icons.Outlined.Storage, onClick = { navigate("storage") })
                    }
                    AsterSettingsGroup("隐私与安全") {
                        AsterSettingsRow("应用锁与访问控制", leadingIcon = Icons.Outlined.Lock,
                            onClick = { viewModel.pushSubScreen(SubScreen.PrivacySecuritySettings) })
                    }
                    AsterSettingsGroup("数据与支持") {
                        AsterSettingsRow("备份与恢复", leadingIcon = Icons.Outlined.Backup, onClick = { navigate("backup") })
                        AsterGroupDivider()
                        AsterSettingsRow("诊断与帮助", leadingIcon = Icons.Outlined.Info, onClick = { navigate("diagnostics") })
                        AsterGroupDivider()
                        AsterSettingsRow("关于", leadingIcon = Icons.Outlined.Apps, onClick = { navigate("about") })
                    }
                }
                "appearance" -> {
                    AsterSettingsGroup("主题") {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            if (index > 0) AsterGroupDivider()
                            SettingsRadioRow(mode.label, uiState.themeMode == mode) { viewModel.setThemeMode(mode) }
                        }
                    }
                    AsterSettingsGroup("动效与首页") {
                        AsterSwitchRow("减少动效", checked = uiState.reduceMotion, onCheckedChange = viewModel::setReduceMotion)
                        AsterGroupDivider()
                        AsterSwitchRow("继续阅读", checked = uiState.showRecentReading, onCheckedChange = viewModel::setShowRecentReading)
                    }
                }
                "reading" -> {
                    AsterSettingsGroup("阅读模式") {
                        ReaderLayoutMode.entries.forEachIndexed { index, mode ->
                            if (index > 0) AsterGroupDivider()
                            SettingsRadioRow(mode.label, uiState.readerLayoutMode == mode) { viewModel.setReaderLayoutMode(mode) }
                        }
                    }
                    AsterSettingsGroup("画面适配") {
                        listOf("适合屏幕", "原始比例").forEachIndexed { index, fit ->
                            if (index > 0) AsterGroupDivider()
                            SettingsRadioRow(fit, uiState.fitModeLabel == fit) { viewModel.setFitModeLabel(fit) }
                        }
                    }
                    AsterSettingsGroup("翻页") {
                        AsterSwitchRow("音量键翻页", checked = uiState.volumeKeyPageTurn, onCheckedChange = viewModel::setVolumeKeyPageTurn)
                        AsterGroupDivider()
                        AsterSwitchRow("封面单独显示", checked = uiState.smartSpreadStitch, onCheckedChange = viewModel::setSmartSpreadStitch)
                    }
                }
                "stats" -> AsterSettingsGroup {
                    AsterSwitchRow("记录阅读时长", checked = uiState.showReadingStats, onCheckedChange = viewModel::setShowReadingStats)
                    if (uiState.showReadingStats) {
                        AsterGroupDivider()
                        AsterSettingsRow("查看统计", onClick = { viewModel.pushSubScreen(SubScreen.ReadingStats) })
                    }
                }
                "sources" -> {
                    AsterSettingsGroup {
                        AsterSettingsRow("导入文件", leadingIcon = Icons.Outlined.Add, onClick = onTriggerImportFile)
                        AsterGroupDivider()
                        AsterSettingsRow("添加仓库", leadingIcon = Icons.Outlined.CreateNewFolder,
                            onClick = { viewModel.addRepositoryAndScan("本地书库", com.example.data.local.LibraryRepository.DEFAULT_SCAN_RULE) })
                    }
                    if (repositories.isNotEmpty()) AsterSettingsGroup("已添加") {
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
                    shelves.forEach { shelf ->
                        AsterSettingsGroup(shelf.name) {
                            AsterSettingsRow("重命名", onClick = { editShelf = shelf; shelfName = shelf.name; shelfEditor = true })
                            AsterGroupDivider()
                            AsterSettingsRow("上移", onClick = { viewModel.moveShelf(shelf, -1) })
                            AsterGroupDivider()
                            AsterSettingsRow("下移", onClick = { viewModel.moveShelf(shelf, 1) })
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
                    AsterSettingsRow("清理可再生缓存", onClick = { confirmation = "cache" })
                    AsterGroupDivider()
                    AsterSettingsRow("重建封面", onClick = viewModel::rebuildLibraryCovers)
                }
                "backup" -> AsterSettingsGroup {
                    AsterSettingsRow("导出公开书库清单", onClick = { confirmation = "export" })
                    AsterGroupDivider()
                    AsterSettingsRow("合并恢复书库清单", onClick = { confirmation = "restore" })
                }
                "diagnostics" -> AsterSettingsGroup {
                    SettingsValueRow("Android", android.os.Build.VERSION.RELEASE)
                    AsterGroupDivider()
                    SettingsValueRow("数据库版本", "2")
                    AsterGroupDivider()
                    SettingsValueRow("公开作品", booksCount.toString())
                    AsterGroupDivider()
                    SettingsValueRow("可用空间", "${context.filesDir.usableSpace / 1_048_576} MB")
                    AsterGroupDivider()
                    SettingsValueRow("仓库状态", "${repositories.count { it.isHealthy }} / ${repositories.size}")
                }
                "about" -> AsterSettingsGroup {
                    SettingsValueRow("Aster Reader", context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty())
                    AsterGroupDivider()
                    SettingsValueRow("应用包名", context.packageName)
                }
                else -> Text("设置页面不可用")
            }
        }
    }
    if (confirmation != null) AlertDialog(
        onDismissRequest = { confirmation = null },
        title = { Text(when (confirmation) { "cache" -> "清理缓存"; "export" -> "导出书库清单"; "restore" -> "合并恢复"; else -> "确认移除" }) },
        text = { Text(when (confirmation) {
            "cache" -> "清理封面与阅读副本。原文件和阅读记录将保留。"
            "export" -> "导出公开作品的进度、书架和批注。不包含隐藏作品、漫画文件和安全凭据。"
            "restore" -> "恢复前会验证文件。保留本机来源；按阅读时间合并进度；重复批注只保留一份。"
            else -> if (confirmation?.startsWith("tag:") == true) "从所有公开作品中移除标签「${confirmation?.removePrefix("tag:")}」。作品和阅读记录将保留。"
                else "移除分类或仓库关联，保留原文件和作品阅读记录。"
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
        }) { Text("继续") } },
        dismissButton = { TextButton(onClick = { confirmation = null }) { Text("取消") } }
    )
    if (shelfEditor) AlertDialog(onDismissRequest = { shelfEditor = false }, title = { Text("书架名称") },
        text = { OutlinedTextField(shelfName, { shelfName = it.take(80) }, singleLine = true, label = { Text("名称") }) },
        confirmButton = { TextButton(enabled = shelfName.isNotBlank() && shelves.none { it.id != editShelf?.id && it.name == shelfName.trim() }, onClick = {
            val shelf = editShelf
            if (shelf == null) viewModel.createCustomShelf(shelfName, "", false) else viewModel.renameShelf(shelf.id, shelfName)
            shelfEditor = false
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

@Composable
private fun SettingsRadioRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = AsterLayout.SettingsRowHeight)
        .selectableSetting(selected, onClick).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        RadioButton(selected, onClick = null)
    }
}

private fun Modifier.selectableSetting(selected: Boolean, onClick: () -> Unit): Modifier =
    this.then(Modifier.selectable(selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick))

@Composable
private fun SettingsValueRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = AsterLayout.SettingsRowHeight).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text(value, Modifier.widthIn(max = 180.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
