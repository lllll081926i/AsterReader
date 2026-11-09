package com.example

import com.example.ui.relativeImportText
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** 导入时间文案必须随日期变化，不能把「刚刚导入」永久写进数据库。 */
class RelativeImportTextTest {
    private val today = LocalDate.of(2026, 3, 15)

    @Test fun sameDayShowsToday() {
        assertEquals("今天导入", relativeImportText(today, today))
    }

    @Test fun previousDayShowsYesterday() {
        assertEquals("昨天导入", relativeImportText(today.minusDays(1), today))
    }

    @Test fun withinMonthShowsDayCount() {
        assertEquals("5 天前导入", relativeImportText(today.minusDays(5), today))
    }

    @Test fun olderShowsCalendarDate() {
        assertEquals("2 月 1 日导入", relativeImportText(LocalDate.of(2026, 2, 1), today))
    }

    @Test fun missingTimestampProducesEmptyText() {
        assertEquals("", relativeImportText(0L, System.currentTimeMillis()))
    }
}
