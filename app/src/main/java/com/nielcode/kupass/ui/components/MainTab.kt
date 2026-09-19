package com.nielcode.kupass.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import com.nielcode.kupass.R

/** Top-level pages of the home pager, in page order. */
enum class MainTab(@StringRes val title: Int, val icon: ImageVector) {
    Home(R.string.nav_home, Icons.Default.Home),
    Data(R.string.nav_data, Icons.Default.Storage),
    Settings(R.string.nav_settings, Icons.Default.Settings),
}
