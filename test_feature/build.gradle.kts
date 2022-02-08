plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":test_app"))
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
