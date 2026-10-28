import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Same id and key as the phone app: the Data Layer only pairs apps that match on both.
val localSigning = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, key: String): String? = System.getenv(env) ?: localSigning.getProperty(key)
val buildNumber = (System.getenv("BUILD_NUMBER") ?: localSigning.getProperty("buildNumber") ?: "1").toInt()

// Default watch face + its Watch Face Push token, generated from :watchface.
val wfAssets = layout.buildDirectory.dir("wf/assets").get().asFile
val wfRes = layout.buildDirectory.dir("wf/res").get().asFile
@Suppress("UNCHECKED_CAST")
val prepareFace = (rootProject.extra["watchFaceAssetTask"] as (Project, String, String, File) -> TaskProvider<Task>)(project, "home", "default_watchface", wfAssets)
val tokenRes = tasks.register("defaultWatchFaceTokenRes") {
    dependsOn(prepareFace)
    outputs.dir(wfRes)
    doLast {
        val token = File(wfAssets, "default_watchface_token.txt").readText()
        File(wfRes, "values").mkdirs()
        File(wfRes, "values/wf_token.xml").writeText("<resources><string name=\"default_wf_token\" translatable=\"false\">$token</string></resources>")
    }
}

android {
    namespace = "com.ohav.pixelforge.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ohav.pixelforge"
        minSdk = 36
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    sourceSets["main"].assets.srcDir(wfAssets)
    sourceSets["main"].res.srcDir(wfRes)

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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
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

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.named("preBuild") { dependsOn(tokenRes) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.wear.compose:compose-material3:1.5.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.wear.watchfacepush:watchfacepush:1.0.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
}
