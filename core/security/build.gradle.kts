plugins { alias(libs.plugins.kupass.android.library) }

android { namespace = "com.nielcode.kupass.core.security" }

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
