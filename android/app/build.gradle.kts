import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.baselineprofile)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "app.palettez"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.palettez"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.2.1"
    }

    signingConfigs {
        create("release") {
            val envStore = System.getenv("PALETTEZ_KEYSTORE_PATH")
            val propStore = keystoreProperties.getProperty("storeFile")
            val resolvedStore = when {
                !envStore.isNullOrBlank() -> file(envStore)
                !propStore.isNullOrBlank() -> rootProject.file(propStore)
                else -> null
            }
            if (resolvedStore != null && resolvedStore.exists()) {
                storeFile = resolvedStore
                storePassword = System.getenv("PALETTEZ_KEYSTORE_PASSWORD")
                    ?: keystoreProperties.getProperty("storePassword")
                keyAlias = System.getenv("PALETTEZ_KEY_ALIAS")
                    ?: keystoreProperties.getProperty("keyAlias")
                keyPassword = System.getenv("PALETTEZ_KEY_PASSWORD")
                    ?: keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                // Fallback to debug key only when no local keystore or CI env is configured.
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    // M3 kept as infrastructure (Surface etc.); Palettez design system is built on top.
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    // DataStore
    implementation(libs.datastore.preferences)

    // Network
    implementation(libs.okhttp)

    // Image loading
    implementation(libs.coil.compose)

    // Baseline Profiles — ships the compiled profile inside the APK/AAB
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":macrobenchmark"))

    // Unit testing (JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    // Roborazzi screenshot tests (runs on JVM via Robolectric)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.rule)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.ui.tooling)

    // Compose instrumented UI tests (androidTest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
