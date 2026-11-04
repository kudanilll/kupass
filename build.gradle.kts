// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP drags these onto the *build* classpath (APK signing, XML, JWT and HTTP handling, Guava), and
// the versions it pins carry published CVEs. None of them ship in the APK (the release runtime
// classpath has no trace of them), but they run on developer machines and in CI, so they are
// pulled up to the patched releases here. Drop an entry once AGP itself ships that version or
// newer.
buildscript {
    configurations.classpath {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.86",
            "org.bouncycastle:bcpkix-jdk18on:1.86",
            "org.bouncycastle:bcutil-jdk18on:1.86",
            "org.jdom:jdom2:2.0.6.1",
            "org.bitbucket.b_c:jose4j:0.9.7",
            "org.apache.commons:commons-lang3:3.21.0",
            "org.apache.httpcomponents:httpclient:4.5.14",
            "com.google.guava:guava:33.7.2-jre",
        )
    }
}

// Lint (`androidLintTool`), KSP processors and Robolectric resolve their own copies of the same
// libraries inside each module, so the list above is applied there too. The app's own debug/release
// classpaths are skipped so a pin can never downgrade a library that ships in the APK.
val patchedBuildTools =
    buildscript.configurations.getByName("classpath").resolutionStrategy.forcedModules.map {
        "${it.group}:${it.name}:${it.version}"
    }
val apkClasspath = Regex("(debug|release)(Compile|Runtime)Classpath")

subprojects {
    configurations
        .matching { !apkClasspath.matches(it.name) }
        .configureEach { resolutionStrategy.force(*patchedBuildTools.toTypedArray()) }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless)
}

// Formatting for the whole repository: `./gradlew spotlessApply` fixes, `spotlessCheck` verifies.
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
    kotlinGradle {
        target("*.gradle.kts", "*/*.gradle.kts")
        ktfmt(libs.versions.ktfmt.get()).kotlinlangStyle()
    }
}
