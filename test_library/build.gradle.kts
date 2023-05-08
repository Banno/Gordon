plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    namespace = "com.banno.android.gordontest.library"
    compileSdk = 33
    buildToolsVersion = "33.0.2"
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility("17")
        targetCompatibility("17")
    }
    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
