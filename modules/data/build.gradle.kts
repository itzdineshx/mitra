plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.mitra.data"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Room + encryption are added in task T09 (see docs/DECISIONS.md Q-3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
