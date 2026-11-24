package com.nielcode.kupass.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

internal const val COMPILE_SDK = 37
// Compose 1.13 (BOM 2026.11) compiles only against the Android 37.1 APIs or newer.
internal const val COMPILE_SDK_MINOR = 2
internal const val MIN_SDK = 27

/** Android settings every Kupass module shares. */
internal fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.compileSdk = COMPILE_SDK
    extension.compileSdkMinor = COMPILE_SDK_MINOR
    extension.defaultConfig.minSdk = MIN_SDK
    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_11
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_11
    // Robolectric needs merged resources/manifest (Compose UI tests, UiSnapshotTest).
    extension.testOptions.unitTests.isIncludeAndroidResources = true
    // Robolectric's SDK 37 runtime reaches into java.io.FileDescriptor via jdk.internal.access.
    tasks.withType<Test>().configureEach {
        jvmArgs(
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        )
    }
}
