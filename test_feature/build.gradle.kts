plugins {
    id("com.android.dynamic-feature")
    kotlin("android")
    id("org.jmailen.kotlinter")
    //id("com.banno.gordon") version "localVersion"
}

android {
    namespace = "com.banno.android.gordontest.feature"
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
}

dependencies {
    implementation(project(":test_app"))
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    androidTestImplementation("androidx.test:runner:1.4.0")
}
