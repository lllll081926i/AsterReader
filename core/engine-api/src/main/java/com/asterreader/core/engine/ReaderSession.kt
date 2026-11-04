package com.asterreader.core.engine

import com.asterreader.core.model.Locator
import kotlinx.coroutines.flow.StateFlow

enum class ReaderCapability {
    NEXT_PREV,
    SEEK,
    RENDER_CANVAS,
    TEXT_SELECTION,
    REFLOW_STYLING
}

data class ReaderState(
    val locator: Locator,
    val progression: Double,
    val resourceTitle: String?,
    val isBusy: Boolean = false,
)

interface ReaderSession : AutoCloseable {
    val state: StateFlow<ReaderState>
    val capabilities: Set<ReaderCapability>
    suspend fun next()
    suspend fun previous()
    suspend fun seek(locator: Locator)
    suspend fun flushProgress()
    override fun close()
}
