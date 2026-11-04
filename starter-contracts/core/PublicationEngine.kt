package reader.core.engine

interface PublicationEngine {
    val id: String
    suspend fun probe(input: PublicationInput): ProbeResult
    suspend fun parse(input: PublicationInput): ParsedPublication
    suspend fun open(input: PublicationInput, locator: Locator? = null): ReaderSession
}

data class ProbeResult(
    val confidence: Float,
    val format: PublicationFormat,
    val capabilities: Set<EngineCapability>,
)

enum class EngineCapability {
    PAGED, SCROLL, SEARCH, TEXT_SELECTION, ANNOTATION, TTS,
    RTL, DOUBLE_PAGE, ZOOM, TABLE_OF_CONTENTS
}
