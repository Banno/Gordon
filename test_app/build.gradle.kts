plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    namespace = "com.banno.android.gordontest"
    compileSdk = 33
    buildToolsVersion = "33.0.2"
    defaultConfig {
        minSdk = 26
        targetSdk = 33
        applicationId = "com.banno.android.gordontest"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility("17")
        targetCompatibility("17")
    }
    kotlinOptions {
        jvmTarget = "17"
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

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
