package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.asterreader.core.designsystem.AsterLayout
import com.example.data.local.BookEntity
import com.example.ui.AsterViewModel
import com.example.ui.components.AsterSubScreenHeader

/** 作品的持久配置使用独立页面，输入状态随页面保存。 */
@Composable
fun PublicationPropertiesScreen(book: BookEntity, section: String, model: AsterViewModel, onBack: () -> Unit) {
    var title by rememberSaveable(book.id) { mutableStateOf(book.title) }
    var author by rememberSaveable(book.id) { mutableStateOf(book.author) }
    var series by rememberSaveable(book.id) { mutableStateOf(book.series) }
    var tags by rememberSaveable(book.id) { mutableStateOf(book.tagsCsv) }
    var synopsis by rememberSaveable(book.id) { mutableStateOf(book.synopsis) }
    var order by rememberSaveable(book.id) { mutableStateOf(book.seriesOrder.toString()) }
    var cover by rememberSaveable(book.id) { mutableStateOf(book.coverPage.toString()) }
    var confirmExit by remember { mutableStateOf(false) }
    val modified = when (section) {
        "metadata" -> title != book.title || author != book.author || series != book.series || tags != book.tagsCsv || synopsis != book.synopsis
        "series" -> series != book.series || order != book.seriesOrder.toString()
        "cover" -> cover != book.coverPage.toString()
        else -> false
    }
    val requestBack: () -> Unit = { if (modified) confirmExit = true else onBack() }
    BackHandler(onBack = requestBack)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
        AsterSubScreenHeader(when (section) {
            "metadata" -> "编辑作品信息"; "file" -> "文件属性"; "cover" -> "选择封面页"; else -> "系列与卷序"
        }, requestBack)
        Column(Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth().align(Alignment.CenterHorizontally)
            .verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()
            .padding(horizontal = AsterLayout.PagePadding).padding(top = 4.dp, bottom = AsterLayout.BottomSpacing),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            when (section) {
                "metadata" -> {
                    OutlinedTextField(title, { title = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(author, { author = it }, label = { Text("作者") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(series, { series = it }, label = { Text("系列") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(tags, { tags = it }, label = { Text("标签") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(synopsis, { synopsis = it }, label = { Text("简介") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Button(enabled = title.isNotBlank(), onClick = {
                        model.updateBookMetadata(book.id, title, author, series, synopsis, tags, onSaved = onBack)
                    }) { Text("保存") }
                }
                "series" -> {
                    OutlinedTextField(series, { series = it }, label = { Text("系列名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(order, { order = it.filter(Char::isDigit).take(6) }, label = { Text("卷序") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(enabled = order.toIntOrNull() != null, onClick = {
                        model.moveSeriesOrder(book.id, series.trim(), order.toInt(), onSaved = onBack)
                    }) { Text("保存") }
                }
                "cover" -> {
                    val valid = cover.toIntOrNull()?.let { it in 1..book.totalPages } == true
                    OutlinedTextField(cover, { cover = it.filter(Char::isDigit).take(6) }, label = { Text("封面页（1–${book.totalPages}）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        isError = !valid, modifier = Modifier.fillMaxWidth())
                    Button(enabled = valid, onClick = { model.setCoverPage(book.id, cover.toInt(), onSaved = onBack) }) { Text("生成封面") }
                }
                "file" -> {
                    PropertyValue("名称", book.title)
                    PropertyValue("格式", book.format)
                    PropertyValue("大小", book.fileSizeText)
                    PropertyValue("页数", book.totalPages.toString())
                    PropertyValue("仓库", book.repositoryName)
                    PropertyValue("来源 URI", book.filePath)
                    PropertyValue("来源状态", when (book.sourceStatus) {
                        "AVAILABLE" -> "可读取"; "MISSING" -> "已失联"; "ERROR" -> "解析失败"; else -> book.sourceStatus
                    })
                }
            }
        }
    }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text("放弃未保存的修改？") },
        confirmButton = { TextButton(onClick = { confirmExit = false; onBack() }) { Text("放弃修改") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("继续编辑") } })
}

@Composable
private fun PropertyValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(value.ifBlank { "未设置" }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
