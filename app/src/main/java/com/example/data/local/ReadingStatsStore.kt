package com.example.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 阅读时长统计：按自然日累计分钟数，保留最近 60 天。
 * 只记录「在阅读器里停留的时间」，不做任何内容或行为采集。
 */
class ReadingStatsStore(context: Context) {
    private val preferences = context.getSharedPreferences("reading_stats", Context.MODE_PRIVATE)

    data class Summary(
        val todayMinutes: Int = 0,
        val weekMinutes: Int = 0,
        val totalMinutes: Int = 0,
        /** 最近 7 天，从早到晚。 */
        val daily: List<DailyMinutes> = emptyList()
    ) {
        val totalHoursText: String get() = formatDuration(totalMinutes)
        val weekText: String get() = formatDuration(weekMinutes)
        val todayText: String get() = formatDuration(todayMinutes)
    }

    data class DailyMinutes(val day: LocalDate, val minutes: Int)

    suspend fun record(minutes: Double) = withContext(Dispatchers.IO) {
        if (minutes <= 0.0 || minutes > 24 * 60) return@withContext
        val today = LocalDate.now()
        val key = today.format(STORAGE_DATE)
        val current = preferences.getFloat(key, 0f)
        // 分钟按整分累加，避免每次进出都写小数
        val updated = current + minutes.toFloat()
        check(preferences.edit().putFloat(key, updated).commit()) { "无法保存阅读统计" }
        prune(today)
    }

    suspend fun read(): Summary = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        var total = 0.0
        var week = 0.0
        var todayMinutes = 0.0
        val perDay = HashMap<LocalDate, Int>()
        val stored = preferences.all
        for ((key, value) in stored) {
            if (value !is Float) continue
            val date = runCatching { LocalDate.parse(key, STORAGE_DATE) }.getOrNull() ?: continue
            total += value
            if (!date.isBefore(weekStart)) week += value
            if (date == today) todayMinutes += value
            if (!date.isBefore(weekStart)) perDay[date] = value.toInt()
        }
        Summary(
            todayMinutes = todayMinutes.toInt(),
            weekMinutes = week.toInt(),
            totalMinutes = total.toInt(),
            daily = (0..6).map { offset ->
                val day = weekStart.plusDays(offset.toLong())
                DailyMinutes(day, perDay[day] ?: 0)
            }
        )
    }

    private fun prune(today: LocalDate) {
        val cutoff = today.minusDays(60)
        val editor = preferences.edit()
        var changed = false
        for (key in preferences.all.keys) {
            val date = runCatching { LocalDate.parse(key, STORAGE_DATE) }.getOrNull() ?: continue
            if (date.isBefore(cutoff)) {
                editor.remove(key)
                changed = true
            }
        }
        if (changed) editor.apply()
    }

    private companion object {
        val STORAGE_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun formatDuration(minutes: Int): String = when {
            minutes < 60 -> "$minutes 分钟"
            else -> "${minutes / 60} 小时 ${minutes % 60} 分"
        }
    }
}
