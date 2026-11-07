package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.asterreader.core.database.*
import com.asterreader.core.vault.VaultCryptography
import com.example.data.local.vault.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TemporaryFolder
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FileVaultStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val password get() = "uN4-pC8_vE2-Ka7_zM9".toCharArray()
    private fun context(directory: File): Context = object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
        override fun getNoBackupFilesDir() = directory
        override fun getApplicationContext(): Context = this
    }
    private class Device : VaultDeviceKey {
        private val key = VaultCryptography.randomKey()
        override fun encrypt(bytes: ByteArray, create: Boolean) = VaultCryptography.seal(key, bytes, byteArrayOf(1))
        override fun decrypt(bytes: ByteArray) = VaultCryptography.unseal(key, bytes, byteArrayOf(1))
        override fun sourceTag(uri: String, create: Boolean) = "test-only-source-tag"
    }
    private fun record(): VaultRecord {
        val id = UUID.randomUUID().toString()
        val book = BookEntity(id, "私密标题-不得落盘", "私密作者", "私密系列", "CBZ", "", currentPage = 1, totalPages = 3,
            progressPercent = 0, currentChapterTitle = "私密章节", statusBadge = "未读", seriesBadge = "", fileSizeText = "1 KB",
            filePath = "content://private/original", repositoryName = "私密仓库", updatedTimeText = "", synopsis = "私密简介",
            tagsCsv = "私密标签", shelfIdsCsv = "")
        return VaultRecord(book, "", listOf(ChapterEntity("c1", id, 1, "私密章节", 1, 3, false, true)),
            listOf(AnnotationEntity(-1, id, "私密章节", 1, "私密引用", "私密笔记", "GREEN", "NOTE", "")), ReaderStateEntity(id))
    }

    @Test fun encryptedMetadataAndFileSurviveRestartAndRemainUnavailableWhileLocked() {
        val directory = temporary.newFolder()
        val device = Device()
        val store = FileVaultStore.forTest(context(directory), device) {}
        store.open(password, true)
        val record = record()
        val data = "这是漫画字节-不能留在磁盘".repeat(1000).toByteArray()
        val saved = store.create(record, data.size.toLong(), { data.inputStream() })
        val reader = store.openReader(saved.book.id)
        assertArrayEquals(data.copyOf(40), reader.read(0, 40))
        store.lock()
        assertTrue(store.records.value.isEmpty())
        assertThrows(Exception::class.java) { reader.read(0, 1) }
        assertThrows(Exception::class.java) { store.openReader(saved.book.id) }
        val reopened = FileVaultStore.forTest(context(directory), device) {}
        assertThrows(Exception::class.java) { reopened.open("incorrect-long-secret".toCharArray(), false) }
        reopened.open(password, false)
        assertEquals(record.book.title, reopened.records.value[saved.book.id]!!.book.title)
        reopened.openReader(saved.book.id).use { assertArrayEquals(data, it.stream().readBytes()) }
        directory.walkTopDown().filter(File::isFile).forEach { file ->
            val disk = file.readBytes().toString(Charsets.UTF_8)
            assertFalse(disk.contains(record.book.title)); assertFalse(disk.contains("这是漫画字节")); assertFalse(disk.contains("私密笔记"))
        }
    }

    @Test fun exportRestoresOnIndependentDeviceAndTamperNeverCommits() {
        val first = FileVaultStore.forTest(context(temporary.newFolder()), Device()) {}
        first.open(password, true)
        val data = ByteArray(1100000) { (it % 199).toByte() }
        val saved = first.create(record(), data.size.toLong(), { data.inputStream() })
        val output = ByteArrayOutputStream()
        val exportPassword = "zK8-yR5_Pm2-Ae7_Dt9".toCharArray()
        first.export(saved.book.id, exportPassword, output)
        first.lock()
        val second = FileVaultStore.forTest(context(temporary.newFolder()), Device()) {}
        second.open(password, true)
        val damaged = output.toByteArray().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertThrows(Exception::class.java) { second.restore(damaged.inputStream(), exportPassword) }
        assertTrue(second.committedIds().isEmpty())
        assertThrows(Exception::class.java) { second.restore(output.toByteArray().inputStream(), password) }
        assertTrue(second.committedIds().isEmpty())
        val restored = second.restore(output.toByteArray().inputStream(), exportPassword)
        assertNotEquals(saved.book.id, restored.book.id)
        second.openReader(restored.book.id).use { assertArrayEquals(data, it.stream().readBytes()) }
        assertEquals(saved.book.title, restored.book.title)
    }

    @Test fun passwordRotationRequiresCurrentPasswordAndPreservesData() {
        val directory = temporary.newFolder()
        val device = Device()
        val store = FileVaultStore.forTest(context(directory), device) {}
        store.open(password, true)
        val data = "更换口令不能损坏正文".toByteArray()
        val saved = store.create(record(), data.size.toLong(), { data.inputStream() })
        val next = "cR7-pX2_vT8-Mk5_aN9".toCharArray()
        assertThrows(Exception::class.java) { store.changePassword(next, next) }
        store.changePassword(password, next)
        store.lock()
        val reopened = FileVaultStore.forTest(context(directory), device) {}
        assertThrows(Exception::class.java) { reopened.open(password, false) }
        reopened.open(next, false)
        reopened.openReader(saved.book.id).use { assertArrayEquals(data, it.stream().readBytes()) }
        next.fill('\u0000')
    }

    @Test fun cancelledWritePreservesSourceAndDoesNotPublishMetadata() {
        val store = FileVaultStore.forTest(context(temporary.newFolder()), Device()) {}
        store.open(password, true)
        val source = temporary.newFile().apply { writeBytes(ByteArray(1000) { 7 }) }
        assertThrows(Exception::class.java) { store.create(record(), source.length(), { source.inputStream() }) { error("已取消") } }
        assertTrue(source.exists()); assertEquals(1000L, source.length()); assertTrue(store.committedIds().isEmpty())
    }

    @Test fun repeatedAttemptsPersistAcrossStoreRecreation() {
        val directory = temporary.newFolder()
        val device = Device()
        val store = FileVaultStore.forTest(context(directory), device) {}
        store.open(password, true); store.lock()
        repeat(5) {
            val failure = assertThrows(Exception::class.java) { store.open("bad-secret-value".toCharArray(), false) }
            assertTrue("错误口令应失败于认证：${failure.javaClass.name}: ${failure.message}", failure is javax.crypto.AEADBadTagException)
        }
        val attemptFile = File(directory, "file-vault-v2/attempts.json")
        assertEquals(attemptFile.readText(), 5, org.json.JSONObject(attemptFile.readText()).getInt("count"))
        val recreated = FileVaultStore.forTest(context(directory), device)
        assertThrows(Exception::class.java) { recreated.open(password, false) }
        assertFalse(recreated.unlocked)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun firstEncryptionContinuesAfterCreatingVaultInViewModel() = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher())
        val models = androidx.lifecycle.ViewModelStore()
        val store = FileVaultStore.forTest(context(temporary.newFolder()), Device()) {}
        try {
            val app = ApplicationProvider.getApplicationContext<android.app.Application>()
            app.getSharedPreferences("reader_settings", 0).edit().clear().commit()
            val model = com.example.ui.AsterViewModel(app)
            models.put("加密流程", model)
            kotlinx.coroutines.withTimeout(10000) { model.uiState.first { it.settingsLoaded } }
            val repository = com.example.ui.AsterViewModel::class.java.getDeclaredField("repository").let {
                it.isAccessible = true; it.get(model) as com.example.data.local.LibraryRepository
            }.apply { fileVault = store }
            val source = temporary.newFile("first.zip")
            java.util.zip.ZipOutputStream(source.outputStream()).use { zip ->
                repeat(3) { index -> zip.putNextEntry(java.util.zip.ZipEntry("${index + 1}.png")); zip.write(byteArrayOf(1, 2, 3)); zip.closeEntry() }
            }
            val book = record().book.copy(filePath = android.net.Uri.fromFile(source).toString(), sizeBytes = source.length(), sourceModifiedAt = source.lastModified())
            AsterDatabase.getDatabase(app).asterDao().insertBook(book)
            model.commitSearchHistory(book.title)
            model.beginEncryptBook(book.id)
            model.openFileVault(password, true)
            kotlinx.coroutines.withTimeout(30000) { model.uiState.first { !it.fileVaultBusy } }
            assertTrue(model.uiState.value.fileVaultError, model.uiState.value.fileVaultOpen)
            model.encryptPendingBook()
            kotlinx.coroutines.withTimeout(30000) { model.uiState.first { !it.fileVaultBusy } }
            assertNull(model.uiState.value.fileVaultError)
            assertTrue(repository.getBookById(book.id)!!.isVaultEncrypted)
            assertTrue(model.uiState.value.recentSearches.isEmpty())
            assertTrue(source.exists())
        } finally { models.clear(); store.lock(); kotlinx.coroutines.Dispatchers.resetMain() }
    }

    @Test fun repositoryMigrationRemovesPlainMetadataAndKeepsEncryptedReadingState() = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.withTimeout(30000) {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val database = androidx.room.Room.inMemoryDatabaseBuilder(app, AsterDatabase::class.java).allowMainThreadQueries().build()
        val store = FileVaultStore.forTest(context(temporary.newFolder()), Device()) {}
        val repository = com.example.data.local.LibraryRepository(app, database.asterDao()).apply { fileVault = store }
        try {
            store.open(password, true)
            val source = temporary.newFile("original.zip")
            java.util.zip.ZipOutputStream(source.outputStream()).use { zip ->
                repeat(3) { index -> zip.putNextEntry(java.util.zip.ZipEntry("${index + 1}.png")); zip.write("私密图片$index".toByteArray()); zip.closeEntry() }
            }
            val original = record()
            val book = original.book.copy(filePath = android.net.Uri.fromFile(source).toString(), sizeBytes = source.length(), sourceModifiedAt = source.lastModified())
            database.asterDao().insertBook(book)
            database.asterDao().insertAnnotation(original.annotations.first())
            repository.encryptBookFile(book.id)
            val shell = database.asterDao().getBookById(book.id)!!
            assertEquals("加密作品", shell.title); assertEquals("", shell.author); assertTrue(shell.isVaultEncrypted)
            assertTrue(database.asterDao().getChapters(book.id).isEmpty())
            assertTrue(database.asterDao().getAnnotationsForBook(book.id).isEmpty())
            assertTrue(source.exists())
            assertEquals(book.title, repository.getBookById(book.id)!!.title)
            repository.saveReadingPosition(book.id, 3, "完成章节", null)
            repository.saveReaderOptions(book.id, "RTL_PAGER", "原始比例", true, true)
            assertEquals("已读完", repository.getBookById(book.id)!!.statusBadge)
            assertEquals("RTL_PAGER", repository.readerState(book.id)!!.layoutMode)
            repository.lockFileVault()
            assertEquals("加密作品", repository.getBookById(book.id)!!.title)
            store.open(password, false)
            assertEquals(3, repository.getBookById(book.id)!!.currentPage)
            assertEquals("完成章节", repository.getBookById(book.id)!!.currentChapterTitle)
            assertEquals("私密笔记", store.records.value[book.id]!!.annotations.first().noteText)
            VaultComicArchive(store, store.records.value[book.id]!!).use { archive ->
                assertEquals("私密图片1", archive.openPage(2).use { it.readBytes().toString(Charsets.UTF_8) })
            }
        } finally { store.lock(); database.close() }
        }
    }
}
