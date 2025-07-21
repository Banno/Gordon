import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish")
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
}

val androidGradlePluginVersion: String by project
val aapt2Version: String by project

dependencies {
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.3")

    implementation("com.android.tools.build:gradle:$androidGradlePluginVersion")
    implementation("com.android.tools.build:bundletool:1.17.2")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("org.smali:dexlib2:2.5.2")

    implementation("io.arrow-kt:arrow-core:0.13.2")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.mockk:mockk:1.12.0")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "21"
    targetCompatibility = "17"
}

tasks.withType<Test>().configureEach {
    dependsOn(":test_app:assembleDebugAndroidTest")
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}

val jar = tasks.named<Jar>("jar")
mapOf(
    "linux" to "linux/",
    "osx" to "macos/",
    "windows" to "windows/"
).forEach { (classifier, jarDestination) ->
    val configuration = configurations.register("aapt2$classifier")
    dependencies.add(configuration.name, "com.android.tools.build:aapt2:$aapt2Version:$classifier")
    jar.configure { from(configuration.map { zipTree(it.singleFile) }) { into(jarDestination) } }
}

gradlePlugin {
    website.set("https://github.com/Banno/Gordon")
    vcsUrl.set("https://github.com/Banno/Gordon")
    plugins {
        register("gordon") {
            id = "com.banno.gordon"
            implementationClass = "com.banno.gordon.GordonPlugin"
            displayName = "Gordon"
            description = "Android instrumentation test runner designed for speed, simplicity, and reliability"
            tags.set(setOf("android", "instrumentation", "test", "runner"))
        }
    }
}

extra["gradle.publish.key"] = System.getenv("GRADLE_PLUGIN_PUBLISH_KEY")
extra["gradle.publish.secret"] = System.getenv("GRADLE_PLUGIN_PUBLISH_SECRET")
