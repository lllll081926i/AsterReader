package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookEntity
import com.example.data.local.RepositoryEntity
import com.example.data.local.ShelfEntity
import com.example.data.local.SystemBiometrics
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.LibraryLayoutMode
import com.example.ui.ReaderLayoutMode
import com.example.ui.ShelvesViewMode
import com.example.ui.SubScreen
import com.example.ui.ThemeMode
import com.example.ui.components.AsterBookCover
import com.example.ui.components.AsterSettingsGroup
import com.example.ui.components.AsterSettingsRow
import com.example.ui.components.AsterSwitchRow
import com.example.ui.components.AsterSubScreenHeader
import com.example.ui.components.BotanicalGrimoireIllustration
import com.example.ui.components.SafeProtectedBookCard
import com.example.ui.components.TranquilDeskEmptyIllustration
import com.example.ui.components.VaultGeometricShieldIllustration
import com.example.ui.theme.RobotoMonoFontFamily

@Composable
fun PrivacySecuritySettingsScreen(
    uiState: AsterUiState,
    viewModel: AsterViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    // 离开页面即撤销「待开启应用锁」，避免用户跳过 PIN 设置后留下弱锁状态
    DisposableEffect(Unit) {
        onDispose { viewModel.cancelPendingAppLockActivation() }
    }
    val context = LocalContext.current
    val availability = remember(context) { SystemBiometrics.availability(context) }
    val biometricReady = availability == SystemBiometrics.Availability.READY

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var tempPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // 首次开启应用锁时引导设置自己的 PIN，而不是一直用默认 1234
    LaunchedEffect(uiState.requestPinSetup) {
        if (uiState.requestPinSetup) {
            tempPin = ""
            confirmPin = ""
            pinError = null
            showPinDialog = true
            viewModel.consumePinSetupRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .testTag("privacy_security_settings_screen")
    ) {
        AsterSubScreenHeader(
            title = "隐私与安全",
            onBack = onBack,
            subtitle = "访问控制都在本机完成"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 顶部说明：明确「不加密文件」，避免用户误以为书籍被改写
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "只锁入口，不动文件",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "应用锁与隐藏作品只改变可见性，书籍文件保持原样，PIN 只保存在本机。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            AsterSettingsGroup(
                title = "应用锁",
            ) {
                AsterSwitchRow(
                    title = "启用应用锁",
                    subtitle = "未验证时书库内容不会加载",
                    checked = uiState.isAppLockEnabled,
                    onCheckedChange = { viewModel.setAppLockEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.Key,
                    title = "应用 PIN 码",
                    subtitle = "4–8 位数字，仅保存加盐摘要",
                    badgeText = if (uiState.hasCustomPin) "已设置" else "默认 1234",
                    onClick = {
                        tempPin = ""
                        confirmPin = ""
                        pinError = null
                        showPinDialog = true
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "系统生物识别",
                    subtitle = SystemBiometrics.hint(availability),
                    checked = uiState.biometricUnlockEnabled && biometricReady,
                    enabled = biometricReady,
                    onCheckedChange = { viewModel.setBiometricUnlockEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.LockClock,
                    title = "自动锁定",
                    subtitle = "离开应用多久后重新上锁",
                    badgeText = uiState.autoLockPolicyLabel,
                    onClick = { showTimeoutDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "息屏后锁定",
                    subtitle = "屏幕关闭时立即上锁",
                    checked = uiState.lockOnScreenOff,
                    onCheckedChange = { viewModel.setLockOnScreenOff(it) }
                )
            }

            AsterSettingsGroup(
                title = "隐藏作品",
            ) {
                val hiddenCount = viewModel.hiddenCount.collectAsStateWithLifecycle().value
                AsterSettingsRow(
                    leadingIcon = Icons.Outlined.VisibilityOff,
                    title = "已隐藏作品",
                    subtitle = "文件不加密，验证后照常阅读",
                    badgeText = if (hiddenCount > 0) "$hiddenCount 本" else "无",
                    onClick = { viewModel.openHiddenBooks() }
                )
            }

            AsterSettingsGroup(
                title = "防窥",
            ) {
                AsterSwitchRow(
                    title = "防截屏保护",
                    subtitle = "禁止系统截屏与录屏",
                    checked = uiState.flagSecureEnabled,
                    onCheckedChange = { viewModel.setFlagSecureEnabled(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                AsterSwitchRow(
                    title = "多任务模糊遮罩",
                    subtitle = "最近任务列表里隐藏画面",
                    checked = uiState.hideRecentsPreview,
                    onCheckedChange = { viewModel.setHideRecentsPreview(it) }
                )
            }

            AsterSettingsGroup(
                title = "会话",
            ) {
                AsterSettingsRow(
                    leadingIcon = Icons.Default.LockOpen,
                    title = "立即锁定",
                    subtitle = "马上回到验证界面",
                    onClick = { viewModel.lockAppImmediately() }
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showTimeoutDialog) {
        val options = listOf("立即锁定", "30 秒后", "1 分钟后", "5 分钟后", "仅息屏锁定")
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("自动锁定时间", fontWeight = FontWeight.SemiBold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    options.forEach { option ->
                        val selected = uiState.autoLockPolicyLabel == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    viewModel.setAutoLockPolicyLabel(option)
                                    showTimeoutDialog = false
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    viewModel.setAutoLockPolicyLabel(option)
                                    showTimeoutDialog = false
                                }
                            )
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("关闭", maxLines = 1, softWrap = false) }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                viewModel.cancelPendingAppLockActivation()
            },
            title = { Text("设置应用 PIN 码", fontWeight = FontWeight.SemiBold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "4–8 位数字，需二次确认。应用锁开启时不能清除 PIN。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { tempPin = it; pinError = null } },
                        label = { Text("新 PIN 码") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) { confirmPin = it; pinError = null } },
                        label = { Text("再次输入") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (uiState.hasCustomPin && !uiState.isAppLockEnabled) {
                        TextButton(onClick = {
                            viewModel.setCustomPin("")
                            showPinDialog = false
                        }) { Text("清除自定义 PIN") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            tempPin.isNotEmpty() && tempPin.length !in 4..8 -> pinError = "PIN 码需为 4–8 位数字"
                            tempPin != confirmPin -> pinError = "两次输入不一致"
                            else -> {
                                viewModel.setCustomPin(tempPin)
                                showPinDialog = false
                            }
                        }
                    }
                ) { Text("保存", maxLines = 1, softWrap = false) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    viewModel.cancelPendingAppLockActivation()
                }) { Text("取消", maxLines = 1, softWrap = false) }
            }
        )
    }
}
