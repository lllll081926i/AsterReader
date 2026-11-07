package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterreader.core.designsystem.AsterModalBottomSheet
import com.asterreader.core.designsystem.LocalAsterReduceMotion
import com.asterreader.core.designsystem.asterBounceClick
import com.example.data.local.SystemBiometrics
import com.example.ui.AsterUiState
import com.example.ui.AsterViewModel
import com.example.ui.theme.LocalAsterColors
import com.example.ui.theme.LoraFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 从 Compose 的 Context 里找到宿主 Activity（系统生物识别需要 Activity 句柄）。 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

/**
 * 系统生物识别启动器：优先框架 BiometricPrompt，个别机型失败时回退系统锁屏凭据页。
 * 返回的 lambda 是一次「发起验证」，页面销毁时会自动取消在途验证。
 */
@Composable
fun rememberSystemBiometricLauncher(
    onSuccess: () -> Unit,
    onMessage: (String?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val fallback = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) onSuccess()
        else onMessage(null)
    }
    var cancelPending by remember { mutableStateOf<(() -> Unit)?>(null) }
    DisposableEffect(Unit) { onDispose { cancelPending?.invoke() } }

    return {
        if (activity == null) {
            onMessage("无法启动系统验证，请使用 PIN 码")
        } else {
            cancelPending?.invoke()
            cancelPending = SystemBiometrics.authenticate(
                activity = activity,
                onSuccess = {
                    cancelPending = null
                    onSuccess()
                },
                onFailure = { message ->
                    cancelPending = null
                    if (message != null && message.contains("无法启动")) {
                        val intent = SystemBiometrics.deviceCredentialIntent(context)
                        if (intent != null) runCatching { fallback.launch(intent) }
                            .onFailure { onMessage("无法启动系统验证，请使用 PIN 码") }
                        else onMessage("设备未设置锁屏凭据，请使用 PIN 码")
                    } else {
                        onMessage(message)
                    }
                }
            )
        }
    }
}

/**
 * 全屏应用锁。PIN 键盘 + 系统生物识别，验证通过后由 ViewModel 统一完成状态跃迁。
 * 不做任何文件内容改动：这里纯粹是「进入应用前的门禁」。
 */
@Composable
fun AppLockScreen(
    viewModel: AsterViewModel,
    uiState: AsterUiState,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val asterColors = LocalAsterColors.current
    val reduceMotion = LocalAsterReduceMotion.current
    val context = LocalContext.current

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shakeToken by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableLongStateOf(viewModel.pinLockRemainingSeconds()) }
    var autoPrompted by remember { mutableStateOf(false) }
    var checkingPin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val availability = remember(context) { SystemBiometrics.availability(context) }
    val biometricReady = uiState.biometricUnlockEnabled && availability == SystemBiometrics.Availability.READY

    val launchBiometric = rememberSystemBiometricLauncher(
        onSuccess = {
            viewModel.beginUnlockAttempt()
            viewModel.unlockAppAndVault()
        },
        onMessage = { message ->
            viewModel.cancelUnlockAttempt()
            error = message
        }
    )

    // 冷却倒计时；结束后自动清掉提示
    LaunchedEffect(uiState.pinLockedUntilMs) {
        while (viewModel.pinLockRemainingSeconds() > 0) {
            remainingSeconds = viewModel.pinLockRemainingSeconds()
            delay(1000)
        }
        remainingSeconds = 0
        if (error?.contains("30 秒") == true) error = null
    }

    // 进入锁屏时，如果设备支持且用户开启了生物识别，主动弹一次系统验证
    LaunchedEffect(biometricReady) {
        if (biometricReady && !autoPrompted) {
            autoPrompted = true
            viewModel.beginUnlockAttempt()
            launchBiometric()
        }
    }

    val submitPin = {
        if (remainingSeconds > 0) {
            error = "请 $remainingSeconds 秒后重试"
        } else if (pin.isEmpty()) {
            error = "请输入 PIN 码"
        } else if (!checkingPin) {
            checkingPin = true
            viewModel.beginUnlockAttempt()
            scope.launch {
                try {
                    if (viewModel.unlockWithPin(pin)) {
                        error = null
                    } else {
                        viewModel.cancelUnlockAttempt()
                        error = if (viewModel.pinLockRemainingSeconds() > 0) "尝试次数过多，请稍后重试" else "PIN 码不正确"
                        shakeToken++
                        pin = ""
                    }
                } finally { checkingPin = false }
            }
        }
    }

    val shade = Brush.verticalGradient(
        listOf(
            colorScheme.primaryContainer.copy(alpha = if (asterColors.isDark) 0.20f else 0.14f),
            colorScheme.surface,
            colorScheme.surface
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(shade)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = viewportHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                border = BorderStroke(0.8.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.size(70.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "书库已锁定",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                ),
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "输入应用 PIN 码继续阅读",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(30.dp))

            PinErrorShake(shakeToken, reduceMotion) {
                PinDots(pin.length)
            }

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error ?: if (remainingSeconds > 0) "请 $remainingSeconds 秒后重试" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) colorScheme.error else colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(14.dp))

            PinKeypad(
                enabled = remainingSeconds == 0L && !checkingPin,
                keySize = 66.dp,
                onDigit = { digit ->
                    if (pin.length < 8) pin += digit
                    error = null
                },
                onDelete = {
                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    error = null
                },
                onBiometric = if (biometricReady) {
                    {
                        viewModel.beginUnlockAttempt()
                        launchBiometric()
                    }
                } else null
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = submitPin,
                enabled = remainingSeconds == 0L && !checkingPin,
                shape = CircleShape,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "解锁",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    softWrap = false
                )
            }

        }
    }
}

/** 验证身份用的紧凑底部抽屉：隐藏作品等敏感入口使用，与应用锁共用同一套 PIN 键盘。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSecurityAuthPromptSheet(
    viewModel: AsterViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var shakeToken by remember { mutableIntStateOf(0) }
    val reduceMotion = LocalAsterReduceMotion.current
    val sheetState by viewModel.uiState.collectAsStateWithLifecycle()
    var checkingPin by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableLongStateOf(viewModel.pinLockRemainingSeconds()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(sheetState.pinLockedUntilMs) {
        do {
            remainingSeconds = viewModel.pinLockRemainingSeconds()
            if (remainingSeconds > 0) delay(1000)
        } while (remainingSeconds > 0)
    }

    val availability = remember(context) { SystemBiometrics.availability(context) }
    val biometricReady = sheetState.biometricUnlockEnabled &&
        availability == SystemBiometrics.Availability.READY

    val launchBiometric = rememberSystemBiometricLauncher(
        onSuccess = {
            viewModel.beginUnlockAttempt()
            viewModel.unlockAppAndVault()
        },
        onMessage = { message ->
            viewModel.cancelUnlockAttempt()
            error = message
        }
    )

    AsterModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "验证身份",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "输入应用 PIN 码或使用系统验证",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            PinErrorShake(shakeToken, reduceMotion) { PinDots(pin.length) }
            Box(Modifier.height(20.dp).padding(top = 2.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = if (remainingSeconds > 0) "请 $remainingSeconds 秒后重试" else error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) colorScheme.error else colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(10.dp))

            PinKeypad(
                enabled = remainingSeconds == 0L && !checkingPin,
                keySize = 52.dp,
                onDigit = { digit -> if (pin.length < 8) pin += digit; error = null },
                onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1); error = null },
                onBiometric = if (biometricReady) {
                    { viewModel.beginUnlockAttempt(); launchBiometric() }
                } else null
            )

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("取消") }
                FilledTonalButton(
                    onClick = {
                        checkingPin = true
                        viewModel.beginUnlockAttempt()
                        scope.launch {
                            try {
                                if (!viewModel.unlockWithPin(pin)) {
                                    viewModel.cancelUnlockAttempt()
                                    error = "PIN 码不正确"
                                    shakeToken++
                                    pin = ""
                                }
                            } finally { checkingPin = false }
                        }
                    },
                    enabled = pin.isNotEmpty() && remainingSeconds == 0L && !checkingPin,
                    shape = CircleShape
                ) { Text("解锁") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 共用视觉组件
// ---------------------------------------------------------------------------

@Composable
private fun PinErrorShake(token: Int, reduceMotion: Boolean, content: @Composable () -> Unit) {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(token) {
        if (token == 0 || reduceMotion) return@LaunchedEffect
        listOf(10f, -10f, 8f, -8f, 4f, -4f, 0f).forEach { value ->
            offset.animateTo(value, tween(45))
        }
    }
    Box(modifier = Modifier.graphicsLayer { translationX = offset.value }) {
        content()
    }
}

@Composable
private fun PinDots(count: Int) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(18.dp)
    ) {
        val slots = maxOf(4, count).coerceAtMost(8)
        repeat(slots) { index ->
            val filled = index < count
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) colorScheme.primary
                        else colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(
    enabled: Boolean,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onBiometric: (() -> Unit)?,
    keySize: Dp = 66.dp,
    modifier: Modifier = Modifier
) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    val gap = 16.dp
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { digit ->
                    PinKey(label = digit, enabled = enabled, size = keySize) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            if (onBiometric != null) {
                PinKey(
                    icon = Icons.Filled.Fingerprint,
                    description = "使用系统验证",
                    enabled = enabled,
                    prominent = true,
                    size = keySize,
                    onClick = onBiometric
                )
            } else {
                Spacer(Modifier.size(keySize))
            }
            PinKey(label = "0", enabled = enabled, size = keySize) { onDigit("0") }
            PinKey(
                icon = Icons.Filled.Backspace,
                description = "删除一位",
                enabled = enabled,
                size = keySize,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun PinKey(
    label: String? = null,
    icon: ImageVector? = null,
    description: String? = null,
    enabled: Boolean = true,
    prominent: Boolean = false,
    size: Dp,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (prominent) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
        contentColor = if (prominent) colorScheme.onPrimaryContainer else colorScheme.onSurface,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .asterBounceClick(enabled = enabled, targetScale = 0.92f) { if (enabled) onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                label != null -> Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                    color = colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
                )
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = description,
                    tint = if (prominent) colorScheme.onPrimaryContainer else colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
