package com.banno.gordon

import org.junit.Test
import java.io.File

class ManifestLoaderTest {

    @Test
    fun applicationManifestTest() {
        val application = File("src/test/assets/app-debug.apk")

        val actual = getManifestPackage(application).unsafeRunSync()

        actual shouldEqual "com.banno.android.gordontest"
    }

    @Test
    fun instrumentationManifestTest() {
        val instrumentation = File("src/test/assets/app-debug-androidTest.apk")

        val actual = getManifestPackage(instrumentation).unsafeRunSync()

        actual shouldEqual "com.banno.android.gordontest.test"
    }
}