plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.11.0"
    `maven-publish`
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
}

repositories {
    google()
    jcenter()
    maven("https://www.jitpack.io")
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.1")

    val androidGradlePluginVersion: String by project
    implementation("com.android.tools.build:gradle:$androidGradlePluginVersion")
    implementation("com.android.tools.build:bundletool:0.15.0")
    implementation("org.smali:dexlib2:2.4.0")
    implementation("com.github.vidstige:jadb:v1.1.0")

    implementation("io.arrow-kt:arrow-core-data:0.10.5")
    implementation("io.arrow-kt:arrow-fx:0.10.5")

    testImplementation("junit:junit:4.13")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
}

tasks.withType<Test>().configureEach {
    dependsOn(":app:assembleDebugAndroidTest")
    testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
}

val aapt2Version = "4.0.0-6051327"
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
    plugins {
        register("gordon") {
            id = "com.banno.gordon"
            implementationClass = "com.banno.gordon.GordonPlugin"
            displayName = "Gordon"
            description = "Android instrumentation test runner designed for speed, simplicity, and reliability"
        }
    }
}

pluginBundle {
    website = "https://github.com/Banno/Gordon"
    vcsUrl = "https://github.com/Banno/Gordon"
    tags = setOf("android", "instrumentation", "test", "runner")
}

extra["gradle.publish.key"] = System.getenv("GRADLE_PLUGIN_PUBLISH_KEY")
extra["gradle.publish.secret"] = System.getenv("GRADLE_PLUGIN_PUBLISH_SECRET")
