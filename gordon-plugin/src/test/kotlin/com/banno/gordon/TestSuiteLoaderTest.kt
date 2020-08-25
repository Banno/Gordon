package com.banno.gordon

import org.junit.Test
import java.io.File

class TestSuiteLoaderTest {

    @Test
    fun loadingTestSuite() {
        val instrumentation = File("src/test/assets/app-debug-androidTest.apk")

        val testSuite = loadTestSuite(instrumentation).unsafeRunSync()

        testSuite.sortedBy { it.fullyQualifiedClassName + it.methodName } shouldEqual listOf(
            TestCase("com.banno.android.gordontest.AnnotatedTestClass", "annotatedA", false, setOf("androidx.test.filters.FlakyTest", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.AnnotatedTestClass", "annotatedB", false, setOf("androidx.test.filters.FlakyTest", "androidx.test.filters.SmallTest", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.AnnotatedTest", "annotatedA", false, setOf("androidx.test.filters.LargeTest", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.AnnotatedTest", "annotatedB", false, setOf("androidx.test.filters.SmallTest", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.AnnotatedTest", "annotatedC", false, setOf("androidx.test.filters.LargeTest", "androidx.test.filters.FlakyTest", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.BaseClassTest", "baseA", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.BaseClassTest", "baseB", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.BaseClassTest", "baseC", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ClassOne", "a1", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ClassOne", "b1", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ClassOne", "c1", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ClassTwo", "a2", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ClassTwo", "b2", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ClassTwo", "c2", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.FailingTest", "failure1", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.FailingTest", "failure2", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.FailingTest", "failure3", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.FlakyTest", "flaky", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.IgnoredClass", "a", true, setOf("org.junit.Ignore", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.IgnoredClass", "b", true, setOf("org.junit.Ignore", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.IgnoredClass", "c", true, setOf("org.junit.Ignore", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.InheritedTest", "a", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.InheritedTest", "b", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.InheritedTest", "c", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.InheritedTest", "interfaceA", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.InheritedTest", "interfaceB", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.InheritedTest", "interfaceC", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.ParameterizedTest", "parameterizedFail", false, setOf("org.junit.runner.RunWith", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.ParameterizedTest", "parameterizedIgnore", true, setOf("org.junit.runner.RunWith", "org.junit.Ignore", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.ParameterizedTest", "parameterizedPass", false, setOf("org.junit.runner.RunWith", "org.junit.Test")),
            TestCase("com.banno.android.gordontest.StandardTest", "standardA", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.StandardTest", "standardB", false, setOf("org.junit.Test")),
            TestCase("com.banno.android.gordontest.StandardTest", "standardIgnore", true, setOf("org.junit.Ignore", "org.junit.Test"))
        )
        /**
         * Purposefully ignoring:
         * - AbstractTest, tests in abstract base classes aren't picked up.
         */
    }
}
