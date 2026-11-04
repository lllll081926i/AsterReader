package reader.core.reader

import kotlinx.coroutines.flow.StateFlow

interface ReaderSession : AutoCloseable {
    val state: StateFlow<ReaderState>
    val capabilities: Set<ReaderCapability>
    suspend fun next()
    suspend fun previous()
    suspend fun seek(locator: Locator)
    suspend fun flushProgress()
    override fun close()
}

data class ReaderState(
    val locator: Locator,
    val progression: Double,
    val resourceTitle: String?,
    val isBusy: Boolean = false,
)
