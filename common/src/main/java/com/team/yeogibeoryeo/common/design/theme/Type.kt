package com.team.yeogibeoryeo.common.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.common.R

val PretendardFontFamily = FontFamily(
    Font(R.font.pretendard_thin, FontWeight.Thin),
    Font(R.font.pretendard_extralight, FontWeight.ExtraLight),
    Font(R.font.pretendard_light, FontWeight.Light),
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    Font(R.font.pretendard_black, FontWeight.Black),
)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = PretendardFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = PretendardFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = PretendardFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = PretendardFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = PretendardFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = PretendardFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = PretendardFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = PretendardFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = PretendardFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = PretendardFontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = PretendardFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = PretendardFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = PretendardFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = PretendardFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = PretendardFontFamily),
)
