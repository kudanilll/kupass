plugins {
    alias(libs.plugins.kupass.android.library)
    alias(libs.plugins.kupass.android.compose)
}

android { namespace = "com.nielcode.kupass.feature.autofill" }

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:security"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
}
