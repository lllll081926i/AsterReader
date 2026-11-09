package com.example.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 作品导入时间只保存时间戳，显示文案在渲染时计算，
 * 避免「刚刚导入」在数据库里永久不变。
 */
fun relativeImportText(addedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (addedAtMillis <= 0L || addedAtMillis > nowMillis) return ""
    val zone = ZoneId.systemDefault()
    val added = Instant.ofEpochMilli(addedAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(added, today)
    return when {
        days <= 0L -> "今天导入"
        days == 1L -> "昨天导入"
        days < 30L -> "$days 天前导入"
        else -> "${added.monthValue} 月 ${added.dayOfMonth} 日导入"
    }
}

/** 供测试直接构造日期，避免依赖真实时钟。 */
internal fun relativeImportText(added: LocalDate, today: LocalDate): String =
    relativeImportText(added.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

/** 导入时间文案：旧数据没有时间戳时回退到已存文字。 */
fun importTimeText(addedAtMillis: Long, stored: String): String =
    relativeImportText(addedAtMillis).ifBlank { stored }
