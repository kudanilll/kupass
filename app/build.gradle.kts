plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "com.nielcode.kupass"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nielcode.kupass"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "GIT_URL", "\"https://github.com/kudanilll/kupass\"")
            buildConfigField("String", "DEV_URL", "\"https://www.kudaniel.my.id\"")
            buildConfigField("String", "DEV_NAME", "\"Achmad Daniel Syahputra\"")
        }
        release {
            buildConfigField("String", "GIT_URL", "\"https://github.com/kudanilll/kupass\"")
            buildConfigField("String", "DEV_URL", "\"https://www.kudaniel.my.id\"")
            buildConfigField("String", "DEV_NAME", "\"Achmad Daniel Syahputra\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.google.android.material)
    implementation(libs.google.gson)
    implementation(libs.io.coil.kt)
    implementation(libs.play.services.oss.licenses)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}