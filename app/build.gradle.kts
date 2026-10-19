plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.morfo"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.morfo"
        // Target devices: Samsung Galaxy S25 Edge and Galaxy Tab S9 FE (arm64). Android 10+ is plenty.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    val keystorePath = project.findProperty("RELEASE_STORE_FILE") as String?
        ?: System.getenv("RELEASE_STORE_FILE")
    val hasReleaseKeystore = !keystorePath.isNullOrEmpty() && file(keystorePath).exists()

    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
                    ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
                    ?: System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
                    ?: System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    androidResources {
        // GLB files are already binary-packed; keep them uncompressed so they can be memory-mapped quickly.
        noCompress += "glb"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.filament.android)
    implementation(libs.filament.gltfio)
    implementation(libs.filament.utils)
    testImplementation(libs.junit)
}
