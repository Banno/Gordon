include("gordon-plugin", "test-app")

pluginManagement {
    val androidGradlePluginVersion: String by settings
    val kotlinVersion: String by settings
    val kotlinterVersion: String by settings
    val gradlePluginPublishVersion: String by settings

    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenLocal()
        maven("https://www.jitpack.io")
    }

    plugins {
        kotlin("android") version kotlinVersion
        kotlin("plugin.serialization") version "1.3.72" //TODO - When Gradle's embedded Kotlin version is updated to 1.4.0+, this can be changed back to `kotlinVersion`
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("com.gradle.plugin-publish") version gradlePluginPublishVersion
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:$androidGradlePluginVersion")
            }
        }
    }
}
