plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.zirosaur.ziromaps"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zirosaur.ziromaps"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePath: String? = providers.gradleProperty("ZMAP_KEYSTORE_PATH")
        .orElse(providers.environmentVariable("ZMAP_KEYSTORE_PATH"))
        .orNull
    val keystorePass: String? = providers.gradleProperty("ZMAP_KEYSTORE_PASS")
        .orElse(providers.environmentVariable("ZMAP_KEYSTORE_PASS"))
        .orNull
    val releaseKeyAlias: String? = providers.gradleProperty("ZMAP_KEY_ALIAS")
        .orElse(providers.environmentVariable("ZMAP_KEY_ALIAS"))
        .orNull
    val releaseKeyPass: String? = providers.gradleProperty("ZMAP_KEY_PASS")
        .orElse(providers.environmentVariable("ZMAP_KEY_PASS"))
        .orNull

    signingConfigs {
        create("release") {
            val ksFile: java.io.File? = keystorePath?.let { file(it) }
            if (ksFile != null &&
                ksFile.exists() &&
                !keystorePass.isNullOrBlank() &&
                !releaseKeyAlias.isNullOrBlank() &&
                !releaseKeyPass.isNullOrBlank()
            ) {
                storeFile = ksFile
                storePassword = keystorePass
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPass
            } else {
                val debugConfig = getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:geopdf"))
    implementation(project(":core:location"))
    implementation(project(":core:camera"))
    implementation(project(":core:database"))
    implementation(project(":core:export"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
