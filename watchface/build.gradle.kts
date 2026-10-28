// Watch Face Format packages (resources only) installed through Watch Face Push.
// Package names must be "<marketplace app id>.watchfacepush.<name>" and signed with a key
// other than the app's, so they use the CI debug key.
plugins {
    id("com.android.application")
}

val buildNumber = (System.getenv("BUILD_NUMBER") ?: "1").toInt()

android {
    namespace = "com.ohav.pixelforge.watchfacepush"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        targetSdk = 33
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    flavorDimensions += "face"
    productFlavors {
        // Default face the watch installs together with the Pixel Forge watch app.
        create("home") {
            dimension = "face"
            applicationId = "com.ohav.pixelforge.watchfacepush.home"
            resValue("string", "app_name", "Pixel Forge")
        }
        // Template the phone rewrites (raw/watchface.xml) and re-signs on device.
        create("custom") {
            dimension = "face"
            applicationId = "com.ohav.pixelforge.watchfacepush.custom1"
            resValue("string", "app_name", "Pixel Forge Studio")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            // No code at all: minify strips the dex, which Watch Face Push requires.
            isMinifyEnabled = true
            isShrinkResources = false
        }
    }
}
