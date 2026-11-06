plugins {
    alias(libs.plugins.kupass.android.library)
    alias(libs.plugins.kupass.android.compose)
}

android { namespace = "com.nielcode.kupass.core.designsystem" }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
}
