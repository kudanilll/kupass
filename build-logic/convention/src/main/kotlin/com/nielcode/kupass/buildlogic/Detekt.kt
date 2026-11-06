package com.nielcode.kupass.buildlogic

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/** Default rules plus config/detekt/detekt.yml and the Compose rules. No baseline: fix findings. */
internal fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")
    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(true)
        parallel.set(true)
    }
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
    dependencies.add("detektPlugins", libs.findLibrary("compose-rules-detekt").get())
}
