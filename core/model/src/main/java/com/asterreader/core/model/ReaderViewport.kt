package com.asterreader.core.model

/** 平移按画布宽高归一化，页内偏移按完整页面高度归一化。 */
data class ReaderViewport(
    val page: Int = 1,
    val offsetFraction: Float = 0f,
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    fun sanitized(totalPages: Int) = copy(
        page = page.coerceIn(1, totalPages.coerceAtLeast(1)),
        offsetFraction = offsetFraction.finite(0f).coerceIn(0f, 0.9999f),
        scale = scale.finite(1f).coerceIn(1f, 4f),
        panX = panX.finite(0f).coerceIn(-1.5f, 1.5f),
        panY = panY.finite(0f).coerceIn(-1.5f, 1.5f))
}
private fun Float.finite(fallback: Float) = takeIf { it.isFinite() } ?: fallback
