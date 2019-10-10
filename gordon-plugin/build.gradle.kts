plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.1"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.12")

    val androidGradlePluginVersion: String by project
    implementation("com.android.tools.build:gradle:$androidGradlePluginVersion")

    implementation("com.github.vidstige:jadb:v1.1.0")
    implementation("org.smali:dexlib:1.4.2")
    implementation("com.shazam:axmlparser:1.0")

    implementation("io.arrow-kt:arrow-core-data:0.9.0")
    implementation("io.arrow-kt:arrow-effects-io-extensions:0.9.0")

    testImplementation("junit:junit:4.12")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
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
