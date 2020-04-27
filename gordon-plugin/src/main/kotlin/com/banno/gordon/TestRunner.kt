package com.banno.gordon

import org.gradle.api.logging.Logger
import se.vidstige.jadb.JadbDevice

internal fun runTest(
    logger: Logger,
    instrumentationPackage: String,
    instrumentationRunnerOptions: InstrumentationRunnerOptions,
    testTimeoutMillis: Long,
    test: TestCase,
    device: JadbDevice
): TestResult {
    val targetInstrumentation = "$instrumentationPackage/${instrumentationRunnerOptions.testInstrumentationRunner}"

    val flags = listOfNotNull(
        "-w",
        "--no-window-animation".takeIf { instrumentationRunnerOptions.animationsDisabled }
    )
        .joinToString(" ")

    val options = instrumentationRunnerOptions
        .testInstrumentationRunnerArguments
        .plus("class" to "${test.fullyQualifiedClassName}#${test.methodName}")
        .entries
        .joinToString(" ") { "-e ${it.key} ${it.value}" }

    val command = "am instrument $flags $options $targetInstrumentation"

    val testName = "${test.fullyQualifiedClassName.substringAfterLast('.')}.${test.methodName}"

    return device.executeShellWithTimeout(testTimeoutMillis, command)
        .attempt()
        .unsafeRunSync()
        .fold(
            {
                logger.error("${device.serial}: $testName: UNABLE TO RUN")
                TestResult.NotRun
            }
        ) { shellOutput: String? ->
            val testTime = shellOutput
                ?.substringAfter("Time: ")
                ?.substringBefore("\n")
                ?.toFloatOrNull()

            when {
                shellOutput == null -> {
                    logger.error("${device.serial}: $testName: TIMED OUT")
                    TestResult.Failed(testTime, "Test timed out", device.serial)
                }

                shellOutput.endsWith("OK (1 test)") -> {
                    logger.lifecycle("${device.serial}: $testName: PASSED")
                    TestResult.Passed(testTime)
                }

                shellOutput.endsWith("OK (0 tests)") -> {
                    logger.lifecycle("${device.serial}: $testName: IGNORED")
                    TestResult.Ignored
                }

                else -> {
                    val failureOutput = shellOutput.substringBeforeLast("There was 1 failure")
                    logger.error("${device.serial}: $testName: FAILED\n$failureOutput\n")
                    TestResult.Failed(testTime, failureOutput, device.serial)
                }
            }
        }
}
