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
        id("com.android.application") version androidGradlePluginVersion
        id("com.android.library") version androidGradlePluginVersion
        id("com.android.dynamic-feature") version androidGradlePluginVersion
        id("com.gradle.plugin-publish") version gradlePluginPublishVersion
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
