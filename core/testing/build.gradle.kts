plugins { alias(libs.plugins.kupass.android.library) }

android { namespace = "com.nielcode.kupass.core.testing" }

// Shared test fakes and rules; only ever a testImplementation dependency.
dependencies {
    api(project(":core:data"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
