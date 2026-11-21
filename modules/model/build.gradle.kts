plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mitra.model"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    // implementation(libs.litertlm.android)   // task T06: enable together with the version in libs.versions.toml
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
