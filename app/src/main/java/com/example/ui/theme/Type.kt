package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Bundled local Google Fonts for Reader Engine isolation & Display accents
val LoraFontFamily = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.Medium),
    Font(R.font.lora, FontWeight.Bold)
)

val RobotoMonoFontFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono, FontWeight.Medium)
)

val SystemRobotoFlexFamily = FontFamily.Default

// Aster System Typography Scale (UIUX-36 & Image_21_2e04b0d2.markdown)
val AsterTypography = Typography(
    // headline-lg: 32sp / 40sp
    headlineLarge = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    // headline-lg-mobile: 28sp / 36sp
    headlineMedium = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    // headline-sm: 24sp / 32sp
    headlineSmall = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // title-lg: 22sp / 28sp
    titleLarge = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // title-md: 16sp / 24sp / 500 / 0.15sp
    titleMedium = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // title-sm: 14sp / 20sp / 500 / 0.1sp
    titleSmall = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // body-lg: 16sp / 24sp / 400 / 0.5sp
    bodyLarge = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // body-md: 14sp / 20sp / 400 / 0.25sp
    bodyMedium = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // body-sm: 12sp / 16sp / 400 / 0.4sp
    bodySmall = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // label-lg: 14sp / 20sp / 500 / 0.1sp
    labelLarge = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // label-md: 12sp / 16sp / 500 / 0.5sp
    labelMedium = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // label-sm: 11sp / 16sp / 500 / 0.5sp
    labelSmall = TextStyle(
        fontFamily = SystemRobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
