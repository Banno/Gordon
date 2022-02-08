plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions.add("foo")
    productFlavors {
        register("bar") {
            dimension = "foo"
        }
        register("baz") {
            dimension = "foo"
            testInstrumentationRunnerArguments["notAnnotation"] = "org.junit.Test"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
