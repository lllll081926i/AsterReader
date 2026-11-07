package com.example.data.local.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface VaultDeviceKey {
    fun encrypt(bytes: ByteArray, create: Boolean): ByteArray
    fun decrypt(bytes: ByteArray): ByteArray
    fun sourceTag(uri: String, create: Boolean = false): String
}

/** 密钥缺失时只允许首次初始化生成，禁止覆盖已有保险库。 */
internal class AndroidVaultDeviceKey(private val prefix: String = "aster.vault") : VaultDeviceKey {
    private val store get() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key(alias: String, create: Boolean, hmac: Boolean = false): SecretKey {
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create) { "设备密钥不可用，请从加密导出文件恢复" }
        val algorithm = if (hmac) KeyProperties.KEY_ALGORITHM_HMAC_SHA256 else KeyProperties.KEY_ALGORITHM_AES
        val purpose = if (hmac) KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY else
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        val spec = KeyGenParameterSpec.Builder(alias, purpose).setKeySize(256).setUserAuthenticationRequired(false)
        if (hmac) spec.setDigests(KeyProperties.DIGEST_SHA256)
        else spec.setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        return KeyGenerator.getInstance(algorithm, "AndroidKeyStore").apply { init(spec.build()) }.generateKey()
    }
    override fun encrypt(bytes: ByteArray, create: Boolean): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key("$prefix.device.v2", create))
        cipher.updateAAD("aster-device-slot-v2".toByteArray())
        return cipher.iv + cipher.doFinal(bytes)
    }
    override fun decrypt(bytes: ByteArray): ByteArray {
        require(bytes.size == 124) { "设备密钥槽损坏" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key("$prefix.device.v2", false), GCMParameterSpec(128, bytes.copyOf(12)))
        cipher.updateAAD("aster-device-slot-v2".toByteArray())
        return cipher.doFinal(bytes, 12, bytes.size - 12)
    }
    override fun sourceTag(uri: String, create: Boolean): String = Mac.getInstance("HmacSHA256").apply {
        init(key("$prefix.source.v2", create, true))
    }.doFinal(uri.toByteArray()).joinToString("") { "%02x".format(it) }
}
