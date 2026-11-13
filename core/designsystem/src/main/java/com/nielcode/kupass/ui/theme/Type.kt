package com.nielcode.kupass.ui.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.nielcode.kupass.core.designsystem.R

/**
 * One weight of a variable font, from a font-family XML that sets 'wght' when Android 9+ builds the
 * typeface. Compose's own variationSettings go through Paint, and MIUI 14 then draws its system
 * font instead; Android 8.1 ignores the XML attribute, so only there Compose sets the weight.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font =
    Font(
        resId,
        weight,
        variationSettings =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) FontVariation.Settings()
            else FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

val googleSansFlexFontFamily: FontFamily =
    FontFamily(
        variableFont(R.font.googlesansflex_400, FontWeight.Normal),
        variableFont(R.font.googlesansflex_700, FontWeight.Bold),
    )

val hemingFontFamily: FontFamily =
    FontFamily(
        variableFont(R.font.heming_400, FontWeight.Normal),
        variableFont(R.font.heming_700, FontWeight.Bold),
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
