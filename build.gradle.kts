plugins {
    kotlin("jvm") apply false
    kotlin("android") apply false
    kotlin("android.extensions") apply false
    kotlin("plugin.serialization") apply false
    id("org.jmailen.kotlinter") apply false
    id("com.android.application") apply false
}

tasks.register<Delete>("clean") {
    delete(buildDir)
}
