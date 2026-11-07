package com.example.data.local.vault

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import com.asterreader.core.vault.VaultCryptography as Crypto
import com.asterreader.core.vault.VaultPassword
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** 只在 IO 线程调用耗时操作。锁定通过会话代次立即撤销在途读取。 */
internal class FileVaultStore private constructor(
    context: Context, private val device: VaultDeviceKey, private val directorySync: (File) -> Unit
) {
    private val root = File(context.noBackupFilesDir, "file-vault-v2").apply { mkdirs() }
    private val slot = AtomicFile(File(root, "device.slot"))
    private val guard = Any()
    private val writes = Any()
    private var master: ByteArray? = null
    private val generation = AtomicLong()
    private val readers = mutableSetOf<Crypto.Reader>()
    private val mutableRecords = MutableStateFlow<Map<String, VaultRecord>>(emptyMap())
    val records = mutableRecords.asStateFlow()
    val configured get() = slot.baseFile.exists() || File(root, "device.slot.bak").exists()
    val unlocked get() = synchronized(guard) { master != null }
    fun sessionToken(): Long = generation.get()
    fun isSessionValid(token: Long): Boolean = token == generation.get() && unlocked
    fun committedIds(): Set<String> = root.listFiles().orEmpty().filter { it.name.endsWith(".vmeta") }
        .map { it.name.removeSuffix(".vmeta") }.toSet()
    fun sourceTag(uri: String) = device.sourceTag(uri)
    private fun readSlot(): ByteArray = slot.openRead().use { input ->
        require(input.channel.size() == 124L) { "设备密钥槽损坏" }
        ByteArray(124).also { DataInputStream(input).readFully(it) }
    }

    fun lock() {
        generation.incrementAndGet()
        val closing = synchronized(guard) {
            master?.fill(0); master = null
            mutableRecords.value = emptyMap()
            readers.toList().also { readers.clear() }
        }
        closing.forEach { runCatching { it.close() } }
    }

    fun open(password: CharArray, create: Boolean, checkActive: () -> Unit = {}) {
        val attempt = generation.get()
        val candidate: ByteArray
        if (create) {
            require(!configured && committedIds().isEmpty()) { "保险库已经存在，请解锁" }
            VaultPassword.validate(password)
            candidate = Crypto.randomKey()
            try {
                val wrapped = VaultPassword.wrap(candidate, password)
                try {
                    val protected = device.encrypt(wrapped, true)
                    device.sourceTag("initialize", true)
                    checkActive(); check(attempt == generation.get()) { "操作已撤销" }
                    atomic(slot, protected)
                    val verified = device.decrypt(readSlot())
                    try {
                        val recovered = VaultPassword.unwrap(verified, password)
                        try { require(MessageDigest.isEqual(candidate, recovered)) } finally { recovered.fill(0) }
                    } finally { verified.fill(0) }
                } finally { wrapped.fill(0) }
            } catch (failure: Throwable) { candidate.fill(0); throw failure }
        } else {
            reserveAttempt()
            val wrapped = device.decrypt(readSlot())
            candidate = try { VaultPassword.unwrap(wrapped, password) } finally { wrapped.fill(0) }
        }
        try {
            checkActive(); check(attempt == generation.get()) { "操作已撤销" }
            val loaded = linkedMapOf<String, VaultRecord>()
            for (id in committedIds()) {
                checkActive()
                val bytes = Crypto.Reader(File(root, "$id.vmeta"), candidate).use { reader ->
                    require(reader.length <= VaultRecord.MAX_METADATA)
                    reader.stream().use { it.readBytes() }
                }
                val record = try { VaultRecord.decode(bytes) } finally { bytes.fill(0) }
                require(record.book.id == id && File(root, record.dataName).isFile) { "保险库目录损坏" }
                Crypto.Reader(File(root, record.dataName), candidate).use { require(it.objectId.toString() + ".avd" == record.dataName) }
                loaded[id] = record
            }
            synchronized(guard) {
                checkActive(); check(attempt == generation.get()) { "操作已撤销" }
                master?.fill(0); master = candidate.copyOf()
                mutableRecords.value = loaded
            }
            resetAttempts()
        } finally { candidate.fill(0) }
    }

    fun changePassword(current: CharArray, next: CharArray) = session { key, active ->
        reserveAttempt()
        val original = device.decrypt(readSlot())
        try {
            val verified = VaultPassword.unwrap(original, current)
            try { require(MessageDigest.isEqual(key, verified)) } finally { verified.fill(0) }
        } finally { original.fill(0) }
        val wrapped = VaultPassword.wrap(key, next)
        try {
            val protected = device.encrypt(wrapped, false)
            val recovered = device.decrypt(protected)
            try {
                val verified = VaultPassword.unwrap(recovered, next)
                try { require(MessageDigest.isEqual(key, verified)) } finally { verified.fill(0) }
            } finally { recovered.fill(0) }
            active(); atomic(slot, protected); resetAttempts()
        } finally { wrapped.fill(0) }
    }

    fun create(record: VaultRecord, length: Long, input: () -> InputStream, active: () -> Unit = {}): VaultRecord =
        synchronized(writes) { session { key, valid ->
            require(record.book.id !in committedIds()) { "保险库已有此作品" }
            val objectId = UUID.randomUUID()
            val dataName = "$objectId.avd"
            val staging = File(root, "$objectId.part")
            val check = { active(); valid() }
            val digest = input().use { Crypto.write(staging, key, length, it, objectId, check) }
            try { Crypto.verify(staging, key, digest, checkActive = check) } finally { digest.fill(0) }
            check()
            require(staging.renameTo(File(root, dataName))) { "无法提交加密文件" }
            val saved = record.copy(dataName = dataName, book = record.book.copy(isVaultEncrypted = true,
                isVaultProtected = true, filePath = "vault://${record.book.id}", coverUrl = "vault-cover://${record.book.id}",
                altCoverUrl = "", repositoryId = "", repositoryName = "保险库", sourceStatus = "AVAILABLE", contentFingerprint = ""))
            persist(saved, key, check)
            publish(saved, check)
            saved
        } }

    fun update(id: String, transform: (VaultRecord) -> VaultRecord) = synchronized(writes) {
        session { key, active ->
            val current = records.value[id] ?: error("请先解锁文件保险库")
            val next = transform(current)
            require(next.book.id == id && next.dataName == current.dataName)
            persist(next, key, active)
            publish(next, active)
        }
    }

    private fun persist(record: VaultRecord, key: ByteArray, active: () -> Unit) {
        val bytes = record.encode()
        require(bytes.size <= VaultRecord.MAX_METADATA)
        val staged = File(root, "${UUID.randomUUID()}.meta-part")
        try {
            val digest = Crypto.write(staged, key, bytes.size.toLong(), bytes.inputStream(), checkActive = active)
            try { Crypto.verify(staged, key, digest, checkActive = active) } finally { digest.fill(0) }
            active()
            // 元数据为唯一提交标记；原子替换后旧版本仍由文件系统保证完整性。
            java.nio.file.Files.move(staged.toPath(), File(root, "${record.book.id}.vmeta").toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            syncDirectory()
        } finally { bytes.fill(0) }
    }

    private fun publish(record: VaultRecord, active: () -> Unit) = synchronized(guard) {
        active()
        mutableRecords.value = mutableRecords.value + (record.book.id to record)
    }

    fun openReader(id: String): Crypto.Reader = session { key, active ->
        val record = records.value[id] ?: error("请先解锁文件保险库")
        val reader = Crypto.Reader(File(root, record.dataName), key, checkActive = active)
        try {
            require(reader.objectId.toString() + ".avd" == record.dataName) { "密文引用校验失败" }
            synchronized(guard) { active(); readers.add(reader) }
            reader
        } catch (failure: Throwable) { reader.close(); throw failure }
    }

    fun release(reader: Crypto.Reader) { synchronized(guard) { readers.remove(reader) }; reader.close() }

    fun export(id: String, password: CharArray, destination: OutputStream, active: () -> Unit = {}) {
        val record = records.value[id] ?: error("请先解锁文件保险库")
        val metadata = record.encode()
        val exportKey = Crypto.randomKey()
        val objectFile = File(root, "${UUID.randomUUID()}.export-part")
        try {
            val header = VaultPassword.wrap(exportKey, password)
            openReader(id).let { reader ->
                try {
                    val length = 4L + metadata.size + reader.length
                    val prefix = java.nio.ByteBuffer.allocate(4).putInt(metadata.size).array() + metadata
                    SequenceInputStream(prefix.inputStream(), reader.stream()).use { stream ->
                        val digest = Crypto.write(objectFile, exportKey, length, stream, checkActive = active)
                        try { Crypto.verify(objectFile, exportKey, digest, checkActive = active) } finally { digest.fill(0) }
                    }
                } finally { release(reader) }
            }
            active()
            destination.write(header)
            objectFile.inputStream().use { input -> copyChecked(input, destination, active) }
            destination.flush()
        } finally { metadata.fill(0); exportKey.fill(0); objectFile.delete() }
    }

    fun restore(source: InputStream, password: CharArray, active: () -> Unit = {}): VaultRecord {
        reserveAttempt()
        val staging = File(root, "${UUID.randomUUID()}.import-part")
        try {
            FileOutputStream(staging).use { output -> copyChecked(source, output, active); output.fd.sync() }
            val header = ByteArray(VaultPassword.HEADER_SIZE)
            staging.inputStream().use { DataInputStream(it).readFully(header) }
            val exportKey = VaultPassword.unwrap(header, password)
            try {
                Crypto.verify(staging, exportKey, offset = header.size.toLong(), checkActive = active)
                Crypto.Reader(staging, exportKey, header.size.toLong(), active).use { reader ->
                    val size = java.nio.ByteBuffer.wrap(reader.read(0, 4)).int
                    require(size in 1..VaultRecord.MAX_METADATA && 4L + size <= reader.length)
                    val bytes = reader.stream(4, size.toLong()).use { it.readBytes() }
                    val imported = try { VaultRecord.decode(bytes) } finally { bytes.fill(0) }
                    val id = UUID.randomUUID().toString()
                    val record = imported.copy(book = imported.book.copy(id = id), reader = imported.reader.copy(bookId = id), originalUri = "",
                        chapters = imported.chapters.map { it.copy(id = "$id:${it.chapterNumber}", bookId = id) },
                        annotations = imported.annotations.map { it.copy(bookId = id, id = nextAnnotationId()) })
                    val length = reader.length - 4 - size
                    require(record.pageSizes.isEmpty() || record.pageSizes.sum() == length)
                    val saved = create(record, length, { reader.stream(4L + size, length) }, active)
                    resetAttempts()
                    return saved
                }
            } finally { exportKey.fill(0) }
        } finally { staging.delete() }
    }

    private val noteSequence = java.util.concurrent.atomic.AtomicInteger(-1)
    fun nextAnnotationId(): Int {
        val occupied = records.value.values.flatMap { it.annotations }.map { it.id }.toSet()
        var next: Int
        do { next = noteSequence.getAndDecrement() } while (next in occupied || next >= 0)
        return next
    }

    private fun <T> session(block: (ByteArray, () -> Unit) -> T): T {
        val token = generation.get()
        val key = synchronized(guard) { master?.copyOf() ?: error("请先解锁文件保险库") }
        val active = { check(generation.get() == token && unlocked) { "保险库会话已撤销" } }
        return try { active(); block(key, active) } finally { key.fill(0) }
    }

    private val attempts = AtomicFile(File(root, "attempts.json"))
    @Synchronized private fun reserveAttempt() {
        val now = System.currentTimeMillis()
        val previous = if (attempts.baseFile.exists()) JSONObject(attempts.openRead().use {
            require(it.channel.size() <= 4096); it.readBytes().toString(Charsets.UTF_8)
        }) else JSONObject()
        val until = previous.optLong("until", 0)
        val elapsed = SystemClock.elapsedRealtime()
        val sameBoot = previous.optInt("boot", -1) == bootCount()
        val remaining = maxOf(until - now, if (sameBoot) previous.optLong("elapsedUntil", 0) - elapsed else 0)
        if (remaining > 0) throw VaultCooldownException(((remaining + 999) / 1000).coerceAtMost(3600))
        val count = (previous.optInt("count", 0) + 1).coerceAtMost(30)
        val delay = if (count < 5) 0 else minOf(3600000L, 30000L shl (count - 5).coerceAtMost(7))
        atomic(attempts, JSONObject().put("count", count).put("until", now + delay)
            .put("elapsedUntil", elapsed + delay).put("boot", bootCount()).toString().toByteArray())
    }
    @Synchronized private fun resetAttempts() = atomic(attempts, "{}".toByteArray())
    private val appContext = context.applicationContext
    private fun bootCount() = android.provider.Settings.Global.getInt(appContext.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1)

    private fun copyChecked(input: InputStream, output: OutputStream, active: () -> Unit) {
        val buffer = ByteArray(65536)
        var total = 0L
        try {
            while (true) {
                active()
                val count = input.read(buffer)
                if (count < 0) break
                require(count > 0)
                total += count
                require(total <= Crypto.MAX_LENGTH + 16 * 1024 * 1024) { "密文文件过大" }
                output.write(buffer, 0, count)
            }
        } finally { buffer.fill(0) }
    }

    private fun syncDirectory() {
        directorySync(root)
    }

    private fun atomic(file: AtomicFile, bytes: ByteArray) {
        val staging = File(root, "${UUID.randomUUID()}.atomic-part")
        FileOutputStream(staging).use { output -> output.write(bytes); output.fd.sync() }
        java.nio.file.Files.move(staging.toPath(), file.baseFile.toPath(),
            java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        syncDirectory()
    }

    companion object {
        @Volatile private var instance: FileVaultStore? = null
        fun get(context: Context): FileVaultStore = instance ?: synchronized(this) {
            instance ?: FileVaultStore(context.applicationContext, AndroidVaultDeviceKey(), ::syncNative).also { instance = it }
        }
        fun forTest(context: Context, key: VaultDeviceKey, directorySync: (File) -> Unit = ::syncNative) = FileVaultStore(context, key, directorySync)
        private fun syncNative(root: File) {
            val fd = android.system.Os.open(root.absolutePath, android.system.OsConstants.O_RDONLY, 0)
            try { android.system.Os.fsync(fd) } finally { android.system.Os.close(fd) }
        }
    }
}

internal class VaultCooldownException(val seconds: Long) : IllegalStateException("尝试过多，请 $seconds 秒后重试")
