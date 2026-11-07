package com.example.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** 连接配置与凭据一起加密；密钥不离开 Android Keystore。 */
internal class WebDavAccounts(context: Context, private val keyProvider: ((Boolean) -> SecretKey)? = null,
    private val directorySync: (File) -> Unit = ::syncDirectory, private val alias: String = "aster.webdav.accounts.v1") {
    private val file = File(context.noBackupFilesDir, "webdav-accounts.gcm")
    private val aad = alias.toByteArray(Charsets.UTF_8)

    @Synchronized fun read(): List<DavAccount> {
        if (!file.exists()) return emptyList()
        require(file.length() in 30..(1024 * 1024)) { "WebDAV 连接配置损坏" }
        val bytes = file.readBytes()
        require(bytes[0] == 1.toByte()) { "WebDAV 连接版本不受支持" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        cipher.updateAAD(aad)
        val plain = cipher.doFinal(bytes, 13, bytes.size - 13)
        return try {
            val array = JSONArray(String(plain, Charsets.UTF_8))
            List(array.length()) { index -> array.getJSONObject(index).let {
                DavAccount(it.getString("id"), it.getString("name"), it.getString("address"), it.getString("username"), it.getString("password"), it.optBoolean("allowHttp"))
            } }
        } finally { plain.fill(0) }
    }

    @Synchronized fun save(account: DavAccount) {
        val accounts = read().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index >= 0) accounts[index] = account else { require(accounts.size < 50) { "最多添加 50 个连接" }; accounts.add(account) }
        write(accounts)
    }

    @Synchronized fun remove(id: String) = write(read().filterNot { it.id == id })

    private fun write(accounts: List<DavAccount>) {
        val array = JSONArray()
        accounts.forEach { account -> array.put(JSONObject().put("id", account.id).put("name", account.name).put("address", account.address)
            .put("username", account.username).put("password", account.password).put("allowHttp", account.allowHttp)) }
        val plain = array.toString().toByteArray(Charsets.UTF_8)
        try {
            require(plain.size <= 1024 * 1024 - 29) { "连接配置超过大小上限，请减少连接数量" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(true)); cipher.updateAAD(aad)
            val bytes = byteArrayOf(1) + cipher.iv + cipher.doFinal(plain)
            val staging = File.createTempFile("dav-credentials-", ".part", file.parentFile)
            try {
                staging.outputStream().use { output -> output.write(bytes); output.fd.sync() }
                java.nio.file.Files.move(staging.toPath(), file.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                directorySync(requireNotNull(file.parentFile))
            } finally { staging.delete() }
        } finally { plain.fill(0) }
    }

    private fun key(create: Boolean): SecretKey {
        keyProvider?.let { return it(create) }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create && !file.exists()) { "WebDAV 凭据密钥不可用，请恢复设备密钥；不会覆盖原配置" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    companion object {
        private fun syncDirectory(directory: File) {
            val descriptor = android.system.Os.open(directory.absolutePath, android.system.OsConstants.O_RDONLY, 0)
            try { android.system.Os.fsync(descriptor) } finally { android.system.Os.close(descriptor) }
        }
    }
}
