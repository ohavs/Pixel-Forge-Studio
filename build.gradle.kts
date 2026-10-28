import java.net.URLClassLoader

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}

// The validator runs in its own classloader: its protobuf/tink versions clash with AGP's.
val validatorClasspath = configurations.detachedConfiguration(
    dependencies.create("com.google.android.wearable.watchface.validator:validator-push:1.0.0-alpha09"),
)
/** Validates a WFF APK for Watch Face Push and returns its token (shared by :app and :wear). */
extra["wfToken"] = fun(apk: File, marketplacePackage: String): String {
    val loader = URLClassLoader(validatorClasspath.files.map { it.toURI().toURL() }.toTypedArray(), ClassLoader.getPlatformClassLoader())
    fun Any.call(name: String, vararg args: Any): Any? =
        javaClass.methods.first { it.name == name && it.parameterCount == args.size }.apply { isAccessible = true }.invoke(this, *args)
    val validator = loader.loadClass("com.google.android.wearable.watchface.validator.client.DwfValidatorFactory").getMethod("create").invoke(null)!!
    val result = validator.call("validate", apk, marketplacePackage)!!
    val failures = result.call("failures") as Collection<*>
    if (failures.isNotEmpty())
        throw GradleException("Watch face ${apk.name} invalid:\n" + failures.joinToString("\n"))
    return result.call("validationToken") as String
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
