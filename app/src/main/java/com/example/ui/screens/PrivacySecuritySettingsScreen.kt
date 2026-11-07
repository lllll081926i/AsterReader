package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.asterreader.core.designsystem.AsterLayout
import com.example.data.local.SystemBiometrics
import com.example.ui.*
import com.example.ui.components.*

@Composable
fun PrivacySecuritySettingsScreen(
    uiState: AsterUiState,
    viewModel: AsterViewModel,
    onBack: () -> Unit,
    section: String? = null
) {
    val back: () -> Unit = {
        if (section == "pin") viewModel.cancelPendingAppLockActivation()
        onBack()
    }
    BackHandler(onBack = back)
    val context = LocalContext.current
    val availability = remember(context) { SystemBiometrics.availability(context) }
    val biometricReady = availability == SystemBiometrics.Availability.READY
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.requestPinSetup) {
        if (uiState.requestPinSetup && section == null) {
            viewModel.consumePinSetupRequest()
            viewModel.pushSubScreen(SubScreen.SecuritySection("pin"))
        }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding()
        .testTag("privacy_security_settings_screen")) {
        AsterSubScreenHeader(when (section) { "pin" -> "应用 PIN"; "timeout" -> "自动锁定"; else -> "隐私与安全" }, back)
        Column(Modifier.weight(1f).widthIn(max = 840.dp).fillMaxWidth()
            .align(androidx.compose.ui.Alignment.CenterHorizontally).verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()
            .padding(horizontal = AsterLayout.PagePadding).padding(top = 4.dp, bottom = AsterLayout.BottomSpacing),
            verticalArrangement = Arrangement.spacedBy(AsterLayout.GroupSpacing)) {
            when (section) {
                "pin" -> {
                    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8); error = null },
                        label = { Text("新 PIN（4–8 位数字）") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(), isError = error != null)
                    OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(8); error = null },
                        label = { Text("再次输入 PIN") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(), isError = error != null)
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = {
                        when {
                            pin.length !in 4..8 -> error = "PIN 需为 4–8 位数字"
                            pin != confirmation -> error = "两次输入不一致"
                            else -> { viewModel.setCustomPin(pin); onBack() }
                        }
                    }) { Text("保存") }
                }
                "timeout" -> AsterSettingsGroup {
                    listOf("立即锁定", "30 秒后", "1 分钟后", "5 分钟后", "仅息屏锁定").forEachIndexed { index, label ->
                        if (index > 0) AsterGroupDivider()
                        Row(Modifier.fillMaxWidth().heightIn(min = AsterLayout.SettingsRowHeight)
                            .selectable(uiState.autoLockPolicyLabel == label, role = Role.RadioButton,
                                onClick = { viewModel.setAutoLockPolicyLabel(label) }).padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            RadioButton(uiState.autoLockPolicyLabel == label, onClick = null)
                        }
                    }
                }
                else -> {
                    AsterSettingsGroup("身份验证") {
                        AsterSwitchRow("应用锁", checked = uiState.isAppLockEnabled, onCheckedChange = viewModel::setAppLockEnabled)
                        AsterGroupDivider()
                        AsterSettingsRow("应用 PIN", badgeText = if (uiState.hasCustomPin) "已设置" else "未设置",
                            onClick = { viewModel.pushSubScreen(SubScreen.SecuritySection("pin")) })
                        AsterGroupDivider()
                        AsterSwitchRow("系统生物识别", subtitle = SystemBiometrics.hint(availability),
                            checked = uiState.biometricUnlockEnabled && biometricReady, enabled = biometricReady,
                            onCheckedChange = viewModel::setBiometricUnlockEnabled)
                    }
                    AsterSettingsGroup("锁定时机") {
                        AsterSettingsRow("自动锁定", badgeText = uiState.autoLockPolicyLabel,
                            onClick = { viewModel.pushSubScreen(SubScreen.SecuritySection("timeout")) })
                        AsterGroupDivider()
                        AsterSwitchRow("息屏锁定", checked = uiState.lockOnScreenOff, onCheckedChange = viewModel::setLockOnScreenOff)
                        AsterGroupDivider()
                        AsterSettingsRow("立即锁定", onClick = viewModel::lockAppImmediately)
                    }
                    AsterSettingsGroup("内容与防窥") {
                        AsterSettingsRow("隐藏作品（访问锁）", onClick = viewModel::openHiddenBooks)
                        AsterGroupDivider()
                        AsterSwitchRow("禁止截屏与录屏", checked = uiState.flagSecureEnabled, onCheckedChange = viewModel::setFlagSecureEnabled)
                        AsterGroupDivider()
                        AsterSwitchRow("隐藏最近任务预览", checked = uiState.hideRecentsPreview, onCheckedChange = viewModel::setHideRecentsPreview)
                    }
                }
            }
        }
    }
}
