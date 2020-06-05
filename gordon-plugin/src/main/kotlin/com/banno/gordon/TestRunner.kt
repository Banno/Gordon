package com.banno.gordon

import org.gradle.api.logging.Logger
import se.vidstige.jadb.JadbDevice

internal fun runTest(
    logger: Logger,
    instrumentationPackage: String,
    instrumentationRunnerOptions: InstrumentationRunnerOptions,
    testTimeoutMillis: Long,
    test: TestCase,
    device: JadbDevice,
    progress: String
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

    return device.executeShellWithTimeout(testTimeoutMillis, command)
        .attempt()
        .unsafeRunSync()
        .fold(
            {
                logger.error("$progress -> ${device.serial}: ${test.classAndMethodName}: UNABLE TO RUN")
                TestResult.NotRun
            }
        ) { shellOutput: String? ->
            val testTime = shellOutput
                ?.substringAfter("Time: ")
                ?.substringBefore("\n")
                ?.toFloatOrNull()

            when {
                shellOutput == null -> {
                    logger.error("$progress -> ${device.serial}: ${test.classAndMethodName}: TIMED OUT")
                    TestResult.Failed(testTime, "Test timed out", device.serial)
                }

                shellOutput.endsWith("OK (1 test)") -> {
                    logger.lifecycle("$progress -> ${device.serial}: ${test.classAndMethodName}: PASSED")
                    TestResult.Passed(testTime)
                }

                shellOutput.endsWith("OK (0 tests)") -> {
                    logger.lifecycle("$progress -> ${device.serial}: ${test.classAndMethodName}: IGNORED")
                    TestResult.Ignored
                }

                else -> {
                    val failureOutput = shellOutput.substringBeforeLast("There was 1 failure")
                    logger.error("$progress -> ${device.serial}: ${test.classAndMethodName}: FAILED\n$failureOutput\n")
                    TestResult.Failed(testTime, failureOutput, device.serial)
                }
            }
        }
}

internal fun Logger.logIgnored(test: TestCase, progress: String) =
    lifecycle("$progress -> ${test.classAndMethodName}: IGNORED")

private val TestCase.classAndMethodName
    get() = "${fullyQualifiedClassName.substringAfterLast('.')}.$methodName"
