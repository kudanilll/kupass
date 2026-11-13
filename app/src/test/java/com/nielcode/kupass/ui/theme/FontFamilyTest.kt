package com.nielcode.kupass.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.ResourceFont
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTextApi::class)
@RunWith(RobolectricTestRunner::class)
class FontFamilyTest {
    @Test
    @Config(sdk = [35])
    fun `Android 9 and later take each weight from its font-family XML`() {
        // Compose applies variationSettings through Paint, which drew the system font on MIUI 14
        // (Redmi Note 10 Pro, Android 13).
        assertEachFont { FontVariation.Settings() }
    }

    @Test
    @Config(sdk = [27])
    fun `Android 8_1 ignores the XML weight, so Compose sets it`() {
        assertEachFont { font -> FontVariation.Settings(FontVariation.weight(font.weight.weight)) }
    }

    private fun assertEachFont(expected: (ResourceFont) -> FontVariation.Settings) {
        for (family in listOf(googleSansFlexFontFamily, hemingFontFamily)) {
            for (font in (family as FontListFontFamily).fonts) {
                font as ResourceFont
                assertEquals(expected(font), font.variationSettings)
            }
        }
    }
}
