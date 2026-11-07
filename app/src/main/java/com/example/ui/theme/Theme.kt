package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

val AsterShapes = com.asterreader.core.designsystem.AsterShapes

@Composable
fun AsterReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = com.asterreader.core.designsystem.AsterReaderTheme(darkTheme, content)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = AsterReaderTheme(darkTheme, content)
