import com.google.android.wearable.watchface.validator.client.DwfValidatorFactory

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") { content { includeGroup("com.github.xgouchet") } }
    }
    dependencies {
        classpath("com.google.android.wearable.watchface.validator:validator-push:1.0.0-alpha10")
    }
}

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}

/** Validates a WFF APK for Watch Face Push and returns its token (shared by :app and :wear). */
extra["wfToken"] = fun(apk: File, marketplacePackage: String): String {
    val result = DwfValidatorFactory.create().validate(apk, marketplacePackage)
    if (result.failures().isNotEmpty())
        throw GradleException("Watch face ${apk.name} invalid:\n" + result.failures().joinToString("\n") { "${it.name()}: ${it.failureMessage()}" })
    return result.validationToken()
}

/** Builds a :watchface flavor, validates it and drops APK + token into [outDir] as `<name>.apk` / `<name>_token.txt`. */
extra["watchFaceAssetTask"] = fun(project: Project, flavor: String, name: String, outDir: File): TaskProvider<Task> =
    project.tasks.register("prepare${name.replaceFirstChar(Char::titlecase)}") {
        val apk = rootProject.file("watchface/build/outputs/apk/$flavor/release/watchface-$flavor-release.apk")
        dependsOn(":watchface:assemble${flavor.replaceFirstChar(Char::titlecase)}Release")
        inputs.file(apk)
        outputs.dir(outDir)
        doLast {
            @Suppress("UNCHECKED_CAST")
            val token = (rootProject.extra["wfToken"] as (File, String) -> String)(apk, "com.ohav.pixelforge")
            outDir.mkdirs()
            apk.copyTo(File(outDir, "$name.apk"), overwrite = true)
            File(outDir, "${name}_token.txt").writeText(token)
        }
    }
