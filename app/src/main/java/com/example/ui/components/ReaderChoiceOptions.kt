package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.asterreader.core.designsystem.AsterChoice
import com.example.ui.ReaderLayoutMode

fun readerModeIcon(mode: ReaderLayoutMode): ImageVector = when (mode) {
    ReaderLayoutMode.VERTICAL_SCROLL -> Icons.Outlined.ViewDay
    ReaderLayoutMode.RTL_PAGER -> Icons.Outlined.KeyboardArrowLeft
    ReaderLayoutMode.LTR_PAGER -> Icons.Outlined.KeyboardArrowRight
    ReaderLayoutMode.DUAL_PAGE -> Icons.Outlined.ViewColumn
}

/** 选项标签统一与「阅读设置 → 翻页」的当前值一致，避免同一状态两种叫法。 */
val readerFitChoices get() = listOf(AsterChoice("适合屏幕", Icons.Outlined.FitScreen),
    AsterChoice("原始比例", Icons.Outlined.AspectRatio))
val readerModeChoices get() = ReaderLayoutMode.entries.map {
    AsterChoice(readerModeLabel(it), readerModeIcon(it), it.label)
}

fun readerModeLabel(mode: ReaderLayoutMode): String = when (mode) {
    ReaderLayoutMode.VERTICAL_SCROLL -> "上下滚动"
    ReaderLayoutMode.RTL_PAGER -> "从右向左"
    ReaderLayoutMode.LTR_PAGER -> "从左向右"
    ReaderLayoutMode.DUAL_PAGE -> "双页"
}
