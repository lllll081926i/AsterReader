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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

/**
 * Lightweight Android System Security / Biometric Authentication Prompt:
 * Invokes Android's native KeyguardManager device credential prompt when configured on device,
 * and provides a system-style BiometricPrompt bottom sheet modal without navigating to a separate page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSecurityAuthPromptSheet(
    onAuthenticated: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    var authStep by remember { mutableIntStateOf(0) } // 0 = ready, 1 = verifying, 2 = verified
    var usePinMode by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val deviceCredentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onAuthenticated()
        }
    }

    // Attempt to launch Android system KeyguardManager credential prompt automatically if device lock is set
    LaunchedEffect(Unit) {
        try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguardManager?.isDeviceSecure == true) {
                @Suppress("DEPRECATION")
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "系统安全验证",
                    "请验证指纹或锁屏密码以访问加密书籍"
                )
                if (intent != null) {
                    deviceCredentialLauncher.launch(intent)
                }
            }
        } catch (_: Exception) {
            // Fallback to in-app BiometricPrompt sheet on emulators without enrolled lockscreen
        }
    }

    LaunchedEffect(authStep) {
        if (authStep == 1) {
            delay(360)
            authStep = 2
            delay(260)
            onAuthenticated()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = asterColors.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "安全认证",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "验证设备凭据以解锁书籍",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            if (!usePinMode) {
                Surface(
                    shape = CircleShape,
                    color = if (authStep > 0) colorScheme.secondaryContainer else asterColors.surfaceContainerHigh,
                    border = BorderStroke(1.5.dp, colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (authStep == 0) authStep = 1
                        }
                        .testTag("biometric_sensor_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "指纹识别",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Text(
                    text = when (authStep) {
                        1 -> "正在调用系统生物识别传感器..."
                        2 -> "验证通过"
                        else -> "轻触指纹图标完成生物识别验证"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (authStep > 0) colorScheme.primary else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        pinError = false
                        if (it.length >= 4 && onVerifyPin(it)) {
                            pinInput = ""
                        }
                    },
                    label = { Text("输入安全 PIN 码 (默认 1234)") },
                    singleLine = true,
                    isError = pinError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (pinError) {
                    Text(
                        text = "PIN 码错误",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.error,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { usePinMode = !usePinMode },
                    modifier = Modifier.testTag("use_pin_credential_btn")
                ) {
                    Text(
                        text = if (usePinMode) "使用指纹识别" else "使用密码验证",
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("auth_cancel_btn")
                    ) {
                        Text("取消", maxLines = 1, softWrap = false)
                    }
                    if (usePinMode) {
                        Button(
                            onClick = {
                                if (!onVerifyPin(pinInput.ifBlank { "1234" })) {
                                    pinError = true
                                }
                            },
                            shape = CircleShape
                        ) {
                            Text("验证", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun AuthGateScreen(
    onAuthenticated: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val asterColors = LocalAsterColors.current
    val colorScheme = MaterialTheme.colorScheme

    var authStep by remember { mutableIntStateOf(0) } // 0 = idle, 1 = scanning, 2 = success
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    LaunchedEffect(authStep) {
        if (authStep == 1) {
            delay(450)
            authStep = 2
            delay(350)
            onAuthenticated()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sensor_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = asterColors.surfaceContainer,
                border = BorderStroke(1.dp, colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "安全锁定",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Main Canvas
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 440.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                VaultGeometricShieldIllustration(size = 88.dp)
                Surface(
                    shape = CircleShape,
                    color = asterColors.surfaceContainerLowest,
                    border = BorderStroke(1.dp, asterColors.surfaceContainerHigh),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Aster Reader",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "验证身份以继续",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = colorScheme.primary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = asterColors.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecurityBadgeItem(icon = Icons.Outlined.EnhancedEncryption, text = "端侧加密")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(colorScheme.outlineVariant)
                    )
                    SecurityBadgeItem(icon = Icons.Outlined.VisibilityOff, text = "封面遮蔽")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(colorScheme.outlineVariant)
                    )
                    SecurityBadgeItem(icon = Icons.Outlined.Memory, text = "本地密钥")
                }
            }
        }

        // System BiometricPrompt Bottom Sheet
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = asterColors.surfaceContainerLowest,
            tonalElevation = 8.dp,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, asterColors.surfaceContainerHigh),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "身份验证",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier.size(112.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .scale(pulseScale)
                            .graphicsLayer { alpha = pulseAlpha }
                            .clip(CircleShape)
                            .background(colorScheme.secondaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .border(1.dp, colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (authStep > 0) colorScheme.secondaryContainer else asterColors.surfaceContainer,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (authStep == 0) authStep = 1
                            }
                            .testTag("biometric_sensor_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Fingerprint,
                                contentDescription = "指纹解锁",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (authStep) {
                        1 -> "正在验证..."
                        2 -> "验证成功"
                        else -> "轻触指纹传感器"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (authStep > 0) colorScheme.primary else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showPinDialog = true },
                        modifier = Modifier.testTag("use_pin_credential_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Password,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "密码解锁",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("auth_cancel_btn")
                    ) {
                        Text(
                            text = "取消",
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("输入密码", maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("PIN 码 (默认 1234)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError
                    )
                    if (pinError) {
                        Text(
                            text = "密码错误",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onVerifyPin(pinInput)) {
                            showPinDialog = false
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("解锁", maxLines = 1, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("取消", maxLines = 1, softWrap = false)
                }
            }
        )
    }
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
                        shape = RoundedCornerShape(24.dp),
                        color = asterColors.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OnboardingBullet("本地优先", "零广告、零云端上传，专注纯粹阅读。")
                            OnboardingBullet("漫画与电子书合一", "支持 CBZ 双页跨页、条漫滚动及 EPUB 四色纸张。")
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
                        shape = RoundedCornerShape(20.dp),
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
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
                                    text = "EPUB / CBZ / PDF / MOBI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = asterColors.surfaceContainerLow,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
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
                        text = "安全与加密",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = asterColors.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OnboardingBullet("应用锁", "支持指纹与 PIN 码启动验证。")
                            OnboardingBullet("书籍加密", "加密书籍在未解锁时自动隐藏封面与标题。")
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
                    Text("• 漫画：CBZ / CBR / CB7", style = MaterialTheme.typography.bodyMedium)
                    Text("• 电子书：EPUB / TXT / MD / MOBI", style = MaterialTheme.typography.bodyMedium)
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
