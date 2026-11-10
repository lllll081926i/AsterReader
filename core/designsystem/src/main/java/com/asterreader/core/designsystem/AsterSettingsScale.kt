package com.asterreader.core.designsystem

import androidx.compose.runtime.Composable
/** 设置沿用系统密度与字体缩放；保留现有页面的公共入口。 */
@Composable
fun AsterSettingsScale(content: @Composable () -> Unit) {
    content()
}
