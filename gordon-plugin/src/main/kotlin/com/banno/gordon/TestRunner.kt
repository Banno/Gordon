package com.banno.gordon

import com.android.tools.build.bundletool.device.Device
import org.gradle.api.logging.Logger

internal fun runTest(
    logger: Logger,
    instrumentationPackage: String,
    instrumentationRunnerOptions: InstrumentationRunnerOptions,
    testTimeoutMillis: Long,
    test: TestCase,
    device: Device,
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
        .fold(
            {
                when (it) {
                    is AdbTimeoutException -> {
                        logger.error("$progress -> ${device.serialNumber}: ${test.classAndMethodName}: TIMED OUT")
                        TestResult.Failed(null, "Test timed out", device.serialNumber)
                    }
                    else -> {
                        logger.error("$progress -> ${device.serialNumber}: ${test.classAndMethodName}: UNABLE TO RUN")
                        TestResult.NotRun
                    }
                }
            }
        ) { shellOutput ->
            val testTime = shellOutput
                .substringAfter("Time: ")
                .substringBefore("\n")
                .toFloatOrNull()

            when {
                shellOutput.matches(Regex(".*OK \\([1-9][0-9]* tests?\\)$", RegexOption.DOT_MATCHES_ALL)) -> {
                    logger.lifecycle("$progress -> ${device.serialNumber}: ${test.classAndMethodName}: PASSED")
                    TestResult.Passed(testTime)
                }

                shellOutput.endsWith("OK (0 tests)") -> {
                    logger.lifecycle("$progress -> ${device.serialNumber}: ${test.classAndMethodName}: IGNORED")
                    TestResult.Ignored
                }

                else -> {
                    val failureOutput = shellOutput.substringBeforeLast("There was 1 failure")
                    logger.error("$progress -> ${device.serialNumber}: ${test.classAndMethodName}: FAILED\n$failureOutput\n")
                    TestResult.Failed(testTime, failureOutput, device.serialNumber)
                }
            }
        }
}

internal fun Logger.logIgnored(test: TestCase, progress: String) =
    lifecycle("$progress -> ${test.classAndMethodName}: IGNORED")

private val TestCase.classAndMethodName
    get() = "${fullyQualifiedClassName.substringAfterLast('.')}.$methodName"
