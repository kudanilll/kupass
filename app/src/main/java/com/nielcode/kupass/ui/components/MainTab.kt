package com.nielcode.kupass.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.icons.KupassIcons

/** Top-level pages of the home pager, in page order. */
enum class MainTab(@StringRes val title: Int, val icon: ImageVector) {
    Home(R.string.nav_home, KupassIcons.Lock),
    Data(R.string.nav_data, KupassIcons.Storage),
    Settings(R.string.nav_settings, KupassIcons.Settings),
}
