plugins { kotlin("jvm") version "2.2.21"; application }
repositories { mavenCentral() }
dependencies { implementation("org.json:json:20240303") }
kotlin { jvmToolchain(21) }
sourceSets["main"].kotlin.srcDir("../../app/src/main/java/com/ohav/pixelforge/editor")
sourceSets["main"].kotlin.exclude("**/EditorState.kt", "**/Assets.kt")
application { mainClass.set("MainKt") }
