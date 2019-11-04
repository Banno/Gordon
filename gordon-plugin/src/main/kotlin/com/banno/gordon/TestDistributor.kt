package com.banno.gordon

import arrow.fx.IO
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.gradle.api.logging.Logger
import se.vidstige.jadb.JadbDevice
import kotlin.coroutines.CoroutineContext

internal fun runAllTests(
    dispatcher: CoroutineDispatcher,
    logger: Logger,
    instrumentationPackage: String,
    instrumentationRunnerOptions: InstrumentationRunnerOptions,
    allTestCases: List<TestCase>,
    allPools: List<DevicePool>,
    retryQuota: Int,
    testTimeoutMillis: Long
): IO<Map<PoolName, Map<TestCase, TestResult>>> = IO {
    runBlocking {
        allPools.map { pool ->
            async(context = dispatcher, start = CoroutineStart.LAZY) {
                val deviceSerials = pool.devices.map { it.serial }.toSet()
                val testResults =
                    allTestCases.associateWith<TestCase, TestResult> { TestResult.NotRun }
                        .toMutableMap()

                testResults += runTestsInPool(
                    dispatcher = dispatcher,
                    logger = logger,
                    instrumentationPackage = instrumentationPackage,
                    instrumentationRunnerOptions = instrumentationRunnerOptions,
                    testTimeoutMillis = testTimeoutMillis,
                    devices = pool.devices,
                    testDistributor = TestDistributor(
                        dispatcher,
                        deviceSerials,
                        allTestCases.associateWith { TestResult.NotRun }
                    )
                ).awaitAll()
                    .fold(emptyMap()) { accumulated, item -> accumulated + item }

                repeat(retryQuota) {
                    val testsThatDidNotRun = testResults.filterResultType<TestResult.NotRun>()

                    testResults += runTestsInPool(
                        dispatcher = dispatcher,
                        logger = logger,
                        instrumentationPackage = instrumentationPackage,
                        instrumentationRunnerOptions = instrumentationRunnerOptions,
                        testTimeoutMillis = testTimeoutMillis,
                        devices = pool.devices,
                        testDistributor = TestDistributor(
                            dispatcher,
                            deviceSerials,
                            testsThatDidNotRun
                        )
                    ).awaitAll()
                        .fold(emptyMap()) { accumulated, item -> accumulated + item }
                }

                repeat(retryQuota) {
                    val testsThatFailed = testResults.filterResultType<TestResult.Failed>()

                    testResults += runTestsInPool(
                        dispatcher = dispatcher,
                        logger = logger,
                        instrumentationPackage = instrumentationPackage,
                        instrumentationRunnerOptions = instrumentationRunnerOptions,
                        testTimeoutMillis = testTimeoutMillis,
                        devices = pool.devices,
                        testDistributor = TestDistributor(
                            dispatcher,
                            deviceSerials,
                            testsThatFailed
                        )
                    ).awaitAll()
                        .fold(emptyMap()) { accumulated, item ->
                            val hydratedResults = item.mapValues { (testCase, result) ->
                                val existingResult = testResults[testCase]

                                when (result) {
                                    TestResult.NotRun -> if (existingResult is TestResult.Failed) existingResult else result

                                    is TestResult.Failed -> {
                                        if (existingResult is TestResult.Failed) {
                                            result.copy(failures = existingResult.failures + result.failures)
                                        } else {
                                            result
                                        }
                                    }

                                    is TestResult.Passed -> {
                                        if (existingResult is TestResult.Failed) {
                                            TestResult.Flaky(result.duration, existingResult.failures)
                                        } else {
                                            result
                                        }
                                    }

                                    is TestResult.Flaky,
                                    TestResult.Ignored -> result
                                }
                            }

                            accumulated + hydratedResults
                        }
                }

                pool.poolName to testResults
            }
        }.awaitAll().toMap()
    }
}

private fun CoroutineScope.runTestsInPool(
    dispatcher: CoroutineDispatcher,
    logger: Logger,
    instrumentationPackage: String,
    instrumentationRunnerOptions: InstrumentationRunnerOptions,
    testTimeoutMillis: Long,
    devices: List<JadbDevice>,
    testDistributor: TestDistributor
): List<Deferred<Map<TestCase, TestResult>>> {
    return devices.map { device ->
        async(context = dispatcher, start = CoroutineStart.LAZY) {
            testDistributor.testCasesChannel(device.serial)
                .consumeAsFlow()
                .map { test ->
                    if (test.isIgnored) {
                        test to TestResult.Ignored
                    } else {
                        test to runTest(
                            logger = logger,
                            instrumentationPackage = instrumentationPackage,
                            instrumentationRunnerOptions = instrumentationRunnerOptions,
                            testTimeoutMillis = testTimeoutMillis,
                            test = test,
                            device = device
                        )
                    }
                }
                .toList()
                .toMap()
        }
    }
}

private class TestDistributor(
    private val dispatcher: CoroutineDispatcher,
    private val deviceSerials: Set<String>,
    testCases: Map<TestCase, TestResult>
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = dispatcher

    private val testsToRun = testCases.toMutableMap()

    @Synchronized
    private fun getNextTestToRunForDevice(deviceSerial: String): TestCase? = testsToRun.entries
        .firstOrNull { (_, testResult) ->
            when (testResult) {
                is TestResult.Failed -> {
                    val failedDevices = testResult.failures.map { it.deviceSerial }

                    !failedDevices.contains(deviceSerial) ||
                            failedDevices.containsAll(deviceSerials)
                }

                else -> true
            }
        }
        ?.key
        ?.also { testsToRun.remove(it) }

    fun testCasesChannel(deviceSerial: String): ReceiveChannel<TestCase> =
        produce(capacity = Channel.RENDEZVOUS) {
            var testToRun = getNextTestToRunForDevice(deviceSerial)

            while (isActive && testToRun != null) {
                send(testToRun)
                testToRun = getNextTestToRunForDevice(deviceSerial)
            }
        }
}
