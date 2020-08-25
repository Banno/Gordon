plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(29)
        applicationId = "com.banno.android.gordontest"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

repositories {
    google()
    jcenter()
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.2.0")
    androidTestImplementation("androidx.test:runner:1.2.0")
}
