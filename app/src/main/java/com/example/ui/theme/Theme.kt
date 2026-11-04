package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val AsterLightColorScheme = lightColorScheme(
    primary = AsterPrimaryLight,
    onPrimary = AsterOnPrimaryLight,
    primaryContainer = AsterPrimaryContainerLight,
    onPrimaryContainer = AsterOnPrimaryContainerLight,
    inversePrimary = AsterInversePrimaryLight,
    secondary = AsterSecondaryLight,
    onSecondary = AsterOnSecondaryLight,
    secondaryContainer = AsterSecondaryContainerLight,
    onSecondaryContainer = AsterOnSecondaryContainerLight,
    tertiary = AsterTertiaryLight,
    onTertiary = AsterOnTertiaryLight,
    tertiaryContainer = AsterTertiaryContainerLight,
    onTertiaryContainer = AsterOnTertiaryContainerLight,
    error = AsterErrorLight,
    onError = AsterOnErrorLight,
    errorContainer = AsterErrorContainerLight,
    onErrorContainer = AsterOnErrorContainerLight,
    background = AsterSurfaceLight,
    onBackground = AsterOnSurfaceLight,
    surface = AsterSurfaceLight,
    onSurface = AsterOnSurfaceLight,
    surfaceVariant = AsterSurfaceContainerHighestLight,
    onSurfaceVariant = AsterOnSurfaceVariantLight,
    outline = AsterOutlineLight,
    outlineVariant = AsterOutlineVariantLight,
    inverseSurface = AsterInverseSurfaceLight,
    inverseOnSurface = AsterInverseOnSurfaceLight,
    surfaceTint = AsterPrimaryContainerLight
)

private val AsterDarkColorScheme = darkColorScheme(
    primary = AsterPrimaryDark,
    onPrimary = AsterOnPrimaryDark,
    primaryContainer = AsterPrimaryContainerDark,
    onPrimaryContainer = AsterOnPrimaryContainerDark,
    inversePrimary = AsterInversePrimaryDark,
    secondary = AsterSecondaryDark,
    onSecondary = AsterOnSecondaryDark,
    secondaryContainer = AsterSecondaryContainerDark,
    onSecondaryContainer = AsterOnSecondaryContainerDark,
    tertiary = AsterTertiaryDark,
    onTertiary = AsterOnTertiaryDark,
    tertiaryContainer = AsterTertiaryContainerDark,
    onTertiaryContainer = AsterOnTertiaryContainerDark,
    error = AsterErrorDark,
    onError = AsterOnErrorDark,
    errorContainer = AsterErrorContainerDark,
    onErrorContainer = AsterOnErrorContainerDark,
    background = AsterSurfaceDark,
    onBackground = AsterOnSurfaceDark,
    surface = AsterSurfaceDark,
    onSurface = AsterOnSurfaceDark,
    surfaceVariant = AsterSurfaceContainerHighestDark,
    onSurfaceVariant = AsterOnSurfaceVariantDark,
    outline = AsterOutlineDark,
    outlineVariant = AsterOutlineVariantDark,
    inverseSurface = AsterInverseSurfaceDark,
    inverseOnSurface = AsterInverseOnSurfaceDark,
    surfaceTint = AsterPrimaryDark
)

val AsterShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // Publication Art CoverArtwork fixed 12dp radius
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp) // Settings Groups AsterSettingsGroup 24dp radius
)

@Composable
fun AsterReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AsterDarkColorScheme
        else -> AsterLightColorScheme
    }

    val extendedColors = if (darkTheme) {
        AsterExtendedColors(
            surfaceContainerLowest = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) colorScheme.surface else AsterSurfaceContainerLowestDark,
            surfaceContainerLow = AsterSurfaceContainerLowDark,
            surfaceContainer = AsterSurfaceContainerDark,
            surfaceContainerHigh = AsterSurfaceContainerHighDark,
            surfaceContainerHighest = AsterSurfaceContainerHighestDark,
            surfaceDim = AsterSurfaceDimDark,
            surfaceBright = AsterSurfaceBrightDark,
            primaryFixed = AsterPrimaryFixed,
            primaryFixedDim = AsterPrimaryFixedDim,
            onPrimaryFixed = AsterOnPrimaryFixed,
            vaultSecureTint = AsterPrimaryDark,
            isDark = true
        )
    } else {
        AsterExtendedColors(
            surfaceContainerLowest = AsterSurfaceContainerLowestLight,
            surfaceContainerLow = AsterSurfaceContainerLowLight,
            surfaceContainer = AsterSurfaceContainerLight,
            surfaceContainerHigh = AsterSurfaceContainerHighLight,
            surfaceContainerHighest = AsterSurfaceContainerHighestLight,
            surfaceDim = AsterSurfaceDimLight,
            surfaceBright = AsterSurfaceBrightLight,
            primaryFixed = AsterPrimaryFixed,
            primaryFixedDim = AsterPrimaryFixedDim,
            onPrimaryFixed = AsterOnPrimaryFixed,
            vaultSecureTint = AsterVaultSecureTint,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalAsterColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AsterTypography,
            shapes = AsterShapes,
            content = content
        )
    }
}

// Keep compatibility alias for existing previews/tests
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AsterReaderTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
