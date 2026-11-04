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

/** 仅接受系统锁屏凭据验证结果。 */
@Composable
fun SystemSecurityAuthPromptSheet(
    onAuthenticated: () -> Unit,
    onDismiss: () -> Unit,
    onAuthenticationStarted: () -> Unit = {},
    onAuthenticationFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onAuthenticated()
        else { onAuthenticationFailed(); error = "身份验证已取消，请重试" }
    }
    val authenticate = {
        val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        @Suppress("DEPRECATION")
        val intent = if (manager.isDeviceSecure) manager.createConfirmDeviceCredentialIntent(
            "解锁 Aster Reader", "请使用系统锁屏凭据验证身份"
        ) else null
        if (intent == null) error = "请先在系统设置中配置安全锁屏"
        else try { onAuthenticationStarted(); launcher.launch(intent) } catch (_: Exception) {
            onAuthenticationFailed()
            error = "无法启动系统身份验证，请稍后重试"
        }
    }
    LaunchedEffect(Unit) { authenticate() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("身份验证") },
        text = { Text(error ?: "使用系统锁屏凭据以继续。应用不会模拟指纹认证。") },
        confirmButton = { TextButton(onClick = { authenticate() }) { Text("验证身份") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
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
