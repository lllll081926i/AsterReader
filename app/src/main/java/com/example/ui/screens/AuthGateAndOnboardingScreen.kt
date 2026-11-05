package com.example.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.asterreader.core.designsystem.AsterModalBottomSheet as ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.BotanicalGrimoireIllustration
import com.example.ui.components.VaultGeometricShieldIllustration
import com.example.ui.theme.LocalAsterColors
import kotlinx.coroutines.delay

/** 支持应用自建 PIN 码验证以及生物/系统凭据验证。 */
@Composable
fun SystemSecurityAuthPromptSheet(
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
    onAuthenticationStarted: () -> Unit = {},
    onAuthenticationFailed: () -> Unit = {},
    verifyCustomPin: (String) -> Boolean = { it == "1234" }
) {
    val context = LocalContext.current
    var inputPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var useSystemBiometric by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onAuthenticated()
        } else {
            onAuthenticationFailed()
            pinError = "系统验证已取消"
        }
    }

    val triggerSystemAuth = {
        val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        @Suppress("DEPRECATION")
        val intent = if (manager.isDeviceSecure) {
            manager.createConfirmDeviceCredentialIntent("安全验证", "使用生物识别或设备锁屏凭据")
        } else null
        if (intent == null) {
            pinError = "设备未设置系统锁屏凭据"
        } else {
            try {
                onAuthenticationStarted()
                launcher.launch(intent)
            } catch (_: Exception) {
                onAuthenticationFailed()
                pinError = "无法启动系统认证"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "安全解锁",
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "输入应用 PIN 码或调用系统生物识别验证身份：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = inputPin,
                    onValueChange = {
                        if (it.length <= 8) {
                            inputPin = it
                            pinError = null
                        }
                    },
                    label = { Text("PIN 码") },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { triggerSystemAuth() }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("使用生物/系统凭据", maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (verifyCustomPin(inputPin)) {
                        onAuthenticated()
                    } else {
                        pinError = "PIN 码不正确"
                    }
                }
            ) {
                Text("确定", maxLines = 1, softWrap = false)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", maxLines = 1, softWrap = false) }
        }
    )
}

@Composable
private fun SecurityBadgeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Onboarding Flow
 */
@Composable
fun OnboardingScreen(
    onImportFileClick: () -> Unit,
    onCreateRepoClick: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onFinish: () -> Unit
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme
    var step by remember { mutableIntStateOf(0) }
    var showSupportedFormatsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aster Reader (${step + 1}/3)",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.primary,
                maxLines = 1,
                softWrap = false
            )
            TextButton(onClick = onFinish) {
                Text("跳过", maxLines = 1, softWrap = false)
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BotanicalGrimoireIllustration(size = 160.dp)
            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                0 -> {
                    Text(
                        text = "安静、通透、书籍优先",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = asterColors.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OnboardingBullet("本地优先", "零广告、零云端上传，专注纯粹阅读。")
                            OnboardingBullet("专注漫画阅读", "支持 CBZ、PDF 漫画、双页展开与连续条漫。")
                            OnboardingBullet("隐私加密", "支持书籍加密保护与生物识别解锁。")
                        }
                    }
                }
                1 -> {
                    Text(
                        text = "导入本地书籍",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onImportFileClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "导入单本",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    text = "CBZ / PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = asterColors.surfaceContainerLow,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onCreateRepoClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreateNewFolder,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "添加目录",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "扫描书库文件夹",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "安全与访问控制",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = asterColors.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OnboardingBullet("应用锁", "使用系统锁屏凭据启动验证。")
                            OnboardingBullet("保险库状态", "当前版本尚不支持文件加密、密文校验和恢复密钥。")
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step == 0) {
                Button(
                    onClick = { step = 1 },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("开始使用", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { showSupportedFormatsDialog = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("支持格式", maxLines = 1, softWrap = false)
                }
            } else if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("下一步", maxLines = 1, softWrap = false)
                }
            } else {
                Button(
                    onClick = onFinish,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("进入书库", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = {
                        onFinish()
                        onOpenSecuritySettings()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("安全设置", maxLines = 1, softWrap = false)
                }
            }
        }
    }

    if (showSupportedFormatsDialog) {
        AlertDialog(
            onDismissRequest = { showSupportedFormatsDialog = false },
            title = { Text("支持格式", maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• 漫画：CBZ", style = MaterialTheme.typography.bodyMedium)
                    Text("• 文档：PDF", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showSupportedFormatsDialog = false }) {
                    Text("确定", maxLines = 1, softWrap = false)
                }
            }
        )
    }
}

@Composable
private fun OnboardingBullet(title: String, desc: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary,
            maxLines = 1
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
    }
}
