package com.nielcode.kupass.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.nielcode.kupass.R

@OptIn(ExperimentalTextApi::class)
val googleSansFlexFontFamily: FontFamily =
    FontFamily(
        Font(
            resId = R.font.googlesansflex,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            resId = R.font.googlesansflex,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

@OptIn(ExperimentalTextApi::class)
val hemingFontFamily: FontFamily =
    FontFamily(
        Font(
            resId = R.font.heming,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            resId = R.font.heming,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

// Default Material 3 typography values
val baseline = Typography()
val Typography =
    Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = hemingFontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = hemingFontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = hemingFontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = hemingFontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = hemingFontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = hemingFontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = hemingFontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = hemingFontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = hemingFontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = googleSansFlexFontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = googleSansFlexFontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = googleSansFlexFontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = googleSansFlexFontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = googleSansFlexFontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = googleSansFlexFontFamily),
    )
