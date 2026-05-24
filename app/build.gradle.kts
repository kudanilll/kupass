import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "com.nielcode.kupass"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.nielcode.kupass"
        minSdk = 27
        targetSdk = 37
        versionCode = 5
        versionName = "3.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val properties = Properties()
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists() && secretsFile.isFile) {
        secretsFile.inputStream().use { properties.load(it) }
    }

    signingConfigs {
        getByName("debug") {
        }

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
        debug {
            buildConfigField("String", "GIT_URL", "\"https://github.com/kudanilll/kupass\"")
            buildConfigField("String", "DEV_URL", "\"https://www.kudaniel.my.id\"")
            buildConfigField("String", "DEV_NAME", "\"Achmad Daniel Syahputra\"")

            // Favget API
            buildConfigField("String", "FAVGET_API_URL", "\"https://favget.nielcode.web.id\"")
            buildConfigField("String", "FAVGET_API_KEY", properties.getProperty("FAVGET_API_KEY"))
        }

        release {
            buildConfigField("String", "GIT_URL", "\"https://github.com/kudanilll/kupass\"")
            buildConfigField("String", "DEV_URL", "\"https://www.kudaniel.my.id\"")
            buildConfigField("String", "DEV_NAME", "\"Achmad Daniel Syahputra\"")

            // Favget API
            buildConfigField("String", "FAVGET_API_URL", "\"https://favget.nielcode.web.id\"")
            buildConfigField("String", "FAVGET_API_KEY", properties.getProperty("FAVGET_API_KEY"))

            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.oss.licenses)
    implementation(platform(libs.androidx.compose.bom))
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
}
