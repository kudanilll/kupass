import java.util.Properties
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kupass.android.application)
    alias(libs.plugins.kupass.android.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "com.nielcode.kupass"

    val properties = Properties()
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists() && secretsFile.isFile) {
        secretsFile.inputStream().use { properties.load(it) }
    }

    defaultConfig {
        applicationId = "com.nielcode.kupass"
        targetSdk = 37
        versionCode = 5
        versionName = "3.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GIT_URL", "\"https://github.com/kudanilll/kupass\"")
        buildConfigField("String", "DEV_URL", "\"https://achmaddaniel.nielcode.com\"")
        buildConfigField("String", "DEV_NAME", "\"Achmad Daniel Syahputra\"")

        // Favget site icons. Without a key, entries keep their letter fallback.
        buildConfigField("String", "FAVGET_API_URL", "\"https://favget.nielcode.web.id\"")
        // Escaped so the key stays a valid Java string literal in the generated BuildConfig.
        val favgetKey =
            properties
                .getProperty("FAVGET_API_KEY")
                .orEmpty()
                .trim()
                // Tolerate KEY="value": .properties files keep quotes as part of the value.
                .removeSurrounding("\"")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        buildConfigField("String", "FAVGET_API_KEY", "\"$favgetKey\"")
    }

    signingConfigs {
        getByName("debug") {}

        create("release") {
            val keystoreFile = rootProject.file("release-key.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = properties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
            } else {
                val debugSigning = getByName("debug")
                storeFile = debugSigning.storeFile
                storePassword = debugSigning.storePassword
                keyAlias = debugSigning.keyAlias
                keyPassword = debugSigning.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    testOptions {
        unitTests.all {
            // UiSnapshotTest uses Robolectric native graphics, which can't share a JVM with the
            // other sandboxes. Run it on its own: -Pkupass.snapshots --tests "*UiSnapshotTest".
            if (!project.hasProperty("kupass.snapshots")) it.exclude("**/UiSnapshotTest*")
        }
    }

    buildFeatures { buildConfig = true }
}

// Detekt's Android classpath omits generated Java bytecode, including BuildConfig.
tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    if (name == "detektDebug" || name == "detektRelease") {
        val variant = name.removePrefix("detekt")
        val kotlinCompile = tasks.named<KotlinJvmCompile>("compile${variant}Kotlin")
        classpath.setFrom(
            kotlinCompile.map { it.libraries },
            kotlinCompile.flatMap { it.destinationDirectory },
            tasks.named<JavaCompile>("compile${variant}JavaWithJavac").flatMap {
                it.destinationDirectory
            },
        )
    }
}

dependencies {
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:security"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material)
    implementation(libs.play.services.oss.licenses)
    implementation(platform(libs.androidx.compose.bom))
    testImplementation(project(":core:testing"))
    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
