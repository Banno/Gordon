package com.banno.gordon

import com.banno.gordon.TestResult.Failed
import com.banno.gordon.TestResult.Flaky
import com.banno.gordon.TestResult.NotRun
import com.banno.gordon.TestResult.Passed
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyVararg
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import se.vidstige.jadb.JadbDevice
import java.io.ByteArrayInputStream
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
                    anyJadbDevice(),
                    anyJadbDevice()
                )
            ),
            DevicePool(
                "Second",
                listOf(
                    anyJadbDevice(),
                    anyJadbDevice()
                )
            ),
            DevicePool(
                "Third",
                listOf(
                    anyJadbDevice(),
                    anyJadbDevice()
                )
            )
        )

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 0,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

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

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(
            TestResponse.EXCEPTION,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Passed(null)
            )
        )

        verify(device, times(2)).executeShell(
            check<String> { it.contains("A#methodOne") },
            anyVararg()
        )
    }

    @Test
    fun shouldRetryFailedTests() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(
            TestResponse.FAIL,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Flaky(null, listOf(Failure(null, "FAILURE", device.serial)))
            )
        )

        verify(device, times(2)).executeShell(
            check<String> { it.contains("A#methodOne") },
            anyVararg()
        )
    }

    @Test
    fun shouldRetryQuotaNumberOfTimesForNotRun() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(
            TestResponse.EXCEPTION,
            TestResponse.EXCEPTION,
            TestResponse.EXCEPTION,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 2,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to NotRun
            )
        )

        verify(device, times(3)).executeShell(
            check<String> { it.contains("A#methodOne") },
            anyVararg()
        )
    }

    @Test
    fun shouldRetryQuotaNumberOfTimesForFailedTests() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(
            TestResponse.FAIL,
            TestResponse.FAIL,
            TestResponse.FAIL,
            TestResponse.PASS
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 2,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Failed(
                    listOf(
                        Failure(null, "FAILURE", device.serial),
                        Failure(null, "FAILURE", device.serial),
                        Failure(null, "FAILURE", device.serial)
                    )

                )
            )
        )

        verify(device, times(3)).executeShell(
            check<String> { it.contains("A#methodOne") },
            anyVararg()
        )
    }

    @Test
    fun shouldRetryFailedTestsOnDifferentDevices() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyJadbDevice()
        val device2 = anyJadbDevice()

        device.mockOrderedTestResponses(TestResponse.FAIL)

        device2.mockOrderedTestResponses(TestResponse.FAIL)

        val pools = listOf(DevicePool("First", listOf(device, device2)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        verify(device, times(1)).executeShell(
            check<String> { it.contains("A#methodOne") },
            anyVararg()
        )
        verify(device, times(1)).executeShell(
            check<String> { it.contains("A#methodOne") },
            anyVararg()
        )
    }

    @Test
    fun shouldNotOverwriteFailureIfRetryDoesNotRun() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(
            TestResponse.FAIL,
            TestResponse.EXCEPTION
        )

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Failed(null, "FAILURE", device.serial)
            )
        )
    }

    @Test
    fun shouldAggregateTestFailureSerials() {
        val testSuite = listOf(
            TestCase("A", "methodOne", false)
        )

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(TestResponse.FAIL, TestResponse.FAIL)

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Failed(
                    listOf(
                        Failure(null, "FAILURE", device.serial),
                        Failure(null, "FAILURE", device.serial)
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

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(TestResponse.FAIL, TestResponse.FAIL, TestResponse.PASS)

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 3,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

        actual shouldEqual mapOf(
            "First" to mapOf(
                TestCase("A", "methodOne", false) to Flaky(
                    null,
                    listOf(
                        Failure(null, "FAILURE", device.serial),
                        Failure(null, "FAILURE", device.serial)
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

        val device = anyJadbDevice()

        device.mockOrderedTestResponses(TestResponse.MULTIPLE_PASS)

        val pools = listOf(DevicePool("First", listOf(device)))

        val actual = runAllTests(
            dispatcher = Dispatchers.IO,
            logger = mock(),
            instrumentationPackage = "instrumentationPackage",
            instrumentationRunnerOptions = anyInstrumentationRunnerOptions(),
            allTestCases = testSuite,
            allPools = pools,
            retryQuota = 1,
            testTimeoutMillis = 1000
        ).unsafeRunSync()

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

    private fun JadbDevice.mockOrderedTestResponses(vararg responses: TestResponse) {
        var index = 0
        forMock(this) {
            on {
                this.executeShell(
                    any<String>(),
                    anyVararg()
                )
            } doAnswer { invocation ->
                val response = responses[index]

                index++

                when (response) {
                    TestResponse.PASS -> ByteArrayInputStream("OK (1 test)".toByteArray())
                    TestResponse.MULTIPLE_PASS -> ByteArrayInputStream("Time: 1.337\n\nOK (42 tests)".toByteArray())
                    TestResponse.FAIL -> ByteArrayInputStream("FAILURE".toByteArray())
                    TestResponse.EXCEPTION -> null
                }
            }
        }
    }

    private fun anyJadbDevice(
        serial: String = UUID.randomUUID().toString()
    ): JadbDevice {
        return mock {
            on { this.serial } doReturn serial
            on {
                this.executeShell(
                    any<String>(),
                    anyVararg()
                )
            } doAnswer { _ -> ByteArrayInputStream("OK (1 test)".toByteArray()) }
        }
    }
}
