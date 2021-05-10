package com.banno.gordon

import com.android.tools.build.bundletool.device.Device
import com.banno.gordon.TestResult.Failed
import com.banno.gordon.TestResult.Flaky
import com.banno.gordon.TestResult.NotRun
import com.banno.gordon.TestResult.Passed
import io.mockk.MockKAdditionalAnswerScope
import io.mockk.MockKStubScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import shadow.bundletool.com.android.ddmlib.IShellOutputReceiver
import java.util.UUID

class TestDistributionTest {

    @Test
    fun shouldRunTestSuiteAcrossAllPools() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false),
            TestCase("A", "methodTwo", false),
            TestCase("B", "methodOne", false),
            TestCase("B", "methodTwo", false),
            TestCase("C", "methodOne", false),
            TestCase("C", "methodTwo", false)
        )

        val pools = listOf(
            DevicePool(
                "First",
                listOf(
                    anyDevice(),
                    anyDevice()
                )
            ),
            DevicePool(
                "Second",
                listOf(
                    anyDevice(),
                    anyDevice()
                )
            ),
            DevicePool(
                "Third",
                listOf(
                    anyDevice(),
                    anyDevice()
                )
            )
        )

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 0,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Passed(null),
                TestCase("A", "methodTwo", false) to Passed(null),
                TestCase("B", "methodOne", false) to Passed(null),
                TestCase("B", "methodTwo", false) to Passed(null),
                TestCase("C", "methodOne", false) to Passed(null),
                TestCase("C", "methodTwo", false) to Passed(null)
            ),
            "Second" to mapOf(
                TestCase("A", "methodOne", false) to Passed(null),
                TestCase("A", "methodTwo", false) to Passed(null),
                TestCase("B", "methodOne", false) to Passed(null),
                TestCase("B", "methodTwo", false) to Passed(null),
                TestCase("C", "methodOne", false) to Passed(null),
                TestCase("C", "methodTwo", false) to Passed(null)
            ),
            "Third" to mapOf(
                TestCase("A", "methodOne", false) to Passed(null),
                TestCase("A", "methodTwo", false) to Passed(null),
                TestCase("B", "methodOne", false) to Passed(null),
                TestCase("B", "methodTwo", false) to Passed(null),
                TestCase("C", "methodOne", false) to Passed(null),
                TestCase("C", "methodTwo", false) to Passed(null)
            )
        )
    }

    @Test
    fun shouldRetryTestsThatWereNotRun() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(
            TestResponse.EXCEPTION,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Passed(null)
            )
        )

        verify(exactly = 2) { device.executeShellCommand(match { it.contains("A#methodOne") }, any(), any(), any()) }
    }

    @Test
    fun shouldRetryFailedTests() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(
            TestResponse.FAIL,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Flaky(null, listOf(Failure(null, "FAILURE", device.serialNumber)))
            )
        )

        verify(exactly = 2) { device.executeShellCommand(match { it.contains("A#methodOne") }, any(), any(), any()) }
    }

    @Test
    fun shouldRetryQuotaNumberOfTimesForNotRun() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(
            TestResponse.EXCEPTION,
            TestResponse.EXCEPTION,
            TestResponse.EXCEPTION,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 2,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to NotRun
            )
        )

        verify(exactly = 3) { device.executeShellCommand(match { it.contains("A#methodOne") }, any(), any(), any()) }
    }

    @Test
    fun shouldRetryQuotaNumberOfTimesForFailedTests() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(
            TestResponse.FAIL,
            TestResponse.FAIL,
            TestResponse.FAIL,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 2,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Failed(
                    listOf(
                        Failure(null, "FAILURE", device.serialNumber),
                        Failure(null, "FAILURE", device.serialNumber),
                        Failure(null, "FAILURE", device.serialNumber)
                    )

                )
            )
        )

        verify(exactly = 3) { device.executeShellCommand(match { it.contains("A#methodOne") }, any(), any(), any()) }
    }

    @Test
    fun shouldRetryFailedTestsOnDifferentDevices() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()
        val device2 = anyDevice()

        device.mockOrderedTestResponses(TestResponse.FAIL)

        device2.mockOrderedTestResponses(TestResponse.FAIL)

        val pools = listOf(DevicePool("First", listOf(device, device2)))

        runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        )

        verify(exactly = 1) { device.executeShellCommand(match { it.contains("A#methodOne") }, any(), any(), any()) }
        verify(exactly = 1) { device2.executeShellCommand(match { it.contains("A#methodOne") }, any(), any(), any()) }
    }

    @Test
    fun shouldNotOverwriteFailureIfRetryDoesNotRun() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(
            TestResponse.FAIL,
            TestResponse.EXCEPTION
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Failed(null, "FAILURE", device.serialNumber)
            )
        )
    }

    @Test
    fun shouldAggregateTestFailureSerials() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(TestResponse.FAIL, TestResponse.FAIL)

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Failed(
                    listOf(
                        Failure(null, "FAILURE", device.serialNumber),
                        Failure(null, "FAILURE", device.serialNumber)
                    )
                )
            )
        )
    }

    @Test
    fun shouldAggregateFailuresWithFlakyResult() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(TestResponse.FAIL, TestResponse.FAIL, TestResponse.PASS)

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 3,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Flaky(
                    null,
                    listOf(
                        Failure(null, "FAILURE", device.serialNumber),
                        Failure(null, "FAILURE", device.serialNumber)
                    )
                )
            )
        )
    }

    @Test
    fun shouldHandleMultipleTestsRunInSingleTestCase() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyDevice()

        device.mockOrderedTestResponses(TestResponse.MULTIPLE_PASS)

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mockk(relaxed = true),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).orNull()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Passed(1.337f)
            )
        )
    }

    private fun anyInstrumentationRunnerOptions() = InstrumentationRunnerOptions(
        testInstrumentationRunner = "Runner",
        testInstrumentationRunnerArguments = emptyMap(),
        animationsDisabled = true
    )

    private enum class TestResponse {
        PASS,
        MULTIPLE_PASS,
        FAIL,
        EXCEPTION
    }

    private fun anyDevice(
        serial: String = UUID.randomUUID().toString()
    ): Device = mockk {
        every { serialNumber } returns serial
        mockOrderedTestResponses(TestResponse.PASS)
    }

    private fun Device.mockOrderedTestResponses(vararg responses: TestResponse) {
        val shellOutputSlot = slot<IShellOutputReceiver>()

        val answerShellOutput: (String) -> Unit = {
            shellOutputSlot.captured.addOutput(it.toByteArray(), 0, it.length)
        }

        fun MockKStubScope<Unit, Unit>.answersTestResponse(response: TestResponse) = when (response) {
            TestResponse.PASS -> answers { answerShellOutput("OK (1 test)") }
            TestResponse.MULTIPLE_PASS -> answers { answerShellOutput("Time: 1.337\n\nOK (42 tests)") }
            TestResponse.FAIL -> answers { answerShellOutput("FAILURE") }
            TestResponse.EXCEPTION -> throws(IllegalStateException("UNABLE TO RUN"))
        }

        fun MockKAdditionalAnswerScope<Unit, Unit>.andThenAnswersTestResponse(response: TestResponse) = when (response) {
            TestResponse.PASS -> andThenAnswer { answerShellOutput("OK (1 test)") }
            TestResponse.MULTIPLE_PASS -> andThenAnswer { answerShellOutput("Time: 1.337\n\nOK (42 tests)") }
            TestResponse.FAIL -> andThenAnswer { answerShellOutput("FAILURE") }
            TestResponse.EXCEPTION -> andThenThrows(IllegalStateException("UNABLE TO RUN"))
        }

        responses.drop(1).fold(
            every {
                executeShellCommand(any(), capture(shellOutputSlot), any(), any())
            }.answersTestResponse(responses.first())
        ) { additionalAnswerScope: MockKAdditionalAnswerScope<Unit, Unit>, response: TestResponse ->
            additionalAnswerScope.andThenAnswersTestResponse(response)
        }
    }
}
