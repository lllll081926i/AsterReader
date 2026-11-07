package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.asterreader.core.designsystem.AsterLayout
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.components.AsterPairedActions

@Composable
fun EncryptedBackupForm(export: Boolean, state: AsterUiState, model: AsterViewModel, onBack: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
        Text(if (export) "仅备份公开清单，不含漫画、隐藏作品或 PIN。口令丢失无法恢复。"
            else "验证口令后合并公开清单。漫画文件需另行保存。",
            style = MaterialTheme.typography.bodyMedium)
        if (!state.encryptedBackupHasTarget) {
            FilledTonalButton(onClick = { model.chooseEncryptedBackupTarget(export) }) {
                Icon(Icons.Outlined.FolderOpen, null); Text("选择文件")
            }
        } else {
            OutlinedTextField(password, { password = it.take(256) }, Modifier.fillMaxWidth(),
                label = { Text(if (export) "口令（至少 12 字符）" else "口令") },
                visualTransformation = PasswordVisualTransformation(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = !state.encryptedBackupBusy)
            if (export) OutlinedTextField(repeated, { repeated = it.take(256) }, Modifier.fillMaxWidth(),
                label = { Text("确认口令") }, visualTransformation = PasswordVisualTransformation(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), enabled = !state.encryptedBackupBusy)
            state.encryptedBackupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.encryptedBackupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            else AsterPairedActions("取消", Icons.Outlined.Close, {
                password = ""; repeated = ""; model.clearEncryptedBackupTarget(); onBack()
            }, if (export) "加密导出" else "恢复", Icons.Outlined.Lock, {
                model.processEncryptedBackup(password.toCharArray(), export); password = ""; repeated = ""
            }, primaryOnRight = true, rightEnabled = if (export) password.length >= 12 && password == repeated else password.isNotEmpty())
        }
    }
}
