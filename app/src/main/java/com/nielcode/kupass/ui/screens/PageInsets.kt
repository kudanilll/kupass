package com.nielcode.kupass.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Insets for the Scaffold of a page inside the home pager: top and sides only. The bottom is owned
 * by the pager, which passes a content padding that clears the floating bottom navigation and the
 * system navigation bar.
 */
@Composable
internal fun pagerPageInsets(): WindowInsets =
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
