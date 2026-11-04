package com.asterreader.core.engine

import com.asterreader.core.model.Locator
import com.asterreader.core.model.PublicationFormat

data class PublicationInput(
    val uri: String,
    val formatHint: PublicationFormat? = null,
    val extraHeaders: Map<String, String> = emptyMap(),
)

data class ParsedPublication(
    val id: String,
    val title: String,
    val author: String,
    val format: PublicationFormat,
    val totalPagesOrChapters: Int,
    val tableOfContents: List<TocItem> = emptyList(),
)

data class TocItem(
    val title: String,
    val locator: Locator,
    val children: List<TocItem> = emptyList(),
)

data class ProbeResult(
    val confidence: Float,
    val format: PublicationFormat,
    val capabilities: Set<EngineCapability>,
)

enum class EngineCapability {
    PAGED, SCROLL, ANNOTATION,
    RTL, DOUBLE_PAGE, ZOOM, TABLE_OF_CONTENTS
}

interface PublicationEngine {
    val id: String
    suspend fun probe(input: PublicationInput): ProbeResult
    suspend fun parse(input: PublicationInput): ParsedPublication
    suspend fun open(input: PublicationInput, locator: Locator? = null): ReaderSession
}
