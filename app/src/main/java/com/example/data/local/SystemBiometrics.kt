package com.example.data.local

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

/**
 * 直接调用系统生物识别（框架 BiometricPrompt，minSdk 31 起全设备可用）。
 *
 * 允许「强生物识别 或 设备锁屏凭据」：有指纹/面容走生物识别，
 * 只设了锁屏密码的设备会直接弹系统凭据确认，不依赖应用自己实现密码框。
 * 应用不接触也不保存任何生物特征数据。
 */
object SystemBiometrics {

    enum class Availability { READY, NEEDS_ENROLLMENT, NO_HARDWARE, UNAVAILABLE }

    private const val ALLOWED =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun availability(context: Context): Availability {
        val manager = context.getSystemService(BiometricManager::class.java)
            ?: return Availability.UNAVAILABLE
        return when (runCatching { manager.canAuthenticate(ALLOWED) }.getOrNull()) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NEEDS_ENROLLMENT
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NO_HARDWARE
            else -> Availability.UNAVAILABLE
        }
    }

    fun hint(availability: Availability): String = when (availability) {
        Availability.READY -> "可使用指纹、面容或设备锁屏凭据解锁"
        Availability.NEEDS_ENROLLMENT -> "请先在系统设置中录入生物识别或锁屏凭据"
        Availability.NO_HARDWARE -> "此设备没有生物识别硬件，请使用 PIN 码"
        Availability.UNAVAILABLE -> "系统验证暂不可用，请使用 PIN 码"
    }

    /** 系统凭据兜底：极少数机型不支持组合验证时，退回系统锁屏确认页。 */
    @Suppress("DEPRECATION")
    fun deviceCredentialIntent(context: Context): Intent? {
        val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return if (manager?.isDeviceSecure == true) {
            manager.createConfirmDeviceCredentialIntent("验证身份", "使用生物识别或设备锁屏凭据解锁 Aster Reader")
        } else null
    }

    /**
     * 弹出系统验证。返回「取消本次验证」的句柄；宿主在页面销毁时调用，避免回调泄漏。
     * onFailure 传入 null 表示用户主动取消（不视为失败）。
     */
    fun authenticate(
        activity: Activity,
        title: String = "验证身份",
        subtitle: String = "解锁 Aster Reader",
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ): () -> Unit {
        val signal = CancellationSignal()
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                onFailure("验证未通过，请重试")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                val cancelled = errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                    errorCode == BiometricPrompt.BIOMETRIC_ERROR_TIMEOUT
                onFailure(if (cancelled) null else errString?.toString() ?: "系统验证未完成")
            }
        }

        try {
            val builder = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setConfirmationRequired(false)
            val prompt = try {
                builder.setAllowedAuthenticators(ALLOWED).build()
            } catch (_: IllegalArgumentException) {
                // 个别 OEM 不接受「生物识别 + 设备凭据」组合，退回仅生物识别 + 负按钮
                BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButton(
                        "使用 PIN 码",
                        activity.mainExecutor,
                        android.content.DialogInterface.OnClickListener { _, _ -> onFailure(null) }
                    )
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setConfirmationRequired(false)
                    .build()
            }
            prompt.authenticate(signal, activity.mainExecutor, callback)
        } catch (_: Throwable) {
            onFailure("无法启动系统验证，请使用 PIN 码")
        }
        return { runCatching { signal.cancel() } }
    }
}
