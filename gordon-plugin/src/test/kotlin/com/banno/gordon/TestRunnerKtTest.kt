package com.banno.gordon

import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestRunnerKtTest {

    private val testTimeout = 2000L
    private val device = anyDevice()

    private val buildDir = File("build")
    private val taskName = "gordon"

    @Test
    fun `should identify a passed test correctly`() {
        // GIVEN
        device.mockResponse({ match { it.contains("am instrument") } }) {
            """
                com.example.app.ui.ExampleFragmentTest:.

                Time: 1.562

                OK (1 test)


                Generated code coverage data to /data/user/10/com.example.app/files/should_show_something.ec
            """.trimIndent()
        }

        // WHEN
        val testResult = runTest()

        // THEN
        assertTrue(testResult is TestResult.Passed)
    }

    @Test
    fun `should identify ignored tests correctly`() {
        // GIVEN
        device.mockResponse({ match { it.contains("am instrument") } }) {
            """
            Time: 0

            OK (0 tests)


            Generated code coverage data to /data/user/10/com.example.app/files/ignored_test.ec"

            """.trimIndent()
        }

        // WHEN
        val testResult = runTest()

        // THEN
        assertTrue(testResult is TestResult.Ignored)
    }

    @Test
    fun `should treat any other result as a failure`() {
        // GIVEN
        device.mockResponse({ match { it.contains("am instrument") } }) {
            """
            Lorem ipsum
            """.trimIndent()
        }

        // WHEN
        val testResult = runTest()

        // THEN
        assertTrue(testResult is TestResult.Failed)
    }

    private fun runTest(): TestResult = runTest(
        logger = mockk(relaxed = true),
        testedApplicationPackage = "com.example.app",
        instrumentationPackage = "com.example.app.test",
        InstrumentationRunnerOptions("androidx.test.runner.AndroidJUnitRunner", mapOf("coverage" to "true"), true),
        testTimeout,
        TestCase(
            "class com.example.app.ui.ExampleFragmentTest",
            "should_show_something",
            false
        ),
        device,
        "",
        buildDir,
        taskName
    )
}
