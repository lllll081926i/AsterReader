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

val readerFitChoices get() = listOf(AsterChoice("适合屏幕", Icons.Outlined.FitScreen),
    AsterChoice("原始比例", Icons.Outlined.AspectRatio))
val readerModeChoices get() = ReaderLayoutMode.entries.map { AsterChoice(it.label, readerModeIcon(it)) }
