// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP drags these onto the *build* classpath (APK signing, XML and JWT handling), and the versions
// it pins carry published CVEs. None of them ship in the APK — `releaseRuntimeClasspath` has no
// trace of them — but they run on developer machines and in CI, so they are pulled up to the
// patched releases here. Drop an entry once AGP itself ships that version or newer.
buildscript {
    configurations.classpath {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.85",
            "org.bouncycastle:bcpkix-jdk18on:1.85",
            "org.bouncycastle:bcutil-jdk18on:1.85",
            "org.jdom:jdom2:2.0.6.1",
            "org.bitbucket.b_c:jose4j:0.9.6",
            "org.apache.commons:commons-lang3:3.18.0",
        )
    }
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
