package com.example.ui.screens
import com.asterreader.core.designsystem.AsterHeaderRow
import com.asterreader.core.designsystem.AsterHeaderTitleStyle
import com.asterreader.core.designsystem.asterHeaderAction

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocalLibrary
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
import androidx.compose.ui.platform.testTag
import com.asterreader.core.designsystem.AsterLayout
import com.asterreader.core.designsystem.asterBounceClick
import com.example.ui.components.BotanicalGrimoireIllustration
import com.example.ui.components.VaultGeometricShieldIllustration
import com.example.ui.theme.LocalAsterColors
import kotlinx.coroutines.delay

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
            .padding(horizontal = AsterLayout.PagePadding)
            .padding(bottom = AsterLayout.BottomSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AsterHeaderRow(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(min = AsterLayout.HeaderHeight)
                .testTag("onboarding_main_header"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Aster Reader",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = com.example.ui.theme.LoraFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colorScheme.primary,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val active = index == step
                        Box(
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (active) colorScheme.primary
                                    else colorScheme.outlineVariant.copy(alpha = 0.7f)
                                )
                                .size(width = if (active) 20.dp else 8.dp, height = 8.dp)
                        )
                    }
                    Text(
                        text = "${step + 1} / 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
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
                        text = "Aster Reader",
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
                            OnboardingBullet("本地优先", "书籍保存在本机。")
                            OnboardingBullet("格式支持", "ZIP / CBZ、RAR4 / CBR、7Z / CB7、TAR / CBT、PDF、图片型 EPUB。")
                            OnboardingBullet("隐私", "隐藏只限制应用内可见；保险库会加密另存一份。")
                        }
                    }
                }
                1 -> {
                    Text(
                        text = "导入书籍",
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
                            .asterBounceClick(onClick = onImportFileClick)
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
                                    text = "导入文件",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                                Text(
                                    text = "压缩包、PDF、图片",
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
                            .asterBounceClick(onClick = onCreateRepoClick)
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
                                    text = "扫描文件夹",
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
                        text = "安全设置",
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
                            OnboardingBullet("应用锁", "4–8 位 PIN，或系统指纹 / 面容 / 锁屏凭据。")
                            OnboardingBullet("防窥", "可禁止截屏，并隐藏任务预览。")
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
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("开始使用", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = { showSupportedFormatsDialog = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.Description, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("支持格式", maxLines = 1, softWrap = false)
                }
            } else if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("下一步", maxLines = 1, softWrap = false)
                }
            } else {
                Button(
                    onClick = onFinish,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.LocalLibrary, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
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
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
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
                    Text("• 漫画：CBZ / ZIP、CBR / RAR、CB7 / 7Z、CBT / TAR", style = MaterialTheme.typography.bodySmall)
                    Text("• 文档：PDF、图片型 EPUB", style = MaterialTheme.typography.bodySmall)
                    Text("• 图片：JPG / PNG / WebP 目录与单张图片", style = MaterialTheme.typography.bodySmall)
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
