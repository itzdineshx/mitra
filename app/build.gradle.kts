plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mitra.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mitra.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    // Two release builds (docs/DECISIONS.md D-002). No Google Play build.
    flavorDimensions += "distribution"
    productFlavors {
        create("direct") {
            dimension = "distribution"
            // Firebase for family approvals is added here in task T23 (direct only).
        }
        create("foss") {
            dimension = "distribution"
            applicationIdSuffix = ".foss"
            // Must never depend on Firebase or Google Play Services (F-Droid).
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":engine-api"))
    implementation(project(":trust"))
    implementation(project(":agents"))
    implementation(project(":voice"))
    implementation(project(":ui"))
    implementation(project(":engine-access"))
    implementation(project(":engine-guide"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.biometric)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
}
