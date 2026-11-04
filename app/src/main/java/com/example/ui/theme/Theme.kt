package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

val AsterShapes = com.asterreader.core.designsystem.AsterShapes

@Composable
fun AsterReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = com.asterreader.core.designsystem.AsterReaderTheme(darkTheme, dynamicColor, content)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = AsterReaderTheme(darkTheme, dynamicColor, content)
