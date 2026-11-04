package com.asterreader.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// Aster System Canonical Palette (from Image_21_2e04b0d2.markdown & HTML specs)
// Botanical Sage Palette (Material 3 Expressive)
// ============================================================================

// Light Tokens
val AsterPrimaryLight = Color(0xFF2B4D40)
val AsterOnPrimaryLight = Color(0xFFFFFFFF)
val AsterPrimaryContainerLight = Color(0xFF436557)
val AsterOnPrimaryContainerLight = Color(0xFFBBE1CF)
val AsterInversePrimaryLight = Color(0xFFAACFBD)

val AsterSecondaryLight = Color(0xFF506359)
val AsterOnSecondaryLight = Color(0xFFFFFFFF)
val AsterSecondaryContainerLight = Color(0xFFD2E8DB)
val AsterOnSecondaryContainerLight = Color(0xFF384B41)

val AsterTertiaryLight = Color(0xFF244B58)
val AsterOnTertiaryLight = Color(0xFFFFFFFF)
val AsterTertiaryContainerLight = Color(0xFF3D6370)
val AsterOnTertiaryContainerLight = Color(0xFFB6DEED)

val AsterErrorLight = Color(0xFFBA1A1A)
val AsterOnErrorLight = Color(0xFFFFFFFF)
val AsterErrorContainerLight = Color(0xFFFFDAD6)
val AsterOnErrorContainerLight = Color(0xFF93000A)

val AsterSurfaceLight = Color(0xFFF3FBF5)
val AsterSurfaceDimLight = Color(0xFFD4DCD6)
val AsterSurfaceBrightLight = Color(0xFFF3FBF5)
val AsterSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val AsterSurfaceContainerLowLight = Color(0xFFEEF6EF)
val AsterSurfaceContainerLight = Color(0xFFE8F0EA)
val AsterSurfaceContainerHighLight = Color(0xFFE2EAE4)
val AsterSurfaceContainerHighestLight = Color(0xFFDCE4DE)
val AsterOnSurfaceLight = Color(0xFF161D1A)
val AsterOnSurfaceVariantLight = Color(0xFF414844)
val AsterOutlineLight = Color(0xFF717974)
val AsterOutlineVariantLight = Color(0xFFC1C8C3)
val AsterInverseSurfaceLight = Color(0xFF2A322E)
val AsterInverseOnSurfaceLight = Color(0xFFEBF3EC)

// Dark Tokens (Botanical Sage Dark Mode)
val AsterPrimaryDark = Color(0xFFAACFBD)
val AsterOnPrimaryDark = Color(0xFF003828)
val AsterPrimaryContainerDark = Color(0xFF2B4D40)
val AsterOnPrimaryContainerDark = Color(0xFFC5EBD9)
val AsterInversePrimaryDark = Color(0xFF2B4D40)

val AsterSecondaryDark = Color(0xFFB7CBBF)
val AsterOnSecondaryDark = Color(0xFF22352C)
val AsterSecondaryContainerDark = Color(0xFF384B41)
val AsterOnSecondaryContainerDark = Color(0xFFD2E8DB)

val AsterTertiaryDark = Color(0xFFA5CDDC)
val AsterOnTertiaryDark = Color(0xFF073541)
val AsterTertiaryContainerDark = Color(0xFF244B58)
val AsterOnTertiaryContainerDark = Color(0xFFC1E9F8)

val AsterErrorDark = Color(0xFFFFB4AB)
val AsterOnErrorDark = Color(0xFF690005)
val AsterErrorContainerDark = Color(0xFF93000A)
val AsterOnErrorContainerDark = Color(0xFFFFDAD6)

val AsterSurfaceDark = Color(0xFF101512)
val AsterSurfaceDimDark = Color(0xFF101512)
val AsterSurfaceBrightDark = Color(0xFF353B37)
val AsterSurfaceContainerLowestDark = Color(0xFF0B0F0D)
val AsterSurfaceContainerLowDark = Color(0xFF181D1A)
val AsterSurfaceContainerDark = Color(0xFF1C211E)
val AsterSurfaceContainerHighDark = Color(0xFF262B28)
val AsterSurfaceContainerHighestDark = Color(0xFF313633)
val AsterOnSurfaceDark = Color(0xFFE0E4DF)
val AsterOnSurfaceVariantDark = Color(0xFFC1C8C3)
val AsterOutlineDark = Color(0xFF8B938E)
val AsterOutlineVariantDark = Color(0xFF414844)
val AsterInverseSurfaceDark = Color(0xFFE0E4DF)
val AsterInverseOnSurfaceDark = Color(0xFF2A322E)

// Fixed & Special Semantic Tokens
val AsterPrimaryFixed = Color(0xFFC5EBD9)
val AsterPrimaryFixedDim = Color(0xFFAACFBD)
val AsterOnPrimaryFixed = Color(0xFF002116)
val AsterVaultSecureTint = Color(0xFF284D3D)

// Decoupled Reader Color Space (Never shifts with system Monet wallpaper)
val PaperLight = Color(0xFFFBF9F5)
val PaperWarm = Color(0xFFF3EAD8)
val PaperDarkGray = Color(0xFF1F2021)
val PaperOledBlack = Color(0xFF000000)

val InkLight = Color(0xFF191C1B)
val InkWarm = Color(0xFF2D261C)
val InkDark = Color(0xFFE1E3DF)

// Highlight Annotation Tint Palettes
val HighlightYellow = Color(0xFFFFF176)
val HighlightGreen = Color(0xFFA5D6A7)
val HighlightBlue = Color(0xFF90CAF9)
val HighlightPink = Color(0xFFF48FB1)

@Immutable
data class AsterExtendedColors(
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val vaultSecureTint: Color,
    val isDark: Boolean
)

val LocalAsterColors = staticCompositionLocalOf {
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
