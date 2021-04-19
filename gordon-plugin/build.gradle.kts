plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    `maven-publish`
    kotlin("plugin.serialization")
    id("org.jmailen.kotlinter")
}

repositories {
    google()
    mavenCentral()
    jcenter {
        content {
            includeModuleByRegex("org\\.jetbrains\\.kotlinx", "kotlinx-html.*")
        }
    }
    maven("https://www.jitpack.io")
}

val androidGradlePluginVersion: String by project
val aapt2Version: String by project

dependencies {
    implementation(gradleKotlinDsl())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")

    implementation("com.android.tools.build:gradle:$androidGradlePluginVersion")
    implementation("com.android.tools.build:bundletool:1.5.0")
    implementation("org.smali:dexlib2:2.4.0")
    implementation("com.github.vidstige:jadb:v1.1.0")

    implementation("io.arrow-kt:arrow-core-data:0.11.0")
    implementation("io.arrow-kt:arrow-fx:0.11.0")

    testImplementation("junit:junit:4.13")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
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
