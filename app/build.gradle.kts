plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ossLicenses)
}

@Suppress("UnstableApiUsage")
android {
    namespace = "com.nielcode.kupass"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nielcode.kupass"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.activity)
    implementation(libs.preference)
    implementation(libs.gson)
    implementation(libs.gms.play.services.oss.licenses)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}