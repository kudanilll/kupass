plugins {
    alias(libs.plugins.kupass.android.library)
    alias(libs.plugins.kupass.android.compose)
}

android { namespace = "com.nielcode.kupass.core.designsystem" }

dependencies {
    // api so modules using the theme get Compose versions without declaring the BOM again.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
}
