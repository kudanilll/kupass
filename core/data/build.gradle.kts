plugins {
    alias(libs.plugins.kupass.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android { namespace = "com.nielcode.kupass.core.data" }

room {
    // Exported schemas are committed and used by MigrationTestHelper (added to androidTest assets).
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:security"))
    api(libs.androidx.room.runtime)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
}
