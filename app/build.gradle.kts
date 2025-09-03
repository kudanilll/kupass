plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.oss.licenses)
}

android {
    namespace = "com.nielcode.kupass"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nielcode.kupass"
        minSdk = 27
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
            signingConfig = signingConfigs.getByName("debug")
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    //arg("room.generateKotlin", "true")
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    implementation(libs.android.database.sqlcipher)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.sqlite)
    implementation(libs.google.android.material)
    implementation(libs.google.gson)
    implementation(libs.io.coil.kt)
    implementation(libs.play.services.oss.licenses)
    implementation(libs.room.ktx)
    implementation(libs.room.runtime)
    implementation(libs.tink.android)

    ksp(libs.room.compiler)

    testImplementation(libs.junit)
}
