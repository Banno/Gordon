plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    namespace = "com.banno.android.gordontest.feature"
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
}

dependencies {
    implementation(project(":test_app"))
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
