plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    namespace = "com.banno.android.gordontest"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        minSdk = 26
        targetSdk = 35
        applicationId = "com.banno.android.gordontest"
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility("21")
        targetCompatibility("21")
    }
    kotlinOptions {
        jvmTarget = "21"
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
