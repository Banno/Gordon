include("gordon-plugin", "app")

val androidGradlePluginVersion: String by settings
val kotlinVersion: String by settings
val kotlinterVersion: String by settings

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenLocal()
        maven("https://www.jitpack.io")
    }

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("android") version kotlinVersion
        kotlin("android.extensions") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:$androidGradlePluginVersion")
            }
        }
    }
}
