package reader.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Locator(
    val publicationId: String,
    val resourceId: String? = null,
    val progression: Double? = null,
    val position: Long? = null,
    val imagePageId: String? = null,
    val updatedAtEpochMs: Long,
)
