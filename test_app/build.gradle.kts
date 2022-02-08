plugins {
    id("com.android.application")
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
        applicationId = "com.banno.android.gordontest"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val debugSigningConfig = signingConfigs.register("debugSigningConfig") {
        storeFile = file("debug.keystore")
        storePassword = "bigbago"
        keyAlias = "key0"
        keyPassword = "pickles"
    }
    buildTypes.named("debug") {
        signingConfig = debugSigningConfig.get()
    }
    dynamicFeatures.add(
        ":test_feature"
    )
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
