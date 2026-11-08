plugins {
    `kotlin-dsl`
    // `kotlin-dsl` alone pulls Gradle's embedded Kotlin Gradle plugin (2.4.10 in Gradle 9.8.2),
    // which has CVE-2026-53914. Declaring the plugin here lifts it to the project's Kotlin version.
    // Drop this once the Gradle wrapper embeds Kotlin 2.4.20 or newer.
    alias(libs.plugins.kotlin.jvm)
}

group = "com.nielcode.kupass.buildlogic"

dependencies {
    // Provided at runtime by the root build's `apply false` plugin declarations.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.kupass.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.kupass.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.kupass.android.compose.get().pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
