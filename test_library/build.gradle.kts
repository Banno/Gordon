plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    namespace = "com.banno.android.gordontest.library"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility("21")
        targetCompatibility("21")
    }
    kotlinOptions {
        jvmTarget = "21"
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
