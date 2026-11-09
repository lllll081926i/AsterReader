package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LibraryRepository
import com.example.data.remote.DavConnection
import com.example.data.remote.DavEntry
import com.example.data.remote.WebDavRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WebDavUiState(val connections: List<DavConnection> = emptyList(), val connection: DavConnection? = null,
    val path: String = "", val entries: List<DavEntry> = emptyList(), val cachedPaths: Set<String> = emptySet(),
    val busy: Boolean = false, val error: String? = null, val message: String? = null, val progress: String? = null,
    val editing: Boolean = false, val editConnection: DavConnection? = null)

class WebDavViewModel(application: Application) : AndroidViewModel(application) {
    private val remote = WebDavRepository.get(application)
    private val library = LibraryRepository(application)
    private val _state = MutableStateFlow(WebDavUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    init { refreshConnections() }

    private fun operation(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, message = null, progress = null) }
        job = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { _state.update { it.copy(error = safeMessage(failure)) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    fun refreshConnections() = operation { _state.update { it.copy(connections = remote.connections()) } }
    fun edit(connection: DavConnection? = null) {
        if (_state.value.busy) return
        _state.update { it.copy(editing = true, editConnection = connection, error = null, message = null) }
    }

    fun save(name: String, address: String, username: String, password: CharArray, allowHttp: Boolean, persist: Boolean, keepPassword: Boolean = false) {
        if (_state.value.busy) { password.fill('\u0000'); return }
        val id = _state.value.editConnection?.id
        operation {
            try {
                remote.save(id, name, address, username, password, allowHttp, persist, keepPassword)
                if (persist && id != null) library.invalidateRemoteReadCopies(id)
                if (persist) _state.update { it.copy(connections = remote.connections(), connection = null, editing = false, editConnection = null,
                    path = "", entries = emptyList(), cachedPaths = emptySet(), message = "连接已保存") }
                else _state.update { it.copy(message = "连接成功") }
            } finally { password.fill('\u0000') }
        }
    }

    fun browse(connection: DavConnection, path: String = "") {
        if (_state.value.busy) return
        _state.update { it.copy(connection = connection, path = path, entries = emptyList(), cachedPaths = emptySet()) }
        operation {
            val items = remote.list(connection.id, path)
            val cached = items.filter { !it.directory && remote.cached(connection.id, it) }.mapTo(mutableSetOf()) { it.path }
            _state.update { it.copy(connection = connection, path = path, entries = items, cachedPaths = cached) }
        }
    }

    fun retry() { val state = _state.value; state.connection?.let { browse(it, state.path) } ?: refreshConnections() }

    fun importFile(entry: DavEntry, onOpen: (String) -> Unit = {}) {
        val connection = _state.value.connection ?: return
        operation {
            val book = library.importDocument(WebDavRepository.documentUri(connection.id, entry.path), connection.name, persist = false)
            val cached = remote.cached(connection.id, entry)
            _state.update { it.copy(message = if (book.isVaultProtected || book.isVaultEncrypted) "已在隐藏作品中，验证后可查看"
                else "已导入《${book.title}》", cachedPaths = if (cached) it.cachedPaths + entry.path else it.cachedPaths) }
            if (!book.isVaultProtected && !book.isVaultEncrypted) onOpen(book.id)
        }
    }

    fun scan() {
        val state = _state.value
        val connection = state.connection ?: return
        operation {
            _state.update { it.copy(progress = "正在扫描") }
            val folderName = if (state.path.isEmpty()) connection.name else Uri.decode(state.path.trimEnd('/').substringAfterLast('/'))
            val report = library.scanRepository(WebDavRepository.treeUri(connection.id, state.path), folderName, LibraryRepository.DEFAULT_SCAN_RULE) { processed, discovered ->
                _state.update { it.copy(progress = "已处理 $processed / $discovered") }
            }
            val cached = state.entries.filter { !it.directory && remote.cached(connection.id, it) }.mapTo(mutableSetOf()) { it.path }
            _state.update { it.copy(message = "已导入：${report.summary()}", cachedPaths = cached) }
        }
    }

    fun remove(connection: DavConnection) = operation {
        remote.remove(connection.id)
        library.invalidateRemoteReadCopies(connection.id)
        _state.update { it.copy(connections = remote.connections(), connection = null, entries = emptyList(), path = "", message = "连接已移除，云端文件与阅读记录保留") }
    }

    fun back(): Boolean {
        val state = _state.value
        if (state.busy) { cancel(); return true }
        if (state.editing) { _state.update { it.copy(editing = false, editConnection = null, error = null, message = null) }; return true }
        val connection = state.connection ?: return false
        if (state.path.isNotEmpty()) {
            val parent = state.path.trimEnd('/').substringBeforeLast('/', "").let { if (it.isBlank()) "" else "$it/" }
            browse(connection, parent)
        } else _state.update { it.copy(connection = null, entries = emptyList(), cachedPaths = emptySet(), error = null, message = null) }
        return true
    }

    fun cancel() { job?.cancel() }

    private fun safeMessage(failure: Exception): String = when (failure) {
        is java.io.IOException -> "网络读取失败，请检查网络与地址"
        is java.security.GeneralSecurityException -> "凭据无法解密，原配置未改动"
        else -> failure.message?.takeIf { !it.contains("://") && it.length <= 180 } ?: "读取失败，请刷新后重试"
    }
}
