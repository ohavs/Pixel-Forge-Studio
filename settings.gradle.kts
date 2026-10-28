pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Watch Face Push validator needs an APK parser that only lives on JitPack.
        maven("https://jitpack.io") { content { includeGroup("com.github.xgouchet") } }
    }
}
rootProject.name = "Pixel-Forge-Studio"
include(":app")
include(":wear")
include(":watchface")
