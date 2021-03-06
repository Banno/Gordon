include("gordon-plugin", "test_app", "test_feature", "test_library")

pluginManagement {
    val androidGradlePluginVersion: String by settings
    val kotlinVersion: String by settings
    val kotlinterVersion: String by settings
    val gradlePluginPublishVersion: String by settings

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
    }

    plugins {
        kotlin("android") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
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
