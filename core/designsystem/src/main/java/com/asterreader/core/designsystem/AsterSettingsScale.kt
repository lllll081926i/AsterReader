package com.asterreader.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val LocalSettingsScaled = staticCompositionLocalOf { false }

/** 设置整体放大，参与真实测量与换行；保留系统字体缩放，嵌套表单不重复放大。 */
@Composable
fun AsterSettingsScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    if (LocalSettingsScaled.current) content()
    else CompositionLocalProvider(
        LocalSettingsScaled provides true,
        LocalDensity provides Density(density.density * 1.5f, density.fontScale),
        content = content
    )
}
