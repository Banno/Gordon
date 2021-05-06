plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    compileSdkVersion(30)
    buildToolsVersion("30.0.3")
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":test_app"))
    implementation("androidx.fragment:fragment-ktx:1.3.3")
    androidTestImplementation("androidx.test:runner:1.3.0")
}
