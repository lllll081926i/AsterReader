package com.example.ui.components

/** 上下各自最多十六页；优先解码移动方向，相同距离从近到远。 */
internal fun readerPrefetchPages(page: Int, total: Int, direction: Int,
    before: Int = 3, after: Int = 6): List<Int> {
    val preceding = (1..before.coerceIn(0, 16)).map { page - it }
    val following = (1..after.coerceIn(0, 16)).map { page + it }
    return (if (direction >= 0) following + preceding else preceding + following).filter { it in 1..total }
}
