package com.banno.gordon

import arrow.core.Either
import arrow.core.computations.either
import com.android.tools.build.bundletool.device.Device
import org.gradle.api.logging.Logger
import java.io.File

private const val EXEC_TIMEOUT_MILLIS = 20_000L

internal fun runTest(
    logger: Logger,
    testedApplicationPackage: String,
    instrumentationPackage: String,
    instrumentationRunnerOptions: InstrumentationRunnerOptions,
    testTimeoutMillis: Long,
    test: TestCase,
    device: Device,
    progress: String,
    buildDir: File,
    taskName: String
): TestResult {
    val targetInstrumentation = "$instrumentationPackage/${instrumentationRunnerOptions.testInstrumentationRunner}"
    val isCoverageEnabled = instrumentationRunnerOptions.isCoverageEnabled()

    val flags = listOfNotNull(
        "-w",
        "--no-window-animation".takeIf { instrumentationRunnerOptions.animationsDisabled }
    )
        .joinToString(" ")

    var options = instrumentationRunnerOptions
        .testInstrumentationRunnerArguments
        .plus("class" to "${test.fullyQualifiedClassName}#${test.methodName}")
        .entries
        .joinToString(" ") { "-e ${it.key} ${it.value}" }

    var coverageFileFullPath: String? = null

    if (isCoverageEnabled) {
        either.eager<Throwable, Unit> {
            val userId = device.getCurrentUserId().bind()
            device.createFilesDirFor(userId, testedApplicationPackage).bind()
            coverageFileFullPath = getCoverageFileFullPath(userId, testedApplicationPackage, test)
            options += " -e coverageFile $coverageFileFullPath"
        }
    }

    val command = "am instrument $flags $options $targetInstrumentation"
    logger.info("Instrumentation command: $command")

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

            if (isCoverageEnabled) {
                coverageFileFullPath?.let {
                    device.copyCoverageFile(buildDir, taskName, testedApplicationPackage, test.coverageFileName, logger)
                }
            }

            when {
                Regex("OK \\([1-9][0-9]* tests?\\)").containsMatchIn(shellOutput) -> {
                    logger.lifecycle("$progress -> ${device.serialNumber}: ${test.classAndMethodName}: PASSED")
                    TestResult.Passed(testTime)
                }

                shellOutput.contains("OK (0 tests)") -> {
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

private fun Device.createFilesDirFor(userId: String, targetPackage: String): Either<Throwable, Unit> =
    executeShellWithTimeout(
        EXEC_TIMEOUT_MILLIS,
        "run-as $targetPackage --user $userId mkdir -p ${getDeviceCoveragePathFor(userId, targetPackage)}"
    ).void()

internal fun Logger.logIgnored(test: TestCase, progress: String) =
    lifecycle("$progress -> ${test.classAndMethodName}: IGNORED")
