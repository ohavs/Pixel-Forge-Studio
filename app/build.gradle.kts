import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing: CI passes env vars; local builds read keystore.properties (gitignored).
val localSigning = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, key: String): String? = System.getenv(env) ?: localSigning.getProperty(key)

// versionCode = CI run number (local builds default to 1).
val buildNumber = (System.getenv("BUILD_NUMBER") ?: localSigning.getProperty("buildNumber") ?: "1").toInt()

// Template watch face (+ CI token) the phone customizes and pushes to the watch.
val wfAssets = layout.buildDirectory.dir("wf/assets").get().asFile
@Suppress("UNCHECKED_CAST")
val prepareTemplate = (rootProject.extra["watchFaceAssetTask"] as (Project, String, String, File) -> TaskProvider<Task>)(project, "custom", "custom_template", wfAssets)

android {
    namespace = "com.ohav.pixelforge"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ohav.pixelforge"
        minSdk = 30
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
        buildConfigField("String", "GITHUB_REPO", "\"${System.getenv("GITHUB_REPOSITORY") ?: localSigning.getProperty("githubRepo") ?: "ohavs/Pixel-Forge-Studio"}\"")
    }

    sourceSets["main"].assets.srcDir(wfAssets)

    signingConfigs {
        create("release") {
            val ks = signingValue("KEYSTORE_FILE", "storeFile")
            if (ks != null) {
                storeFile = rootProject.file(ks)
                storePassword = signingValue("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.named("preBuild") { dependsOn(prepareTemplate) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("com.android.tools.build:apksig:8.13.2")
    implementation("com.google.android.wearable.watchface.validator:validator-push-android:1.0.0-alpha09")
}
