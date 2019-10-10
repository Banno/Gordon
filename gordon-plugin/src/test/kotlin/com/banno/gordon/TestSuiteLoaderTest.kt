package com.banno.gordon

import org.junit.Test
import java.io.File

class TestSuiteLoaderTest {

    @Test
    fun loadingTestSuite() {
        val instrumentation = File("src/test/assets/app-debug-androidTest.apk")

        val testSuite = loadTestSuite(instrumentation).unsafeRunSync()

        testSuite.sortedBy { it.fullyQualifiedClassName + it.methodName } shouldEqual listOf(
            TestCase("com.banno.android.gordontest.BaseClassTest", "baseA", false),
            TestCase("com.banno.android.gordontest.BaseClassTest", "baseB", false),
            TestCase("com.banno.android.gordontest.BaseClassTest", "baseC", false),
            TestCase("com.banno.android.gordontest.ClassOne", "a1", false),
            TestCase("com.banno.android.gordontest.ClassOne", "b1", false),
            TestCase("com.banno.android.gordontest.ClassOne", "c1", false),
            TestCase("com.banno.android.gordontest.ClassTwo", "a2", false),
            TestCase("com.banno.android.gordontest.ClassTwo", "b2", false),
            TestCase("com.banno.android.gordontest.ClassTwo", "c2", false),
            TestCase("com.banno.android.gordontest.FailingTest", "failure1", false),
            TestCase("com.banno.android.gordontest.FailingTest", "failure2", false),
            TestCase("com.banno.android.gordontest.FailingTest", "failure3", false),
            TestCase("com.banno.android.gordontest.FlakyTest", "flaky", false),
            TestCase("com.banno.android.gordontest.IgnoredClass", "a", true),
            TestCase("com.banno.android.gordontest.IgnoredClass", "b", true),
            TestCase("com.banno.android.gordontest.IgnoredClass", "c", true),
            TestCase("com.banno.android.gordontest.InheritedTest", "a", false),
            TestCase("com.banno.android.gordontest.InheritedTest", "b", false),
            TestCase("com.banno.android.gordontest.InheritedTest", "c", false),
            TestCase("com.banno.android.gordontest.InheritedTest", "interfaceA", false),
            TestCase("com.banno.android.gordontest.InheritedTest", "interfaceB", false),
            TestCase("com.banno.android.gordontest.InheritedTest", "interfaceC", false),
            TestCase("com.banno.android.gordontest.StandardTest", "standardA", false),
            TestCase("com.banno.android.gordontest.StandardTest", "standardB", false),
            TestCase("com.banno.android.gordontest.StandardTest", "standardC", true)
        )
        /**
         * Purposefully ignoring:
         * - AbstractTest, tests in abstract base classes aren't picked up.
         */
    }
}
