package com.asterreader.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AsterHeaderTitleStyle = TextStyle(fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)

/** 标题栏仅压缩布局占位，Compose 仍在输入层扩展到系统最小触控范围。 */
@Composable
fun AsterHeaderRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(modifier.heightIn(min = AsterLayout.HeaderHeight), horizontalArrangement, verticalAlignment, content)
    }
}

fun Modifier.asterHeaderAction(): Modifier = size(width = AsterLayout.TouchTarget, height = AsterLayout.HeaderHeight)
