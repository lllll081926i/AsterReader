package reader.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Locator(
    val publicationId: String,
    val resourceId: String? = null,
    val progression: Double? = null,
    val position: Long? = null,
    val textAnchor: TextAnchor? = null,
    val imagePageId: String? = null,
    val updatedAtEpochMs: Long,
)

@Serializable
data class TextAnchor(
    val selector: String? = null,
    val before: String? = null,
    val exact: String? = null,
    val after: String? = null,
)
