plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    compileSdk = 30
    buildToolsVersion = "30.0.3"
    defaultConfig {
        minSdk = 21
        targetSdk = 30
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
    implementation("androidx.appcompat:appcompat:1.3.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
